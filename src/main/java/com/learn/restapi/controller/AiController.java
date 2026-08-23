package com.learn.restapi.controller;

import com.learn.restapi.service.AiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * REST Controller exposing Spring AI + Claude LLM endpoints.
 *
 * <p>Demonstrates:
 * <ul>
 *   <li>{@code /api/ai/generate} - Sync/Mono response from Claude</li>
 *   <li>{@code /api/ai/stream} - Streaming token response (SSE / text-event-stream)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/ai")
@Tag(name = "04. Spring AI (Claude LLM)", description = "Spring AI integration with Anthropic Claude LLM models")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @GetMapping("/generate")
    @Operation(summary = "Generate AI completion", description = "Ask Anthropic Claude a question and get a single complete text response.")
    public Mono<Map<String, Object>> generate(@RequestParam(defaultValue = "Explain Spring Boot in 2 sentences.") String prompt) {
        return aiService.generateCompletion(prompt)
                .map(response -> Map.of(
                        "model", "claude-3-5-sonnet",
                        "prompt", prompt,
                        "response", response
                ));
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Stream AI completion tokens", description = "Streams Claude AI token responses reactively in real-time (Server-Sent Events).")
    public Flux<String> stream(@RequestParam(defaultValue = "Write a short poem about coding in Java.") String prompt) {
        return aiService.streamCompletion(prompt);
    }
}
