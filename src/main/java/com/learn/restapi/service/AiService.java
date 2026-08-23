package com.learn.restapi.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service providing simple learning examples for Spring AI with Anthropic Claude LLM.
 *
 * <p>Supports both simple sync/async ({@link Mono}) and streaming ({@link Flux}) responses.
 */
@Service
public class AiService {

    private final ChatClient chatClient;

    public AiService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * Sends a simple prompt to Claude and returns the generated text completion as a Mono.
     *
     * @param prompt The question or command for Claude
     * @return Mono containing Claude's text response
     */
    public Mono<String> generateCompletion(String prompt) {
        return Mono.fromCallable(() ->
                chatClient.prompt()
                        .user(prompt)
                        .call()
                        .content()
        );
    }

    /**
     * Streams Claude's response token-by-token reactively as a Flux stream.
     *
     * @param prompt The question or command for Claude
     * @return Flux of response text chunks stream
     */
    public Flux<String> streamCompletion(String prompt) {
        return chatClient.prompt()
                .user(prompt)
                .stream()
                .content();
    }
}
