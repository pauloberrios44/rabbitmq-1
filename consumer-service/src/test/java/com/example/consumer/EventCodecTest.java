package com.example.consumer;
import jakarta.validation.Validation;
import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import static org.assertj.core.api.Assertions.*;

class EventCodecTest {
    private final jakarta.validation.ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    private final EventCodec codec = new EventCodec(factory.getValidator());
    @AfterEach void close() { factory.close(); }
    private OrderCreatedEvent event(BigDecimal amount, String type) {
        return new OrderCreatedEvent(UUID.randomUUID(), 1L, UUID.randomUUID(), Instant.now(), type,
                new OrderCreatedEvent.Payload(1001L, amount));
    }
    @Test void validEventRoundTrips() {
        var event = event(BigDecimal.TEN, "OrderCreated");
        assertThat(codec.decode(codec.encode(event).getBytes(StandardCharsets.UTF_8))).isEqualTo(event);
    }
    @Test void negativeAmountFails() {
        assertThatThrownBy(() -> codec.encode(event(BigDecimal.ONE.negate(), "OrderCreated"))).isInstanceOf(InvalidEventException.class);
    }
    @Test void wrongTypeFails() {
        assertThatThrownBy(() -> codec.encode(event(BigDecimal.TEN, "DeleteOrder"))).isInstanceOf(InvalidEventException.class);
    }
    @Test void malformedAndNullFail() {
        assertThatThrownBy(() -> codec.decode("{oops".getBytes(StandardCharsets.UTF_8))).isInstanceOf(MalformedEventException.class);
        assertThatThrownBy(() -> codec.decode("null".getBytes(StandardCharsets.UTF_8))).isInstanceOf(InvalidEventException.class);
    }
    @Test void oversizeMessageFails() {
        assertThatThrownBy(() -> codec.decode(new byte[16385])).isInstanceOf(InvalidEventException.class);
    }
    @Test void missingFieldsFail() {
        assertThatThrownBy(() -> codec.decode("{}".getBytes(StandardCharsets.UTF_8))).isInstanceOf(InvalidEventException.class);
    }
    @Test void unknownFieldsFail() {
        String json = codec.encode(event(BigDecimal.TEN, "OrderCreated"));
        json = json.substring(0, json.length() - 1) + ",\"unexpected\":true}";
        String invalid = json;
        assertThatThrownBy(() -> codec.decode(invalid.getBytes(StandardCharsets.UTF_8))).isInstanceOf(MalformedEventException.class);
    }
    @Test void trailingJsonFails() {
        String json = codec.encode(event(BigDecimal.TEN, "OrderCreated")) + "{}";
        assertThatThrownBy(() -> codec.decode(json.getBytes(StandardCharsets.UTF_8))).isInstanceOf(MalformedEventException.class);
    }
    @Test void duplicateKeysFail() {
        String json = codec.encode(event(BigDecimal.TEN, "OrderCreated"));
        String duplicate = json.substring(0, json.length() - 1) + ",\"eventType\":\"OrderCreated\"}";
        assertThatThrownBy(() -> codec.decode(duplicate.getBytes(StandardCharsets.UTF_8))).isInstanceOf(MalformedEventException.class);
    }
}
