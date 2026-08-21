package com.airlinebookingsystem.controller;

import com.airlinebookingsystem.config.PasswordEncoderConfig;
import com.airlinebookingsystem.config.SecurityConfig;
import com.airlinebookingsystem.dto.assistant.AssistantRequest;
import com.airlinebookingsystem.dto.assistant.AssistantResponse;
import com.airlinebookingsystem.exception.ExternalServiceException;
import com.airlinebookingsystem.security.JwtService;
import com.airlinebookingsystem.security.RestAuthenticationEntryPoint;
import com.airlinebookingsystem.service.ClaudeClient;
import com.airlinebookingsystem.service.TravelAssistantService;
import com.airlinebookingsystem.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer tests for AssistantController.
 *
 * <p>The real SecurityConfig is imported, without which Boot's test defaults
 * secure everything and these requests would be rejected before reaching the
 * controller. Importing it is what makes the point of these tests provable:
 * every request below is unauthenticated, so a 200 means the route really is
 * public rather than the slice being permissive.
 */
@WebMvcTest(controllers = AssistantController.class)
@Import({SecurityConfig.class, PasswordEncoderConfig.class, RestAuthenticationEntryPoint.class})
class AssistantControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private TravelAssistantService travelAssistantService;
    @MockitoBean private ClaudeClient claudeClient;

    // The imported chain installs JwtAuthFilter, so its collaborators have to
    // exist. No request here carries a token, so the filter just passes through.
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserService userService;

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private String askBody(String message) throws Exception {
        return json(new AssistantRequest(message, null));
    }

    @Nested
    @DisplayName("POST /api/v1/assistant")
    class Ask {

        @Test
        @DisplayName("is public, and answers with the reply and the flights")
        void answersWithoutAToken() throws Exception {
            when(travelAssistantService.answer(any(AssistantRequest.class)))
                    .thenReturn(new AssistantResponse("One flight found.", List.of(), null, false));

            mockMvc.perform(post("/api/v1/assistant")
                            .contentType("application/json")
                            .content(askBody("cheapest flight to Paris next Friday")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.reply").value("One flight found."))
                    .andExpect(jsonPath("$.needsMoreInfo").value(false));
        }

        @Test
        @DisplayName("returns 400 and does not call the service when the message is blank")
        void rejectsBlankMessage() throws Exception {
            mockMvc.perform(post("/api/v1/assistant")
                            .contentType("application/json")
                            .content(askBody("   ")))
                    .andExpect(status().isBadRequest());

            verify(travelAssistantService, never()).answer(any(AssistantRequest.class));
        }

        @Test
        @DisplayName("returns 400 when the message is longer than the limit")
        void rejectsOverlongMessage() throws Exception {
            mockMvc.perform(post("/api/v1/assistant")
                            .contentType("application/json")
                            .content(askBody("a".repeat(501))))
                    .andExpect(status().isBadRequest());

            verify(travelAssistantService, never()).answer(any(AssistantRequest.class));
        }

        /**
         * The distinction the frontend depends on. An unconfigured or
         * unreachable assistant is not a fault in this application, and
         * answering 500 would put it in the same bucket as a genuine bug.
         */
        @Test
        @DisplayName("returns 503, not 500, when the assistant is unconfigured")
        void unconfiguredIsServiceUnavailable() throws Exception {
            when(travelAssistantService.answer(any(AssistantRequest.class)))
                    .thenThrow(new ExternalServiceException(
                            "AI travel assistant", "no Anthropic API key is configured"));

            mockMvc.perform(post("/api/v1/assistant")
                            .contentType("application/json")
                            .content(askBody("anything")))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.status").value(503));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/assistant/status")
    class Status {

        @Test
        @DisplayName("is public and reports availability when a key is present")
        void reportsAvailable() throws Exception {
            when(claudeClient.isConfigured()).thenReturn(true);

            mockMvc.perform(get("/api/v1/assistant/status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.available").value(true));
        }

        /**
         * Must stay a 200 with {@code available:false}. Answering an error here
         * would make "switched off" indistinguishable from "broken", and the
         * frontend hides the feature on the strength of this one field.
         */
        @Test
        @DisplayName("reports unavailable with 200, not an error, when no key is set")
        void reportsUnavailable() throws Exception {
            when(claudeClient.isConfigured()).thenReturn(false);

            mockMvc.perform(get("/api/v1/assistant/status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.available").value(false));
        }
    }
}
