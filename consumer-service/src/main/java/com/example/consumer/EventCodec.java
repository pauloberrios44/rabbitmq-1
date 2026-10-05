package com.example.consumer;

import jakarta.validation.Validator;
import org.springframework.stereotype.Component;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.json.JsonMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class EventCodec {
    private final JsonMapper mapper = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();
    private final Validator validator;
    public EventCodec(Validator validator) { this.validator = validator; }
    public OrderCreatedEvent decode(byte[] body) {
        if (body.length > 16384) throw new InvalidEventException();
        final OrderCreatedEvent event;
        try { event = mapper.readValue(body, OrderCreatedEvent.class); }
        catch (RuntimeException failure) { throw new MalformedEventException(); }
        validate(event);
        return event;
    }
    public void validate(OrderCreatedEvent event) {
        if (event == null || !validator.validate(event).isEmpty()
                || event.occurredAt().isAfter(Instant.now().plus(5, ChronoUnit.MINUTES))) {
            throw new InvalidEventException();
        }
    }
    public String encode(OrderCreatedEvent event) {
        validate(event);
        return mapper.writeValueAsString(event);
    }
}
