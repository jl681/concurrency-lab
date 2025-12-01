package org.jl.orderprocessing;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
@Getter
@NoArgsConstructor
public class OutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String topic;
    private String key;
    private String payload;
    private LocalDateTime createdAt;

    public OutboxEvent(String topic, String key, String payload) {
        this.topic = topic;
        this.key = key;
        this.payload = payload;
        this.createdAt = LocalDateTime.now();
    }

}
