package com.airlinebookingsystem.controller;

import com.airlinebookingsystem.dto.live.LiveTrafficResponse;
import com.airlinebookingsystem.service.OpenSkyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Server-side proxy over the OpenSky Network.
 *
 * <p>The proxy exists because OpenSky sends no CORS headers (so the browser
 * cannot call it directly) and because its free tier is credit-limited, which
 * requires shared server-side caching rather than per-client requests.
 */
@RestController
@RequestMapping("/api/v1/live-flights")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
@Tag(name = "Live flights", description = "Real-time aircraft positions from the OpenSky Network")
public class LiveFlightController {

    private final OpenSkyService openSkyService;

    @Operation(summary = "Get live air traffic",
            description = "Returns aircraft currently transmitting ADS-B over the configured region. "
                    + "Results are cached server-side to respect OpenSky's free daily credit allowance.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Traffic snapshot returned"),
            @ApiResponse(responseCode = "503", description = "OpenSky Network unavailable")
    })
    @SecurityRequirements
    @GetMapping
    public ResponseEntity<LiveTrafficResponse> getLiveTraffic() {
        log.info("GET /live-flights");
        return ResponseEntity.ok(openSkyService.getLiveTraffic());
    }
}
