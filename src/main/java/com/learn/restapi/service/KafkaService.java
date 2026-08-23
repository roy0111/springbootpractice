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
 * Service class demonstrating Kafka Producer and multiple Consumer methods for learning.
 *
 * <ul>
 *   <li><b>Producer:</b> Sends messages asynchronously via {@link KafkaTemplate}.</li>
 *   <li><b>Consumers (5 Instances):</b> Demonstrates concurrent message consumption using
 *       either concurrency settings or distinct consumer methods/groups.</li>
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
    //  KAFKA CONSUMERS (5 Consumer Instances in Consumer Group)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Approach 1: Concurrent Consumer (Spawns 5 consumer thread instances inside same listener).
     *
     * <p>Setting {@code concurrency = "5"} creates 5 active consumer threads in the container
     * sharing partitions of {@code learning-events}.
     */
    @KafkaListener(
            topics = KafkaConfig.TOPIC_NAME,
            groupId = "${spring.kafka.consumer.group-id:learning-group}",
            concurrency = "5"
    )
    public void consumeMessage(
            String message,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {
        log.info("[Kafka Concurrent Consumer Thread: {}] Received message='{}' from partition={} offset={}",
                Thread.currentThread().getName(), message, partition, offset);
    }

    // ── Approach 2: 5 Explicit Consumer Methods (for distinct learning listeners) ──────

    @KafkaListener(topics = KafkaConfig.TOPIC_NAME, groupId = "explicit-group-1")
    public void consumerOne(String message) {
        log.info("[Explicit Consumer 1] Processed message: '{}'", message);
    }

    @KafkaListener(topics = KafkaConfig.TOPIC_NAME, groupId = "explicit-group-2")
    public void consumerTwo(String message) {
        log.info("[Explicit Consumer 2] Processed message: '{}'", message);
    }

    @KafkaListener(topics = KafkaConfig.TOPIC_NAME, groupId = "explicit-group-3")
    public void consumerThree(String message) {
        log.info("[Explicit Consumer 3] Processed message: '{}'", message);
    }

    @KafkaListener(topics = KafkaConfig.TOPIC_NAME, groupId = "explicit-group-4")
    public void consumerFour(String message) {
        log.info("[Explicit Consumer 4] Processed message: '{}'", message);
    }

    @KafkaListener(topics = KafkaConfig.TOPIC_NAME, groupId = "explicit-group-5")
    public void consumerFive(String message) {
        log.info("[Explicit Consumer 5] Processed message: '{}'", message);
    }
}
