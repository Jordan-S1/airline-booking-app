package com.airlinebookingsystem.service;

import com.airlinebookingsystem.entity.Airport;
import com.airlinebookingsystem.entity.Flight;
import com.airlinebookingsystem.repository.FlightRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Keeps a rolling window of bookable flights.
 *
 * <p>The seeded timetable covers a fixed set of dates, which would leave the
 * app with nothing to sell once those dates passed. This service treats each
 * distinct flight number as a daily service and materialises instances for any
 * missing date inside the horizon, cloning departure time-of-day, pricing and
 * capacity from an existing instance.
 *
 * <p>Old flights are pruned only when nobody booked them — booked flights are
 * kept indefinitely so a customer's trip history never disappears.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FlightScheduleService {

    private final FlightRepository flightRepository;

    @Value("${flights.schedule.enabled:true}")
    private boolean enabled;

    @Value("${flights.schedule.horizon-days:14}")
    private int horizonDays;

    @Value("${flights.schedule.retention-days:3}")
    private int retentionDays;

    /**
     * Tops the window up at boot so a freshly started app is never empty.
     *
     * <p>Annotated transactional in its own right: the call to
     * {@link #refreshSchedule()} below is a self-invocation, which does not pass
     * through Spring's proxy, so the annotation there would not apply. The
     * scheduler reads each flight's departure airport to work out its timezone,
     * and that association is lazy — without a session open here it fails.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void onStartup() {
        if (!enabled) {
            log.info("Rolling flight schedule is disabled");
            return;
        }
        refreshSchedule();
    }

    /** Runs daily; the cron is configurable via flights.schedule.cron. */
    @Scheduled(cron = "${flights.schedule.cron:0 15 3 * * *}")
    @Transactional
    public void scheduledRefresh() {
        if (!enabled) return;
        refreshSchedule();
    }

    @Transactional
    public void refreshSchedule() {
        int created = materialiseMissingFlights();
        int pruned = pruneExpiredFlights();
        log.info("Rolling flight schedule refreshed — {} created, {} pruned, horizon {} days",
                created, pruned, horizonDays);
    }

    private int materialiseMissingFlights() {
        List<Flight> allFlights = flightRepository.findAll();
        if (allFlights.isEmpty()) {
            log.warn("No flights exist to use as schedule templates");
            return 0;
        }

        Map<String, List<Flight>> byFlightNumber = allFlights.stream()
                .collect(Collectors.groupingBy(Flight::getFlightNumber));

        List<Flight> newFlights = new ArrayList<>();

        for (Map.Entry<String, List<Flight>> entry : byFlightNumber.entrySet()) {
            List<Flight> instances = entry.getValue();

            // Prefer the least-depleted instance so clones inherit full capacity
            // rather than a seat count already reduced by bookings.
            Flight template = instances.stream()
                    .max(Comparator.comparingInt(f ->
                            f.getAvailableSeats() == null ? 0 : f.getAvailableSeats()))
                    .orElseThrow();

            // Dates are handled in the departure airport's own zone, not UTC.
            // "The 1st of August service" is a local-calendar idea, and keeping
            // the window, the existence check and the clone in the same terms is
            // what stops a flight either being missed or created twice near
            // midnight UTC.
            ZoneId zone = zoneOf(template.getDepartureAirport());

            // Include today so the timetable spans flights that have already
            // departed, are boarding, or are still to come — which is what makes
            // the live status widget show real BOARDING/IN_AIR/LANDED states.
            LocalDate firstDate = LocalDate.now(zone);
            LocalDate lastDate = firstDate.plusDays(horizonDays);

            Set<LocalDate> existingDates = instances.stream()
                    .map(f -> localDepartureDate(f, zone))
                    .collect(Collectors.toSet());

            for (LocalDate date = firstDate; !date.isAfter(lastDate); date = date.plusDays(1)) {
                if (!existingDates.contains(date)) {
                    newFlights.add(cloneForDate(template, date, zone));
                }
            }
        }

        if (newFlights.isEmpty()) {
            return 0;
        }

        flightRepository.saveAll(newFlights);
        return newFlights.size();
    }

    /**
     * Builds a fresh instance of a service on a given date, preserving the
     * template's local time-of-day and deriving arrival from the stored
     * duration so overnight flights stay correct. Seat counts are reset to full
     * capacity.
     *
     * <p>Times are stored as UTC instants, so the time-of-day is carried in the
     * departure airport's own zone rather than in UTC. Cloning the UTC clock
     * face directly would make a 06:30 local service drift to 05:30 or 07:30
     * the moment that region changed its offset.
     */
    private Flight cloneForDate(Flight template, LocalDate date, ZoneId departureZone) {
        LocalTime localDepartureTime = template.getDepartureTime()
                .atOffset(ZoneOffset.UTC)
                .atZoneSameInstant(departureZone)
                .toLocalTime();

        LocalDateTime departure = date.atTime(localDepartureTime)
                .atZone(departureZone)
                .toInstant()
                .atOffset(ZoneOffset.UTC)
                .toLocalDateTime();

        int durationMinutes = template.getDuration() != null
                ? template.getDuration()
                : (int) java.time.Duration.between(
                        template.getDepartureTime(), template.getArrivalTime()).toMinutes();

        return Flight.builder()
                .flightNumber(template.getFlightNumber())
                .airline(template.getAirline())
                .departureAirport(template.getDepartureAirport())
                .arrivalAirport(template.getArrivalAirport())
                .departureTime(departure)
                .arrivalTime(departure.plusMinutes(durationMinutes))
                .duration(durationMinutes)
                .basePrice(template.getBasePrice())
                .economyPrice(template.getEconomyPrice())
                .businessPrice(template.getBusinessPrice())
                .firstClassPrice(template.getFirstClassPrice())
                .totalSeats(template.getTotalSeats())
                .availableSeats(template.getTotalSeats())
                .economySeats(template.getEconomySeats())
                .businessSeats(template.getBusinessSeats())
                .firstClassSeats(template.getFirstClassSeats())
                .status(Flight.FlightStatus.SCHEDULED)
                .active(true)
                .aircraft(template.getAircraft())
                .gate(template.getGate())
                .terminal(template.getTerminal())
                .build();
    }

    /**
     * The airport's IANA zone, falling back to UTC. A missing or unrecognised
     * zone must not stop the schedule rolling forward — the flight simply keeps
     * its stored UTC clock face, which is what happened before zones were
     * considered at all.
     */
    private ZoneId zoneOf(Airport airport) {
        String timezone = airport == null ? null : airport.getTimezone();
        if (timezone == null || timezone.isBlank()) {
            return ZoneOffset.UTC;
        }
        try {
            return ZoneId.of(timezone);
        } catch (DateTimeException e) {
            log.warn("Airport {} has unusable timezone '{}'; scheduling in UTC",
                    airport.getCode(), timezone);
            return ZoneOffset.UTC;
        }
    }

    /** The calendar date a stored UTC departure falls on, locally. */
    private LocalDate localDepartureDate(Flight flight, ZoneId zone) {
        return flight.getDepartureTime()
                .atOffset(ZoneOffset.UTC)
                .atZoneSameInstant(zone)
                .toLocalDate();
    }

    private int pruneExpiredFlights() {
        LocalDateTime cutoff = LocalDateTime.now(ZoneOffset.UTC).minusDays(retentionDays);
        List<Flight> prunable = flightRepository.findPrunableFlights(cutoff);
        if (prunable.isEmpty()) {
            return 0;
        }
        flightRepository.deleteAll(prunable);
        return prunable.size();
    }
}
