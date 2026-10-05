package com.example.publisher;

import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "outbox.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxScheduler {
    private final OutboxProcessor processor;
    public OutboxScheduler(OutboxProcessor processor) { this.processor = processor; }
    @Scheduled(fixedDelayString = "${outbox.delay-ms:2000}")
    public void dispatch() {
        try { processor.publishPending(); }
        catch (RuntimeException failure) {
            // Do not log SQL parameters, exception messages or payloads.
            LoggerFactory.getLogger(getClass()).warn("Outbox ciclo fallido category={}", failure.getClass().getSimpleName());
        }
    }
}
