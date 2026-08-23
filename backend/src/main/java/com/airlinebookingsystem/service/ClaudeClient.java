package com.airlinebookingsystem.service;

import com.airlinebookingsystem.exception.ExternalServiceException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;

/**
 * A thin wrapper over the Anthropic Messages API.
 *
 * <p>Knows nothing about flights on purpose: a mock of this class can be told
 * to say anything, which is the only way to test that the grounding rules hold
 * for output no real model can be asked to produce on cue.
 *
 * <p>A blank {@code anthropic.api-key} is a switched-off feature, not a
 * misconfiguration — hence the {@code :} default on the property, and a startup
 * log rather than a failed context.
 *
 * <p>{@link ExternalServiceException} is the failure type throughout because
 * the global handler maps it to 503: nothing that goes wrong out here is a
 * fault in this application.
 */
@Service
@Slf4j
public class ClaudeClient {

    /** Pinned per Anthropic's versioning scheme; unrelated to the model. */
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    /**
     * Thinking stays on — it is what resolves "next Friday" — but at low
     * effort, because a chat box taking twenty seconds reads as broken.
     * Disabling it outright is the worse trade: reasoning can leak into the
     * visible text.
     */
    private static final String EFFORT = "low";

    private final RestClient restClient;

    @Value("${anthropic.base-url:https://api.anthropic.com/v1}")
    private String baseUrl;

    @Value("${anthropic.api-key:}")
    private String apiKey;

    @Value("${anthropic.model:claude-sonnet-5}")
    private String model;

    @Value("${anthropic.max-tokens:8192}")
    private int maxTokens;

    public ClaudeClient(RestClient.Builder restClientBuilder) {
        // The default factory has no read timeout at all, so a stalled upstream
        // would hold the request thread indefinitely. A model call is slower
        // than an ordinary HTTP call, hence the generous but finite ceiling.
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(60));

        this.restClient = restClientBuilder.requestFactory(requestFactory).build();
    }

    @PostConstruct
    void logConfiguration() {
        if (isConfigured()) {
            log.info("Claude client configured — model {}, max_tokens {}", model, maxTokens);
        } else {
            log.info("Claude client has no API key; the travel assistant is disabled");
        }
    }

    /** Whether an API key is present. */
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * Sends one message and returns the model's text.
     *
     * <p>Single-turn by design: the assistant makes two independent calls with
     * no shared history, so there is no conversation to carry.
     *
     * @return the concatenated text blocks, never null or blank
     * @throws ExternalServiceException on a missing key, a failed call, or a
     *                                  response with no usable text
     */
    public String complete(String systemPrompt, String userMessage) {
        if (!isConfigured()) {
            throw new ExternalServiceException(
                    "AI travel assistant", "no Anthropic API key is configured");
        }

        MessagesRequest body = new MessagesRequest(
                model,
                maxTokens,
                systemPrompt,
                List.of(new Message("user", userMessage)),
                new OutputConfig(EFFORT));

        MessagesResponse response;
        try {
            response = restClient.post()
                    .uri(baseUrl + "/messages")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", ANTHROPIC_VERSION)
                    .body(body)
                    .retrieve()
                    .body(MessagesResponse.class);
        } catch (Exception ex) {
            // The message is logged rather than returned: an upstream error body
            // can echo request content, and this one contains a user's message.
            log.warn("Claude request failed: {}", ex.getMessage());
            throw new ExternalServiceException("AI travel assistant", "the model could not be reached");
        }

        if (response == null || response.content() == null) {
            throw new ExternalServiceException("AI travel assistant", "the model returned no content");
        }

        // A safety classifier can decline a request: HTTP 200, no usable text.
        if ("refusal".equals(response.stopReason())) {
            log.warn("Claude declined the request");
            throw new ExternalServiceException("AI travel assistant", "the model declined this request");
        }

        if ("max_tokens".equals(response.stopReason())) {
            // Worth seeing, because a truncated extraction reappears downstream
            // as unparseable JSON, which looks like a prompting problem instead.
            log.warn("Claude response hit max_tokens ({}); output is truncated", maxTokens);
        }

        // Thinking blocks share the content array with text; only text is wanted.
        String text = response.content().stream()
                .filter(block -> "text".equals(block.type()))
                .map(ContentBlock::text)
                .filter(t -> t != null && !t.isBlank())
                .reduce("", (a, b) -> a.isEmpty() ? b : a + "\n" + b);

        if (text.isBlank()) {
            throw new ExternalServiceException("AI travel assistant", "the model returned no text");
        }

        return text;
    }

    // ---- Wire format ------------------------------------------------------
    // Records mirroring the Messages API payloads. Kept private: nothing
    // outside this class should be shaped by Anthropic's JSON.

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record MessagesRequest(
            String model,
            @JsonProperty("max_tokens") int maxTokens,
            String system,
            List<Message> messages,
            @JsonProperty("output_config") OutputConfig outputConfig
    ) {}

    private record Message(String role, String content) {}

    private record OutputConfig(String effort) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record MessagesResponse(
            List<ContentBlock> content,
            @JsonProperty("stop_reason") String stopReason
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ContentBlock(String type, String text) {}
}
