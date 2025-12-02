package org.jl.orderprocessing.Infrastructure.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();

        // 1. Connection Info
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        // --- CONCURRENCY & THROUGHPUT SETTINGS ---

        // 2. Batching (The "Bus" Strategy)
        // Instead of sending every message instantly (creating network overhead),
        // we wait up to 5ms OR until we have 16KB of data.
        // This dramatically reduces CPU usage and Network I/O.
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 5);
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384); // 16KB

        // 3. Resilience & Safety
        // "all": Wait for the Leader AND Replicas to acknowledge. Safest.
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");

        // 4. Timeouts (Fail Fast for the Outbox)
        // If Kafka is down, the default timeout is 60 seconds.
        // That is too long for a user-facing API! We lower it.
        // If the server doesn't ack in 2 seconds, throw exception -> Trigger Outbox Logic.
        configProps.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 2000);
        configProps.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 1000);

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    // --- AUTO-CREATE TOPIC FOR LAB ---
    // This creates the topic automatically when the app starts, so you don't need CLI commands.
    @Bean
    public NewTopic ordersTopic() {
        return TopicBuilder.name("orders-topic")
                .partitions(3) // Allows 3 consumers to read in parallel
                .replicas(1)   // For local dev, 1 replica is fine
                .build();
    }
}
