package com.airlinebookingsystem.service;

import com.airlinebookingsystem.dto.live.LiveFlightResponse;
import com.airlinebookingsystem.dto.live.LiveTrafficResponse;
import com.airlinebookingsystem.exception.ExternalServiceException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Reads live ADS-B aircraft positions from the OpenSky Network.
 *
 * <p>OpenSky's anonymous tier allows 400 API credits per day, and a bounding-box
 * request costs 1-4 credits depending on the area covered. Responses are
 * therefore cached for {@code opensky.cache-ttl-seconds} so that page views do
 * not map one-to-one onto upstream calls. With the default 10-minute TTL and a
 * small bounding box, worst-case usage stays comfortably inside the allowance.
 *
 * <p>No API key is required, which is precisely why this lives on the backend
 * rather than in the browser: OpenSky does not send CORS headers, so a direct
 * frontend call would be blocked regardless.
 */
@Service
@Slf4j
public class OpenSkyService {

    private static final double METRES_TO_FEET = 3.28084;
    private static final double MPS_TO_KNOTS = 1.94384;

    // Indices into OpenSky's positional "state vector" arrays.
    private static final int IDX_ICAO24 = 0;
    private static final int IDX_CALLSIGN = 1;
    private static final int IDX_ORIGIN_COUNTRY = 2;
    private static final int IDX_LONGITUDE = 5;
    private static final int IDX_LATITUDE = 6;
    private static final int IDX_BARO_ALTITUDE = 7;
    private static final int IDX_ON_GROUND = 8;
    private static final int IDX_VELOCITY = 9;
    private static final int IDX_TRUE_TRACK = 10;
    private static final int IDX_GEO_ALTITUDE = 13;

    private final RestClient restClient;

    @Value("${opensky.base-url:https://opensky-network.org/api}")
    private String baseUrl;

    @Value("${opensky.bbox.lamin:49.0}")
    private double laMin;

    @Value("${opensky.bbox.lomin:-11.0}")
    private double loMin;

    @Value("${opensky.bbox.lamax:56.0}")
    private double laMax;

    @Value("${opensky.bbox.lomax:2.0}")
    private double loMax;

    @Value("${opensky.region-name:Ireland & UK}")
    private String regionName;

    @Value("${opensky.cache-ttl-seconds:600}")
    private long cacheTtlSeconds;

    @Value("${opensky.max-results:20}")
    private int maxResults;

    private final AtomicReference<CachedTraffic> cache = new AtomicReference<>();

    public OpenSkyService(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    @PostConstruct
    void logConfiguration() {
        log.info("OpenSky live traffic configured for [{}] bbox({},{})-({},{}), cache {}s",
                regionName, laMin, loMin, laMax, loMax, cacheTtlSeconds);
    }

    /**
     * Returns the current traffic snapshot, refreshing from OpenSky only once the
     * cached copy has expired. If a refresh fails but a stale copy exists, the
     * stale copy is served rather than failing the request.
     */
    public LiveTrafficResponse getLiveTraffic() {
        CachedTraffic cached = cache.get();
        if (cached != null && !cached.isOlderThan(cacheTtlSeconds)) {
            return cached.payload();
        }

        try {
            LiveTrafficResponse fresh = fetchFromOpenSky();
            cache.set(new CachedTraffic(fresh, Instant.now()));
            return fresh;
        } catch (ExternalServiceException ex) {
            if (cached != null) {
                log.warn("OpenSky refresh failed, serving stale snapshot: {}", ex.getMessage());
                return cached.payload();
            }
            throw ex;
        }
    }

    private LiveTrafficResponse fetchFromOpenSky() {
        String url = String.format("%s/states/all?lamin=%s&lomin=%s&lamax=%s&lomax=%s",
                baseUrl, laMin, loMin, laMax, loMax);
        log.info("Fetching live traffic from OpenSky for region [{}]", regionName);

        OpenSkyStatesResponse response;
        try {
            response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(OpenSkyStatesResponse.class);
        } catch (Exception ex) {
            throw new ExternalServiceException("OpenSky Network", ex.getMessage());
        }

        if (response == null || response.states() == null) {
            // A valid response with no aircraft is legitimate (e.g. tiny bbox at night).
            return new LiveTrafficResponse(List.of(), 0, regionName, LocalDateTime.now());
        }

        List<LiveFlightResponse> flights = new ArrayList<>();
        for (List<Object> state : response.states()) {
            LiveFlightResponse mapped = mapState(state);
            if (mapped != null) {
                flights.add(mapped);
            }
        }

        int totalTracked = flights.size();
        List<LiveFlightResponse> topFlights = flights.stream()
                .sorted(Comparator.comparing(
                        LiveFlightResponse::altitudeFeet,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(Math.max(1, maxResults))
                .toList();

        LocalDateTime retrievedAt = response.time() != null
                ? LocalDateTime.ofInstant(Instant.ofEpochSecond(response.time()), ZoneId.systemDefault())
                : LocalDateTime.now();

        log.info("OpenSky returned {} aircraft over [{}]", totalTracked, regionName);
        return new LiveTrafficResponse(topFlights, totalTracked, regionName, retrievedAt);
    }

    /** Returns null for state vectors without a usable position. */
    private LiveFlightResponse mapState(List<Object> state) {
        if (state == null) return null;

        Double latitude = asDouble(state, IDX_LATITUDE);
        Double longitude = asDouble(state, IDX_LONGITUDE);
        if (latitude == null || longitude == null) return null;

        Double altitudeMetres = asDouble(state, IDX_BARO_ALTITUDE);
        if (altitudeMetres == null) {
            altitudeMetres = asDouble(state, IDX_GEO_ALTITUDE);
        }
        Double velocityMps = asDouble(state, IDX_VELOCITY);
        Double trueTrack = asDouble(state, IDX_TRUE_TRACK);

        String callsign = asString(state, IDX_CALLSIGN);
        if (callsign == null || callsign.isBlank()) {
            callsign = "Unknown";
        }

        return new LiveFlightResponse(
                asString(state, IDX_ICAO24),
                callsign,
                asString(state, IDX_ORIGIN_COUNTRY),
                latitude,
                longitude,
                altitudeMetres == null ? null : (int) Math.round(altitudeMetres * METRES_TO_FEET),
                velocityMps == null ? null : (int) Math.round(velocityMps * MPS_TO_KNOTS),
                trueTrack == null ? null : (int) Math.round(trueTrack),
                asBoolean(state, IDX_ON_GROUND)
        );
    }

    private static String asString(List<Object> state, int index) {
        Object value = index < state.size() ? state.get(index) : null;
        return value == null ? null : value.toString().trim();
    }

    private static Double asDouble(List<Object> state, int index) {
        Object value = index < state.size() ? state.get(index) : null;
        return value instanceof Number number ? number.doubleValue() : null;
    }

    private static boolean asBoolean(List<Object> state, int index) {
        Object value = index < state.size() ? state.get(index) : null;
        return value instanceof Boolean bool && bool;
    }

    /** Raw OpenSky payload: a timestamp plus untyped positional state vectors. */
    private record OpenSkyStatesResponse(Long time, List<List<Object>> states) {}

    private record CachedTraffic(LiveTrafficResponse payload, Instant fetchedAt) {
        boolean isOlderThan(long seconds) {
            return Duration.between(fetchedAt, Instant.now()).getSeconds() >= seconds;
        }
    }
}
