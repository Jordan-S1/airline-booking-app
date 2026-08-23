package com.airlinebookingsystem.service;

import com.airlinebookingsystem.dto.assistant.AssistantRequest;
import com.airlinebookingsystem.dto.assistant.AssistantResponse;
import com.airlinebookingsystem.dto.flight.FlightSearchRequest;
import com.airlinebookingsystem.dto.flight.FlightSearchResponse;
import com.airlinebookingsystem.dto.flight.FlightSearchResult;
import com.airlinebookingsystem.entity.Airport;
import com.airlinebookingsystem.exception.ExternalServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the grounded travel assistant.
 *
 * <p>{@link ClaudeClient} is mocked, which is the whole point: the guarantees
 * this service makes are that they hold <em>whatever the model says</em>, and
 * that can only be tested by making the model say the wrong thing on purpose.
 * Most of what follows is therefore a hostile or careless model output paired
 * with an assertion that the database still wins.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TravelAssistantServiceTest {

    @Mock private ClaudeClient claudeClient;
    @Mock private FlightService flightService;
    @Mock private com.airlinebookingsystem.repository.AirportRepository airportRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private TravelAssistantService service;

    /** The whole network as far as these tests are concerned. */
    private final Map<String, Airport> network = new LinkedHashMap<>();

    /** Comfortably inside the rolling schedule horizon and never in the past. */
    private static final LocalDate FUTURE = LocalDate.now(ZoneOffset.UTC).plusDays(7);

    /** Must match the service's, so the assertions read the same wording a user does. */
    private static final DateTimeFormatter DAY =
            DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.UK);

    private static final String QUESTION = "cheapest flight to Paris next Friday";

    /** The one real row. Anything else appearing in a response is invented. */
    private static final FlightSearchResponse REAL_ROW = new FlightSearchResponse(
            1L, "EI512", "Aer Lingus", "EI",
            "DUB", "CDG", "Dublin", "Paris",
            LocalDateTime.of(2026, 8, 28, 9, 15),
            LocalDateTime.of(2026, 8, 28, 11, 5),
            "Europe/Dublin", "Europe/Paris",
            110, new BigDecimal("89.99"), 42, "A320");

    @BeforeEach
    void setUp() {
        service = new TravelAssistantService(
                claudeClient, flightService, airportRepository, objectMapper);

        network.put("DUB", airport("DUB", "Dublin", "Ireland"));
        network.put("CDG", airport("CDG", "Paris", "France"));
        network.put("MAD", airport("MAD", "Madrid", "Spain"));

        when(airportRepository.findAll()).thenReturn(List.copyOf(network.values()));
        when(airportRepository.findByCode(anyString()))
                .thenAnswer(inv -> Optional.ofNullable(network.get(inv.getArgument(0))));

        when(claudeClient.isConfigured()).thenReturn(true);
        when(flightService.searchFlights(any(FlightSearchRequest.class)))
                .thenReturn(new FlightSearchResult(List.of(REAL_ROW), null, false));
        // The rolling window, well past every date these tests search for.
        when(flightService.getLatestDepartureDate())
                .thenReturn(Optional.of(LocalDate.now(ZoneOffset.UTC).plusDays(14)));
    }

    private static Airport airport(String code, String city, String country) {
        return Airport.builder().code(code).city(city).country(country).build();
    }

    /**
     * Scripts both model calls.
     *
     * <p>Keyed on the system prompt rather than call order, so a test that never
     * reaches summarisation still gets the extraction it asked for instead of
     * silently consuming the wrong script.
     */
    private void modelSays(String extraction, String summary) {
        when(claudeClient.complete(anyString(), anyString())).thenAnswer(invocation -> {
            String systemPrompt = invocation.getArgument(0);
            return systemPrompt.contains("You convert a traveller's message") ? extraction : summary;
        });
    }

    /** A well-formed extraction, so a test can vary one field and leave the rest valid. */
    private static String extraction(String fields) {
        return "{\"departureAirport\":\"DUB\",\"arrivalAirport\":\"CDG\","
                + "\"departureDate\":\"" + FUTURE + "\"," + fields + "}";
    }

    private AssistantResponse ask() {
        return service.answer(new AssistantRequest(QUESTION, null));
    }

    /** The request that actually reached the search. */
    private FlightSearchRequest capturedSearch() {
        ArgumentCaptor<FlightSearchRequest> captor =
                ArgumentCaptor.forClass(FlightSearchRequest.class);
        verify(flightService).searchFlights(captor.capture());
        return captor.getValue();
    }

    @Nested
    @DisplayName("the model cannot invent a flight")
    class Grounding {

        /**
         * The case the whole design exists for. The model is told the results
         * and describes a different airline, a different flight number and a
         * cheaper fare — none of which exist. The prose is the model's to get
         * wrong; the flights are not.
         */
        @Test
        @DisplayName("a summary naming a flight that does not exist does not add one to the response")
        void inventedFlightNeverReachesTheResponse() {
            modelSays(
                    extraction("\"passengers\":1,\"seatClass\":\"ECONOMY\""),
                    "Your best option is Ryanair FR9999 departing at 06:30 for just EUR 19.99, "
                            + "and Lufthansa LH441 also has seats.");

            AssistantResponse response = ask();

            // Exactly the database row, and nothing beside it.
            assertThat(response.flights()).containsExactly(REAL_ROW);
            assertThat(response.flights())
                    .extracting(FlightSearchResponse::flightNumber)
                    .containsExactly("EI512");
            assertThat(response.flights())
                    .extracting(FlightSearchResponse::airlineName)
                    .doesNotContain("Ryanair", "Lufthansa");
            assertThat(response.flights())
                    .extracting(FlightSearchResponse::price)
                    .containsExactly(new BigDecimal("89.99"));
            assertThat(response.needsMoreInfo()).isFalse();
        }

        @Test
        @DisplayName("an empty result set stays empty however the summary reads")
        void inventedFlightCannotFillAnEmptyResult() {
            when(flightService.searchFlights(any(FlightSearchRequest.class)))
                    .thenReturn(new FlightSearchResult(List.of(), null, false));
            modelSays(
                    extraction("\"passengers\":1"),
                    "I found three great options for you, starting at EUR 45.");

            assertThat(ask().flights()).isEmpty();
        }
    }

    @Nested
    @DisplayName("validation before the search")
    class Validation {

        /**
         * The second case the design exists for. An airport the timetable does
         * not hold is discarded, and — critically — the search does not run at
         * all. Passing it through would surface as a 404 from deep inside
         * FlightService for a question the traveller asked in good faith.
         */
        @Test
        @DisplayName("an airport that is not in the timetable never reaches searchFlights")
        void unknownAirportStopsTheSearch() {
            modelSays(
                    "{\"departureAirport\":\"DUB\",\"arrivalAirport\":\"XYZ\","
                            + "\"departureDate\":\"" + FUTURE + "\"}",
                    "unused");

            AssistantResponse response = ask();

            verify(flightService, never()).searchFlights(any(FlightSearchRequest.class));
            assertThat(response.needsMoreInfo()).isTrue();
            assertThat(response.flights()).isEmpty();
            assertThat(response.interpretedAs()).isNull();
            assertThat(response.reply()).containsIgnoringCase("where would you like to go");
        }

        @Test
        @DisplayName("an unknown departure airport also stops the search")
        void unknownDepartureStopsTheSearch() {
            modelSays(
                    "{\"departureAirport\":\"ZZZ\",\"arrivalAirport\":\"CDG\","
                            + "\"departureDate\":\"" + FUTURE + "\"}",
                    "unused");

            assertThat(ask().needsMoreInfo()).isTrue();
            verify(flightService, never()).searchFlights(any(FlightSearchRequest.class));
        }

        @Test
        @DisplayName("a date in the past is refused rather than moved")
        void pastDateIsRefused() {
            LocalDate yesterday = LocalDate.now(ZoneOffset.UTC).minusDays(1);
            modelSays(
                    "{\"departureAirport\":\"DUB\",\"arrivalAirport\":\"CDG\","
                            + "\"departureDate\":\"" + yesterday + "\"}",
                    "unused");

            AssistantResponse response = ask();

            verify(flightService, never()).searchFlights(any(FlightSearchRequest.class));
            assertThat(response.needsMoreInfo()).isTrue();
            assertThat(response.reply()).containsIgnoringCase("which date did you mean");
        }

        @Test
        @DisplayName("today is not in the past")
        void todayIsAcceptable() {
            modelSays(
                    "{\"departureAirport\":\"DUB\",\"arrivalAirport\":\"CDG\","
                            + "\"departureDate\":\"" + LocalDate.now(ZoneOffset.UTC) + "\"}",
                    "Here are today's flights.");

            assertThat(ask().needsMoreInfo()).isFalse();
        }

        @Test
        @DisplayName("the same airport at both ends asks where they are going")
        void identicalAirportsAskForADestination() {
            modelSays(
                    "{\"departureAirport\":\"DUB\",\"arrivalAirport\":\"DUB\","
                            + "\"departureDate\":\"" + FUTURE + "\"}",
                    "unused");

            AssistantResponse response = ask();

            verify(flightService, never()).searchFlights(any(FlightSearchRequest.class));
            assertThat(response.reply()).containsIgnoringCase("where are you flying to");
        }

        /**
         * The timetable is a moving 14-day window, so a date past its end finds
         * nothing however many nearby dates are tried. Answering "no flights,
         * try a day either side" sends someone round a loop that cannot
         * terminate; saying where the window ends lets them pick a date that
         * can work.
         */
        @Test
        @DisplayName("a date past the end of the timetable says so instead of searching")
        void dateBeyondTheHorizonIsExplained() {
            LocalDate lastBookable = LocalDate.now(ZoneOffset.UTC).plusDays(14);
            LocalDate christmas = LocalDate.now(ZoneOffset.UTC).plusMonths(4);
            modelSays(
                    "{\"departureAirport\":\"DUB\",\"arrivalAirport\":\"CDG\","
                            + "\"departureDate\":\"" + christmas + "\"}",
                    "unused");

            AssistantResponse response = ask();

            verify(flightService, never()).searchFlights(any(FlightSearchRequest.class));
            assertThat(response.needsMoreInfo()).isTrue();
            assertThat(response.reply())
                    .contains(DAY.format(lastBookable))
                    .doesNotContain("try a day");
        }

        @Test
        @DisplayName("the last date the timetable covers is still searched")
        void theHorizonItselfIsInclusive() {
            LocalDate lastBookable = LocalDate.now(ZoneOffset.UTC).plusDays(14);
            modelSays(
                    "{\"departureAirport\":\"DUB\",\"arrivalAirport\":\"CDG\","
                            + "\"departureDate\":\"" + lastBookable + "\"}",
                    "Here are your options.");

            AssistantResponse response = ask();

            assertThat(capturedSearch().departureDate()).isEqualTo(lastBookable);
            assertThat(response.needsMoreInfo()).isFalse();
        }

        /**
         * An empty timetable is a deployment problem, not something to explain
         * to a traveller as though they had asked for the wrong date.
         */
        @Test
        @DisplayName("an empty timetable does not block the search")
        void emptyTimetableDoesNotBlock() {
            when(flightService.getLatestDepartureDate()).thenReturn(Optional.empty());
            when(flightService.searchFlights(any(FlightSearchRequest.class)))
                    .thenReturn(new FlightSearchResult(List.of(), null, false));
            modelSays(extraction("\"passengers\":1"), "Nothing found.");

            AssistantResponse response = ask();

            verify(flightService).searchFlights(any(FlightSearchRequest.class));
            assertThat(response.needsMoreInfo()).isFalse();
        }

        @Test
        @DisplayName("a missing date asks for one")
        void missingDateAsksForOne() {
            modelSays("{\"departureAirport\":\"DUB\",\"arrivalAirport\":\"CDG\"}", "unused");

            assertThat(ask().needsMoreInfo()).isTrue();
            verify(flightService, never()).searchFlights(any(FlightSearchRequest.class));
        }
    }

    @Nested
    @DisplayName("values that are corrected rather than questioned")
    class Coercion {

        @ParameterizedTest(name = "cabin \"{0}\" is searched as {1}")
        @CsvSource({
                "BUSINESS,   BUSINESS",
                "business,   BUSINESS",
                "FIRST,      FIRST",
                "PREMIUM,    ECONOMY",
                "'',         ECONOMY",
                "null,       ECONOMY"
        })
        @DisplayName("an unrecognised cabin falls back to economy")
        void unknownCabinFallsBackToEconomy(String given, String expected) {
            String field = "null".equals(given)
                    ? "\"seatClass\":null"
                    : "\"seatClass\":\"" + given + "\"";
            modelSays(extraction(field), "Here are your options.");

            service.answer(new AssistantRequest(QUESTION, null));

            assertThat(capturedSearch().seatClass()).isEqualTo(expected);
        }

        @ParameterizedTest(name = "{0} passengers is searched as {1}")
        @CsvSource({"0, 1", "1, 1", "9, 9", "14, 9", "-3, 1", "250, 9"})
        @DisplayName("a passenger count outside 1-9 is clamped")
        void passengerCountIsClamped(int given, int expected) {
            modelSays(extraction("\"passengers\":" + given), "Here are your options.");

            service.answer(new AssistantRequest(QUESTION, null));

            assertThat(capturedSearch().passengers()).isEqualTo(expected);
        }

        @Test
        @DisplayName("a missing passenger count means one traveller")
        void missingPassengerCountDefaultsToOne() {
            modelSays(extraction("\"passengers\":null"), "Here are your options.");

            service.answer(new AssistantRequest(QUESTION, null));

            assertThat(capturedSearch().passengers()).isEqualTo(1);
        }

        /**
         * Dropped without comment: the outbound search is still the answer the
         * traveller wanted, and a question about it would be pedantry.
         */
        @Test
        @DisplayName("a return date before the outbound date is dropped silently")
        void backwardsReturnDateIsDropped() {
            modelSays(
                    "{\"departureAirport\":\"DUB\",\"arrivalAirport\":\"CDG\","
                            + "\"departureDate\":\"" + FUTURE + "\","
                            + "\"returnDate\":\"" + FUTURE.minusDays(3) + "\"}",
                    "Here are your options.");

            AssistantResponse response = ask();

            assertThat(capturedSearch().returnDate()).isNull();
            assertThat(response.needsMoreInfo()).isFalse();
            assertThat(response.flights()).containsExactly(REAL_ROW);
        }

        @Test
        @DisplayName("a return date after the outbound date is kept")
        void forwardReturnDateSurvives() {
            LocalDate back = FUTURE.plusDays(4);
            modelSays(
                    "{\"departureAirport\":\"DUB\",\"arrivalAirport\":\"CDG\","
                            + "\"departureDate\":\"" + FUTURE + "\","
                            + "\"returnDate\":\"" + back + "\"}",
                    "Here are your options.");

            ask();

            assertThat(capturedSearch().returnDate()).isEqualTo(back);
        }

        @Test
        @DisplayName("codes are normalised to the casing the timetable uses")
        void lowercaseCodesResolve() {
            modelSays(
                    "{\"departureAirport\":\"dub\",\"arrivalAirport\":\" cdg \","
                            + "\"departureDate\":\"" + FUTURE + "\"}",
                    "Here are your options.");

            ask();

            assertThat(capturedSearch().departureAirport()).isEqualTo("DUB");
            assertThat(capturedSearch().arrivalAirport()).isEqualTo("CDG");
        }
    }

    @Nested
    @DisplayName("the origin hint")
    class OriginHint {

        @Test
        @DisplayName("fills in an origin the message did not give")
        void hintFillsAMissingOrigin() {
            modelSays(
                    "{\"arrivalAirport\":\"CDG\",\"departureDate\":\"" + FUTURE + "\"}",
                    "Here are your options.");

            service.answer(new AssistantRequest(QUESTION, "MAD"));

            assertThat(capturedSearch().departureAirport()).isEqualTo("MAD");
        }

        @Test
        @DisplayName("does not override an origin the message did give")
        void messageBeatsHint() {
            modelSays(extraction("\"passengers\":1"), "Here are your options.");

            service.answer(new AssistantRequest(QUESTION, "MAD"));

            assertThat(capturedSearch().departureAirport()).isEqualTo("DUB");
        }

        /**
         * The hint comes from a client, so it is checked like anything else.
         *
         * <p>LHR, not a malformed string: a well-formed code for an airport the
         * network does not serve is the case that reaches the database lookup.
         * Anything obviously wrong is stopped by the length guard first, which
         * makes it useless as a test of the lookup.
         */
        @Test
        @DisplayName("a well-formed code for an unserved airport is rejected by the lookup")
        void unknownHintIsRejected() {
            modelSays(
                    "{\"arrivalAirport\":\"CDG\",\"departureDate\":\"" + FUTURE + "\"}",
                    "unused");

            AssistantResponse response =
                    service.answer(new AssistantRequest(QUESTION, "LHR"));

            verify(flightService, never()).searchFlights(any(FlightSearchRequest.class));
            assertThat(response.reply()).containsIgnoringCase("which airport are you flying from");
        }

        @Test
        @DisplayName("a hint that is not an airport code at all is rejected before the lookup")
        void malformedHintIsRejected() {
            modelSays(
                    "{\"arrivalAirport\":\"CDG\",\"departureDate\":\"" + FUTURE + "\"}",
                    "unused");

            AssistantResponse response =
                    service.answer(new AssistantRequest(QUESTION, "'; DROP TABLE flights; --"));

            verify(flightService, never()).searchFlights(any(FlightSearchRequest.class));
            assertThat(response.reply()).containsIgnoringCase("which airport are you flying from");
        }
    }

    @Nested
    @DisplayName("unusable model output")
    class BadOutput {

        @Test
        @DisplayName("output that is not JSON asks for a rephrase instead of guessing")
        void unparseableOutputNeverGuesses() {
            modelSays("I think you probably want to fly to Paris on Friday!", "unused");

            AssistantResponse response = ask();

            verify(flightService, never()).searchFlights(any(FlightSearchRequest.class));
            assertThat(response.needsMoreInfo()).isTrue();
            assertThat(response.reply()).containsIgnoringCase("rephrase");
        }

        @Test
        @DisplayName("truncated JSON is treated the same way")
        void truncatedJsonNeverGuesses() {
            modelSays("{\"departureAirport\":\"DUB\",\"arrivalAir", "unused");

            assertThat(ask().needsMoreInfo()).isTrue();
            verify(flightService, never()).searchFlights(any(FlightSearchRequest.class));
        }

        @Test
        @DisplayName("JSON wrapped in a markdown fence is still read")
        void fencedJsonIsUnwrapped() {
            modelSays("```json\n" + extraction("\"passengers\":2") + "\n```",
                    "Here are your options.");

            AssistantResponse response = ask();

            assertThat(response.needsMoreInfo()).isFalse();
            assertThat(capturedSearch().passengers()).isEqualTo(2);
        }

        @Test
        @DisplayName("a clarification from the model is passed through and stops the search")
        void clarificationStopsTheSearch() {
            modelSays("{\"clarification\":\"Which city would you like to fly to?\"}", "unused");

            AssistantResponse response = ask();

            verify(flightService, never()).searchFlights(any(FlightSearchRequest.class));
            assertThat(response.reply()).isEqualTo("Which city would you like to fly to?");
            assertThat(response.needsMoreInfo()).isTrue();
        }

        /**
         * Found against the live model, not the mock.
         *
         * <p>Asked for a flight with no origin, the real model fills in
         * `clarification` <em>and</em> the fields it could read. Returning on
         * the clarification made originHint unreachable in the one case it
         * exists for. The mock never showed this because the scripted output
         * for that test set no clarification — a shape the real model does not
         * produce for that input.
         */
        @Test
        @DisplayName("the origin hint answers the model's question rather than being skipped by it")
        void hintOutranksAClarificationItCanAnswer() {
            modelSays(
                    "{\"arrivalAirport\":\"CDG\",\"departureDate\":\"" + FUTURE + "\","
                            + "\"clarification\":\"Which city will you be departing from?\"}",
                    "Here are your options.");

            AssistantResponse response =
                    service.answer(new AssistantRequest(QUESTION, "MAD"));

            assertThat(capturedSearch().departureAirport()).isEqualTo("MAD");
            assertThat(response.needsMoreInfo()).isFalse();
            assertThat(response.flights()).containsExactly(REAL_ROW);
        }

        /**
         * Same root cause: a clarification must not stand in for a rule the
         * service is supposed to apply itself. Asked for thirty seats, the
         * model queries the group size; the spec says clamp and search.
         */
        @Test
        @DisplayName("a clarification does not pre-empt the passenger clamp")
        void clarificationDoesNotPreEmptTheClamp() {
            modelSays(
                    extraction("\"passengers\":30,"
                            + "\"clarification\":\"I can only book up to 9 passengers.\""),
                    "Here are your options.");

            AssistantResponse response = ask();

            assertThat(capturedSearch().passengers()).isEqualTo(9);
            assertThat(response.needsMoreInfo()).isFalse();
        }

        /** When a question really is needed, the model's wording is the better one. */
        @Test
        @DisplayName("prefers the model's wording to the generic fallback")
        void modelWordingWinsWhenAQuestionIsNeeded() {
            modelSays(
                    "{\"departureAirport\":\"DUB\",\"departureDate\":\"" + FUTURE + "\","
                            + "\"clarification\":\"SkyAir does not serve Reykjavik.\"}",
                    "unused");

            AssistantResponse response = ask();

            verify(flightService, never()).searchFlights(any(FlightSearchRequest.class));
            assertThat(response.reply()).isEqualTo("SkyAir does not serve Reykjavik.");
        }
    }

    @Nested
    @DisplayName("degradation")
    class Degradation {

        /**
         * The search succeeded, so the answer exists. Losing the wording must
         * not lose it.
         */
        @Test
        @DisplayName("a summarisation failure still returns the flights")
        void summaryFailureKeepsTheFlights() {
            when(claudeClient.complete(anyString(), anyString())).thenAnswer(invocation -> {
                String systemPrompt = invocation.getArgument(0);
                if (systemPrompt.contains("You convert a traveller's message")) {
                    return extraction("\"passengers\":1");
                }
                throw new ExternalServiceException("AI travel assistant", "the model could not be reached");
            });

            AssistantResponse response = ask();

            assertThat(response.flights()).containsExactly(REAL_ROW);
            assertThat(response.needsMoreInfo()).isFalse();
            assertThat(response.interpretedAs()).isNotNull();
            // Generated locally from the same rows, so it still says something true.
            assertThat(response.reply()).contains("1 flight", "DUB", "CDG", "89.99");
        }

        @Test
        @DisplayName("an extraction failure is a 503, because there is nothing to answer with")
        void extractionFailurePropagates() {
            when(claudeClient.complete(anyString(), anyString()))
                    .thenThrow(new ExternalServiceException("AI travel assistant", "the model could not be reached"));

            assertThatThrownBy(TravelAssistantServiceTest.this::ask)
                    .isInstanceOf(ExternalServiceException.class);

            verify(flightService, never()).searchFlights(any(FlightSearchRequest.class));
        }

        @Test
        @DisplayName("an unconfigured assistant fails before any work is done")
        void unconfiguredFailsFast() {
            when(claudeClient.isConfigured()).thenReturn(false);

            assertThatThrownBy(TravelAssistantServiceTest.this::ask)
                    .isInstanceOf(ExternalServiceException.class)
                    .hasMessageContaining("no Anthropic API key is configured");

            verify(claudeClient, never()).complete(anyString(), anyString());
            verify(airportRepository, never()).findAll();
        }
    }

    @Nested
    @DisplayName("the extraction prompt")
    class Prompt {

        /**
         * Two facts the model cannot supply itself: which airports exist, and
         * what day it is. Without either, "Paris next Friday" is unanswerable.
         */
        @Test
        @DisplayName("carries the airport list and today's date")
        void promptCarriesTheGroundTruth() {
            modelSays(extraction("\"passengers\":1"), "Here are your options.");

            ask();

            ArgumentCaptor<String> systemPrompt = ArgumentCaptor.forClass(String.class);
            verify(claudeClient, org.mockito.Mockito.atLeastOnce())
                    .complete(systemPrompt.capture(), anyString());

            String extractionPrompt = systemPrompt.getAllValues().stream()
                    .filter(p -> p.contains("You convert a traveller's message"))
                    .findFirst()
                    .orElseThrow();

            assertThat(extractionPrompt)
                    .contains("DUB", "Dublin", "CDG", "Paris", "MAD", "Madrid")
                    .contains(String.valueOf(LocalDate.now(ZoneOffset.UTC).getYear()))
                    .contains("Today is");
        }
    }
}
