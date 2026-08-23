package com.learn.restapi.service;

import com.learn.restapi.config.KafkaConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

/**
 * Service class demonstrating clean Kafka Producer and dynamic Consumer for learning.
 *
 * <ul>
 *   <li><b>Producer:</b> Sends messages asynchronously via {@link KafkaTemplate}.</li>
 *   <li><b>Dynamic Consumer:</b> Uses Spring's {@code concurrency = "${spring.kafka.listener.concurrency:5}"}
 *       property to dynamically spawn consumer thread instances in production without hardcoded methods.</li>
 * </ul>
 */
@Service
public class KafkaService {

    private static final Logger log = LoggerFactory.getLogger(KafkaService.class);

    private final KafkaTemplate<String, String> kafkaTemplate;

    public KafkaService(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  KAFKA PRODUCER
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Sends a message to the specified Kafka topic asynchronously.
     *
     * @param key     Message key (used for partition routing)
     * @param message Message payload
     * @return Mono wrapping result status string
     */
    public Mono<String> sendMessage(String key, String message) {
        return Mono.fromFuture(() -> {
            CompletableFuture<SendResult<String, String>> future =
                    kafkaTemplate.send(KafkaConfig.TOPIC_NAME, key, message);

            return future.thenApply(result -> {
                log.info("[Kafka Producer] Sent message key='{}' to partition={} offset={}",
                        key, result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                return String.format("Successfully sent message to partition %d with offset %d",
                        result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
            }).exceptionally(ex -> {
                log.error("[Kafka Producer] Failed to send message key='{}'", key, ex);
                return "Failed to send message: " + ex.getMessage();
            });
        });
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  KAFKA CONSUMER (Dynamic Multi-threaded Consumer)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Single clean listener method configured with dynamic concurrency.
     *
     * <p>Spring Boot automatically spawns N consumer instances (threads) based on
     * {@code spring.kafka.listener.concurrency} in application.properties (default: 5).
     */
    @KafkaListener(
            topics = KafkaConfig.TOPIC_NAME,
            groupId = "${spring.kafka.consumer.group-id:learning-group}",
            concurrency = "${spring.kafka.listener.concurrency:5}"
    )
    public void consumeMessage(
            String message,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {
        log.info("[Kafka Consumer Thread: {}] Received message='{}' from partition={} offset={}",
                Thread.currentThread().getName(), message, partition, offset);
    }
}
