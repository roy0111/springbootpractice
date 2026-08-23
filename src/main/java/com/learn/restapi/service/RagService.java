package com.learn.restapi.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Service demonstrating Retrieval-Augmented Generation (RAG) using Spring AI.
 *
 * <p>RAG Workflow:
 * <ol>
 *   <li><b>Indexing / Ingestion:</b> Convert domain knowledge documents into vector embeddings in a {@link VectorStore}.</li>
 *   <li><b>Retrieval:</b> Query vector store for top matching document chunks relevant to prompt.</li>
 *   <li><b>Augmentation & Generation:</b> Combine prompt + retrieved context into LLM prompt via {@link ChatClient}.</li>
 * </ol>
 */
@Service
public class RagService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    public RagService(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
        this.chatClient = chatClientBuilder.build();
        this.vectorStore = vectorStore;
        initSampleKnowledgeBase();
    }

    /**
     * Seeds in-memory VectorStore with sample domain knowledge documents.
     */
    private void initSampleKnowledgeBase() {
        List<Document> documents = List.of(
                new Document("Spring Boot 3.3.2 supports reactive stack with WebFlux and R2DBC for high throughput non-blocking APIs.", Map.of("source", "tech-spec")),
                new Document("Resilience4j Circuit Breaker transitions between CLOSED, OPEN, and HALF_OPEN states to prevent cascade failures.", Map.of("source", "resilience-guide")),
                new Document("Spring AI provides ChatClient and VectorStore abstractions to integrate LLM models like Anthropic Claude.", Map.of("source", "ai-docs"))
        );
        vectorStore.add(documents);
    }

    /**
     * Executes RAG (Retrieval-Augmented Generation) query pipeline.
     *
     * @param query User question
     * @return Mono wrapping RAG response and retrieved background context
     */
    public Mono<Map<String, Object>> queryRag(String query) {
        return Mono.fromCallable(() -> {
            // 1. Retrieve relevant documents from VectorStore
            List<Document> similarDocuments = vectorStore.similaritySearch(query);

            String context = similarDocuments.stream()
                    .map(Document::getContent)
                    .reduce("", (a, b) -> a + "\n- " + b);

            // 2. Augment prompt with context and send to Claude LLM
            String augmentedPrompt = String.format("""
                    Context Information:
                    %s

                    User Question: %s

                    Answer the question based strictly on the context provided above.
                    """, context, query);

            String response = chatClient.prompt()
                    .user(augmentedPrompt)
                    .call()
                    .content();

            return Map.of(
                    "query", query,
                    "retrievedContextCount", similarDocuments.size(),
                    "retrievedContext", context,
                    "aiAnswer", response
            );
        });
    }
}
