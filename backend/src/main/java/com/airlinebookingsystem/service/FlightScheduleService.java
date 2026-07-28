package com.airlinebookingsystem.service;

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

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    /** Tops the window up at boot so a freshly started app is never empty. */
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        if (!enabled) {
            log.info("Rolling flight schedule is disabled");
            return;
        }
        refreshSchedule();
    }

    /** Runs daily; the cron is configurable via flights.schedule.cron. */
    @Scheduled(cron = "${flights.schedule.cron:0 15 3 * * *}")
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

        // Include today so the timetable spans flights that have already
        // departed, are boarding, or are still to come — which is what makes
        // the live status widget show real BOARDING/IN_AIR/LANDED states.
        LocalDate firstDate = LocalDate.now();
        LocalDate lastDate = LocalDate.now().plusDays(horizonDays);

        List<Flight> newFlights = new ArrayList<>();

        for (Map.Entry<String, List<Flight>> entry : byFlightNumber.entrySet()) {
            List<Flight> instances = entry.getValue();

            // Prefer the least-depleted instance so clones inherit full capacity
            // rather than a seat count already reduced by bookings.
            Flight template = instances.stream()
                    .max(Comparator.comparingInt(f ->
                            f.getAvailableSeats() == null ? 0 : f.getAvailableSeats()))
                    .orElseThrow();

            Set<LocalDate> existingDates = instances.stream()
                    .map(f -> f.getDepartureTime().toLocalDate())
                    .collect(Collectors.toSet());

            for (LocalDate date = firstDate; !date.isAfter(lastDate); date = date.plusDays(1)) {
                if (!existingDates.contains(date)) {
                    newFlights.add(cloneForDate(template, date));
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
     * template's time-of-day and deriving arrival from the stored duration so
     * overnight flights stay correct. Seat counts are reset to full capacity.
     */
    private Flight cloneForDate(Flight template, LocalDate date) {
        LocalDateTime departure = date.atTime(template.getDepartureTime().toLocalTime());
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

    private int pruneExpiredFlights() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        List<Flight> prunable = flightRepository.findPrunableFlights(cutoff);
        if (prunable.isEmpty()) {
            return 0;
        }
        flightRepository.deleteAll(prunable);
        return prunable.size();
    }
}
