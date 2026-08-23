package com.airlinebookingsystem.service;

import com.airlinebookingsystem.dto.assistant.AssistantRequest;
import com.airlinebookingsystem.dto.assistant.AssistantResponse;
import com.airlinebookingsystem.dto.assistant.ExtractedSearch;
import com.airlinebookingsystem.dto.flight.FlightSearchRequest;
import com.airlinebookingsystem.dto.flight.FlightSearchResponse;
import com.airlinebookingsystem.dto.flight.FlightSearchResult;
import com.airlinebookingsystem.entity.Airport;
import com.airlinebookingsystem.entity.Booking;
import com.airlinebookingsystem.exception.ExternalServiceException;
import com.airlinebookingsystem.repository.AirportRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Natural-language flight search: extract, search, summarise.
 *
 * <p>The invariant, which is an absence and so cannot be read off the code
 * below: <strong>nothing the model returns ever becomes data.</strong>
 * Extraction produces a claim that is checked against the database before the
 * search runs, and summarisation produces prose about rows already retrieved.
 * An invented airline can make the wording wrong; it cannot add a flight or
 * change a price.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TravelAssistantService {

    private final ClaudeClient claudeClient;
    private final FlightService flightService;
    private final AirportRepository airportRepository;
    private final ObjectMapper objectMapper;

    /** A booking cannot exceed this many passengers, so neither can a search. */
    private static final int MIN_PASSENGERS = 1;
    private static final int MAX_PASSENGERS = 9;

    /** How many rows the summariser is shown. The response still carries all of them. */
    private static final int MAX_ROWS_IN_PROMPT = 10;

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.UK);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.UK);

    /**
     * Answers a natural-language request.
     *
     * @throws ExternalServiceException if the assistant is not configured or the
     *                                  model cannot be reached — 503, because
     *                                  neither is a fault in this request
     */
    @Transactional(readOnly = true)
    public AssistantResponse answer(AssistantRequest request) {
        log.info("Assistant request received ({} characters)", request.message().length());

        // Checked before the airport list is loaded: with no key there is
        // nothing to build a prompt for, and the query would be wasted.
        if (!claudeClient.isConfigured()) {
            throw new ExternalServiceException(
                    "AI travel assistant", "no Anthropic API key is configured");
        }

        // Extraction failing is fatal; there is nothing to search without it.
        String raw = claudeClient.complete(extractionPrompt(), request.message());

        Optional<ExtractedSearch> parsed = parse(raw);
        if (parsed.isEmpty()) {
            // Never guess at a search from text that did not parse.
            return AssistantResponse.question(
                    "Sorry, I did not follow that. Could you rephrase it — for example, "
                            + "\"cheapest flight from Dublin to Paris next Friday\"?");
        }

        ExtractedSearch extracted = parsed.get();

        /*
         * The model's own wording for a question, used only if one turns out to
         * be needed.
         *
         * Deliberately not an early return. The model sets this whenever the
         * message names no origin — which is precisely when originHint is meant
         * to supply one — so returning here made the hint dead in the only case
         * it exists for. It also pre-empted the passenger clamp: asked for
         * thirty seats, the model queried the group size instead of the search
         * running for nine. Its wording is usually better than the fallbacks
         * below, so it is kept; it just does not get to decide that a question
         * is necessary.
         */
        String modelQuestion =
                extracted.clarification() == null || extracted.clarification().isBlank()
                        ? null
                        : extracted.clarification().trim();

        // ---- Validation. Nothing below this line trusts the model. ----------

        String origin = resolveAirport(extracted.departureAirport())
                .or(() -> resolveAirport(request.originHint()))
                .orElse(null);
        if (origin == null) {
            return ask(modelQuestion, "Which airport are you flying from?");
        }

        String destination = resolveAirport(extracted.arrivalAirport()).orElse(null);
        if (destination == null) {
            return ask(modelQuestion,
                    "I could not match that destination to an airport we fly to. Where would you like to go?");
        }

        if (origin.equals(destination)) {
            return AssistantResponse.question(
                    "That is the same airport at both ends — where are you flying to?");
        }

        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        LocalDate departureDate = extracted.departureDate();
        if (departureDate == null) {
            return ask(modelQuestion, "Which date would you like to travel on?");
        }
        if (departureDate.isBefore(today)) {
            // Refused rather than nudged forward: "last Friday" and "next
            // Friday" are a week apart, and picking one is not this code's call.
            return AssistantResponse.question(
                    "%s has already passed. Which date did you mean?".formatted(DAY.format(departureDate)));
        }

        // Past the rolling window there is nothing and never will be, so
        // "no flights found — try a nearby date" is advice that cannot work.
        // Empty means an empty timetable, which is not the traveller's problem
        // to hear about; the search runs and finds nothing on its own.
        Optional<LocalDate> lastBookable = flightService.getLatestDepartureDate();
        if (lastBookable.isPresent() && departureDate.isAfter(lastBookable.get())) {
            return AssistantResponse.question(
                    "Our timetable only runs to %s at the moment, so there is nothing to show for %s yet. Pick a date on or before then and I will search it."
                            .formatted(DAY.format(lastBookable.get()), DAY.format(departureDate)));
        }

        // A return before the outbound leg is a misreading of the sentence, not
        // a request. Dropped silently — the one-way search is still the answer.
        LocalDate returnDate = extracted.returnDate();
        if (returnDate != null && returnDate.isBefore(departureDate)) {
            log.info("Discarded a return date that preceded the outbound date");
            returnDate = null;
        }

        int passengers = extracted.passengers() == null
                ? MIN_PASSENGERS
                : Math.clamp(extracted.passengers(), MIN_PASSENGERS, MAX_PASSENGERS);

        String seatClass = resolveSeatClass(extracted.seatClass());

        FlightSearchRequest searchRequest = new FlightSearchRequest(
                origin, destination, departureDate, returnDate, passengers, seatClass, null);

        // ---- The search itself is the ordinary one, unchanged. --------------

        FlightSearchResult result = flightService.searchFlights(searchRequest);
        List<FlightSearchResponse> flights = result.outboundFlights() == null
                ? List.of()
                : result.outboundFlights();

        int returnOptions = result.returnFlights() == null ? 0 : result.returnFlights().size();

        return new AssistantResponse(
                summarise(request.message(), searchRequest, flights, returnOptions),
                flights,
                searchRequest,
                false);
    }

    /**
     * Asks a question, preferring the model's wording to the fallback.
     *
     * <p>The model can see what the traveller actually wrote, so it names the
     * city they asked for rather than saying "that destination". The fallback
     * covers the case where it offered nothing.
     */
    private AssistantResponse ask(String modelQuestion, String fallback) {
        return AssistantResponse.question(modelQuestion != null ? modelQuestion : fallback);
    }

    // ---- Step 1: extraction ------------------------------------------------

    /**
     * The extraction prompt.
     *
     * <p>Carries the airport list so city names resolve to codes this network
     * actually serves, and today's date so relative dates ("next Friday") can be
     * worked out at all. The date is stated with its weekday because that is the
     * fact the arithmetic turns on, and the list is sorted so the prompt is
     * stable between requests.
     */
    private String extractionPrompt() {
        String airports = airportRepository.findAll().stream()
                .sorted(Comparator.comparing(Airport::getCode))
                .map(a -> "  %s  %s, %s".formatted(a.getCode(), a.getCity(), a.getCountry()))
                .reduce("", (a, b) -> a.isEmpty() ? b : a + "\n" + b);

        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        return """
                You convert a traveller's message into a flight search for SkyAir.

                Reply with one JSON object and nothing else — no prose, no markdown fences.

                Fields, using null for anything the traveller did not say:
                  departureAirport  three-letter IATA code, from the list below only
                  arrivalAirport    three-letter IATA code, from the list below only
                  departureDate     ISO date, YYYY-MM-DD
                  returnDate        ISO date, or null for a one-way trip
                  passengers        whole number, exactly as many as they asked for
                  seatClass         ECONOMY, BUSINESS or FIRST
                  clarification     a short question to ask instead of searching, or null

                Rules:
                - Use only codes from the list. If the traveller names a place that is not
                  on it, leave the field null and say so in clarification.
                - Never invent a destination the traveller did not name.
                - Resolve relative dates against today's date below.
                - A bare date that has already gone by this year stays in the past.
                  Do not roll it forward into next year: return the date as said, and
                  the traveller will be asked which one they meant. Only use a later
                  year if they named one.
                - Report the passenger count they actually said, even if it is large.
                  Limits are applied afterwards; silently reducing it here means a
                  request for thirty seats is searched as one.
                - "cheapest", "direct" and similar preferences are not fields here; ignore them.
                - If the message is not about finding a flight, leave every field null and
                  put a brief, friendly reply in clarification.

                Airports SkyAir serves:
                %s

                Today is %s, UTC.
                """.formatted(airports, DAY.format(today));
    }

    /**
     * Reads the model's reply as {@link ExtractedSearch}.
     *
     * @return empty if it is not usable JSON — which is a signal to ask the
     *         traveller to rephrase, never to fall back on a guessed search
     */
    private Optional<ExtractedSearch> parse(String raw) {
        String json = stripFences(raw);
        try {
            return Optional.ofNullable(objectMapper.readValue(json, ExtractedSearch.class));
        } catch (Exception ex) {
            log.warn("Could not read the model's extraction as JSON: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Removes a ```json fence if one is present.
     *
     * <p>The prompt asks for bare JSON, and this is not a licence to accept
     * anything — everything after it still has to parse. It only avoids
     * discarding an otherwise-correct answer over its packaging.
     */
    private String stripFences(String raw) {
        String text = raw.trim();
        if (!text.startsWith("```")) {
            return text;
        }
        int firstNewline = text.indexOf('\n');
        int closing = text.lastIndexOf("```");
        if (firstNewline < 0 || closing <= firstNewline) {
            return text;
        }
        return text.substring(firstNewline + 1, closing).trim();
    }

    // ---- Validation helpers ------------------------------------------------

    /**
     * Resolves a code to one the timetable knows.
     *
     * @return empty for anything the database does not hold, including null and
     *         malformed input. The caller discards it; it is never repaired.
     */
    private Optional<String> resolveAirport(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        String normalised = code.trim().toUpperCase(Locale.ROOT);
        if (normalised.length() != 3) {
            return Optional.empty();
        }
        return airportRepository.findByCode(normalised).map(Airport::getCode);
    }

    /**
     * Maps a cabin name onto one of the three, falling back to economy.
     *
     * <p>Unlike an unknown airport, an unknown cabin has an obvious safe answer:
     * economy is the cheapest and the most available, so falling back cannot
     * quietly upsell anyone. {@code SeatClassUtils.parseSeatClass} rejects
     * unknown names, which is right for an API caller and wrong here — a model
     * mishearing "premium" should not produce an error page.
     */
    private String resolveSeatClass(String seatClass) {
        if (seatClass == null || seatClass.isBlank()) {
            return Booking.SeatClass.ECONOMY.name();
        }
        try {
            return Booking.SeatClass.valueOf(seatClass.trim().toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException ex) {
            log.info("Unknown cabin '{}' from the model; using economy", seatClass.trim());
            return Booking.SeatClass.ECONOMY.name();
        }
    }

    // ---- Step 3: summarisation --------------------------------------------

    /**
     * Describes the rows that were found.
     *
     * <p>Failure here is not failure of the request. The flights have already
     * been retrieved and are already correct; losing the prose is a cosmetic
     * loss, so a model error falls back to a plain sentence built from the same
     * rows rather than discarding the answer.
     */
    private String summarise(String question,
                             FlightSearchRequest search,
                             List<FlightSearchResponse> flights,
                             int returnOptions) {
        try {
            return claudeClient.complete(
                    SUMMARY_PROMPT,
                    summaryInput(question, search, flights, returnOptions)).trim();
        } catch (ExternalServiceException ex) {
            log.warn("Summarisation failed, returning the results without it: {}", ex.getMessage());
            return plainSummary(search, flights, returnOptions);
        }
    }

    private static final String SUMMARY_PROMPT = """
            You are SkyAir's booking assistant. Write a short reply about flight
            results that have already been found.

            The list you are given is the whole of what is available to you:
            - Never mention a flight, airline, price, time or airport that is not in it.
            - Quote prices and times exactly as written; do not round or convert them.
            - If the list is empty, say plainly that nothing was found and suggest
              trying a nearby date.

            Two or three sentences of plain prose. No bullet points, no markdown,
            no invented booking references.
            """;

    private String summaryInput(String question,
                                FlightSearchRequest search,
                                List<FlightSearchResponse> flights,
                                int returnOptions) {
        StringBuilder input = new StringBuilder()
                .append("The traveller asked: ").append(question).append("\n\n")
                .append("Search run: %s to %s on %s, %d passenger(s), %s."
                        .formatted(search.departureAirport(), search.arrivalAirport(),
                                search.departureDate(), search.passengers(), search.seatClass()))
                .append("\n\n");

        if (flights.isEmpty()) {
            input.append("Flights found: none.\n");
        } else {
            List<FlightSearchResponse> shown = flights.stream()
                    .sorted(Comparator.comparing(FlightSearchResponse::price))
                    .limit(MAX_ROWS_IN_PROMPT)
                    .toList();

            input.append("Flights found: %d. Showing the %d cheapest.\n"
                    .formatted(flights.size(), shown.size()));
            for (FlightSearchResponse f : shown) {
                input.append("  ")
                        .append("%s, %s: %s %s to %s %s (times UTC), %s, EUR %s, %d seats left in %s\n"
                                .formatted(
                                        f.flightNumber(), f.airlineName(),
                                        f.departureAirport(), TIME.format(f.departureTime()),
                                        f.arrivalAirport(), TIME.format(f.arrivalTime()),
                                        duration(f.duration()), f.price(),
                                        f.availableSeats() == null ? 0 : f.availableSeats(),
                                        search.seatClass().toLowerCase(Locale.ROOT)));
            }
        }

        if (search.returnDate() != null) {
            input.append("\nReturn on %s: %d option(s) available. Mention this briefly; "
                    .formatted(search.returnDate(), returnOptions))
                    .append("do not list them.\n");
        }

        return input.toString();
    }

    /** The answer without the prose, built from the same rows. */
    private String plainSummary(FlightSearchRequest search,
                                List<FlightSearchResponse> flights,
                                int returnOptions) {
        String route = "%s to %s on %s".formatted(
                search.departureAirport(), search.arrivalAirport(), DAY.format(search.departureDate()));

        if (flights.isEmpty()) {
            return "No flights found from %s. Try a nearby date.".formatted(route);
        }

        String cheapest = flights.stream()
                .map(FlightSearchResponse::price)
                .min(Comparator.naturalOrder())
                .map(price -> ", from EUR " + price)
                .orElse("");

        String returns = search.returnDate() == null
                ? ""
                : " There are %d return option(s) on %s.".formatted(returnOptions, search.returnDate());

        return "Found %d flight(s) from %s%s.%s".formatted(flights.size(), route, cheapest, returns);
    }

    private String duration(Integer minutes) {
        if (minutes == null || minutes <= 0) {
            return "duration unknown";
        }
        return "%dh %02dm".formatted(minutes / 60, minutes % 60);
    }
}
