package com.airlinebookingsystem.controller;

import com.airlinebookingsystem.dto.assistant.AssistantRequest;
import com.airlinebookingsystem.dto.assistant.AssistantResponse;
import com.airlinebookingsystem.dto.assistant.AssistantStatusResponse;
import com.airlinebookingsystem.service.ClaudeClient;
import com.airlinebookingsystem.service.TravelAssistantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Natural-language flight search.
 *
 * <p>Public, like the search endpoints it delegates to — asking about flights
 * requires no account, and requiring one here would put a sign-in in front of
 * the first thing a visitor tries.
 */
@RestController
@RequestMapping("/api/v1/assistant")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
@Tag(name = "Travel assistant", description = "Ask for flights in plain English")
public class AssistantController {

    private final TravelAssistantService travelAssistantService;
    private final ClaudeClient claudeClient;

    @Operation(summary = "Ask for flights in plain English",
            description = "Reads the message into a flight search, runs the ordinary search "
                    + "against the timetable, and describes what came back. Every flight in "
                    + "the response is a database row; the model contributes the wording only. "
                    + "When something has to be asked first — an unserved airport, a date that "
                    + "has passed — no search runs and needsMoreInfo is true.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Answered, or a question returned",
                    content = @Content(schema = @Schema(implementation = AssistantResponse.class))),
            @ApiResponse(responseCode = "400", description = "Message missing or too long", content = @Content),
            @ApiResponse(responseCode = "503", description = "Assistant not configured, or the model is unreachable",
                    content = @Content)
    })
    @SecurityRequirements
    @PostMapping
    public ResponseEntity<AssistantResponse> ask(@Valid @RequestBody AssistantRequest request) {
        // The message itself is not logged: it is a visitor's own words, and the
        // service already logs everything needed to follow what happened.
        log.info("POST /assistant");
        return ResponseEntity.ok(travelAssistantService.answer(request));
    }

    @Operation(summary = "Whether the assistant is available",
            description = "False when no API key is configured. The frontend polls this on load "
                    + "so the feature can be hidden rather than offered and then failing.")
    @ApiResponse(responseCode = "200", description = "Availability returned")
    @SecurityRequirements
    @GetMapping("/status")
    public ResponseEntity<AssistantStatusResponse> status() {
        return ResponseEntity.ok(new AssistantStatusResponse(claudeClient.isConfigured()));
    }
}
