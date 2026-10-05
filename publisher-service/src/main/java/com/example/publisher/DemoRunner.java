package com.example.publisher;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;

@Component
@ConditionalOnProperty(name = "demo.create-order", havingValue = "true", matchIfMissing = true)
public class DemoRunner implements CommandLineRunner {
    private final OrderService service;
    private final OutboxRepository outbox;
    private final EventCodec codec;
    private final String scenario;
    public DemoRunner(OrderService service, OutboxRepository outbox, EventCodec codec,
            @Value("${demo.scenario:normal}") String scenario) {
        this.service = service; this.outbox = outbox; this.codec = codec; this.scenario = scenario;
    }
    @Override public void run(String... args) {
        if (!java.util.Set.of("normal", "duplicate", "invalid", "malformed").contains(scenario))
            throw new IllegalArgumentException("Unknown demo scenario");
        OrderCreatedEvent event = service.createDemoOrder();
        // Explicit demo fixtures only: production creation always validates through OrderService.
        if (!scenario.equals("normal")) {
            String json = codec.encode(event);
            if (scenario.equals("invalid")) json = json.replace("\"amount\":25000.00", "\"amount\":-1");
            if (scenario.equals("malformed")) json = "{invalid-json";
            if (scenario.equals("invalid")) {
                try { codec.decode(json.getBytes(StandardCharsets.UTF_8)); throw new IllegalStateException("Fixture is valid"); }
                catch (InvalidEventException expected) { /* Deliberately invalid. */ }
            }
            outbox.save(new OutboxEvent(event.eventId(), event.correlationId(), json));
        }
        LoggerFactory.getLogger(getClass()).info("Orden y Outbox guardados eventId={} correlationId={} scenario={}",
                event.eventId(), event.correlationId(), scenario);
    }
}
