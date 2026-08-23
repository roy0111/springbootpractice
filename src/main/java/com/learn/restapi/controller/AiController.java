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
@Tag(name = "04. Spring AI (Claude LLM & RAG)", description = "Spring AI integration with Anthropic Claude LLM & VectorStore RAG")
public class AiController {

    private final AiService aiService;
    private final com.learn.restapi.service.RagService ragService;

    public AiController(AiService aiService, com.learn.restapi.service.RagService ragService) {
        this.aiService = aiService;
        this.ragService = ragService;
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

    @GetMapping("/rag")
    @Operation(summary = "Query RAG pipeline", description = "Executes Retrieval-Augmented Generation by querying VectorStore before prompting Claude LLM.")
    public Mono<Map<String, Object>> rag(@RequestParam(defaultValue = "What reactive features does Spring Boot support?") String query) {
        return ragService.queryRag(query);
    }
}
