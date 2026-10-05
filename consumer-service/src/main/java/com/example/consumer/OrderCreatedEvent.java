package com.example.consumer;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderCreatedEvent(
        @NotNull UUID eventId,
        @NotNull @Positive Long orderId,
        @NotNull UUID correlationId,
        @NotNull Instant occurredAt,
        @NotNull @Pattern(regexp = "OrderCreated") String eventType,
        @NotNull @Valid Payload payload) {
    // Consumer only needs customerId and amount; names and emails stay in MySQL.
    public record Payload(@NotNull @Positive Long customerId,
            @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount) {}
}
