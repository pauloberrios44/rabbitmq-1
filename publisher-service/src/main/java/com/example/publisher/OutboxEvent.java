package com.example.publisher;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events", indexes = @Index(name = "idx_outbox_pending", columnList = "sentAt,id"))
public class OutboxEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 36)
    private String eventId;
    @Column(nullable = false, length = 36)
    private String correlationId;
    @Lob @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;
    @Column(nullable = false)
    private Instant createdAt;
    private Instant sentAt;
    private int attempts;

    protected OutboxEvent() {}
    public OutboxEvent(UUID eventId, UUID correlationId, String payload) {
        this.eventId = eventId.toString();
        this.correlationId = correlationId.toString();
        this.payload = payload;
        this.createdAt = Instant.now();
    }
    public void attempted() { attempts++; }
    public void markSent() { sentAt = Instant.now(); }
    public Long getId() { return id; }
    public String getEventId() { return eventId; }
    public String getCorrelationId() { return correlationId; }
    public String getPayload() { return payload; }
    public Instant getSentAt() { return sentAt; }
    public int getAttempts() { return attempts; }
}
