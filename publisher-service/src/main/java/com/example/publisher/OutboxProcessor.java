package com.example.publisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxProcessor {
    private static final Logger log = LoggerFactory.getLogger(OutboxProcessor.class);
    private final OutboxRepository repository;
    private final OrderPublisher publisher;
    public OutboxProcessor(OutboxRepository repository, OrderPublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
    }
    // Small batch, one worker recommended for the demo; DB lock serializes other workers.
    @Transactional
    public void publishPending() {
        for (OutboxEvent event : repository.findPending(PageRequest.of(0, 10))) {
            event.attempted();
            try {
                publisher.publish(event);
                event.markSent();
                log.info("Outbox confirmado eventId={} correlationId={}", event.getEventId(), event.getCorrelationId());
            } catch (Exception failure) {
                if (failure instanceof InterruptedException) Thread.currentThread().interrupt();
                log.warn("Outbox pendiente eventId={} attempt={} category={}",
                        event.getEventId(), event.getAttempts(), failure.getClass().getSimpleName());
                break;
            }
        }
    }
}
