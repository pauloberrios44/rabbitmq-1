package com.example.consumer;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "processed_events")
public class ProcessedEvent {
    @Id @Column(length = 36)
    private String eventId;
    @Column(nullable = false)
    private Instant processedAt;
    protected ProcessedEvent() {}
}
