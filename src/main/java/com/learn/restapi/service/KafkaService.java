package com.learn.restapi.service;

import com.learn.restapi.config.KafkaConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

/**
 * Service class demonstrating simple Kafka Producer and Consumer methods for learning purposes.
 *
 * <ul>
 *   <li><b>Producer:</b> Uses {@link KafkaTemplate} to send messages asynchronously to a topic.</li>
 *   <li><b>Consumer:</b> Uses {@link KafkaListener} to listen for incoming messages on the topic.</li>
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
    //  KAFKA CONSUMER
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Kafka Listener (Consumer) that automatically listens for incoming messages on the learning topic.
     *
     * @param message The received message content
     */
    @KafkaListener(topics = KafkaConfig.TOPIC_NAME, groupId = "${spring.kafka.consumer.group-id:learning-group}")
    public void consumeMessage(String message) {
        log.info("[Kafka Consumer] Received event payload: '{}'", message);
    }
}
