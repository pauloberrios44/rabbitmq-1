package com.example.publisher;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderServiceTest {
    @Test void createsMinimalEventAndOutbox() {
        OrderRepository orders = mock(OrderRepository.class);
        OutboxRepository outbox = mock(OutboxRepository.class);
        Order stored = mock(Order.class);
        when(stored.getId()).thenReturn(42L);
        when(stored.getCustomerId()).thenReturn(1001L);
        when(stored.getAmount()).thenReturn(new BigDecimal("25000.00"));
        when(orders.save(any())).thenReturn(stored);
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            EventCodec codec = new EventCodec(factory.getValidator());
            OrderCreatedEvent event = new OrderService(orders, outbox, codec).createDemoOrder();
            assertThat(event.eventId()).isNotNull();
            assertThat(event.correlationId()).isNotNull();
            assertThat(event.orderId()).isEqualTo(42L);
            ArgumentCaptor<OutboxEvent> capture = ArgumentCaptor.forClass(OutboxEvent.class);
            verify(outbox).save(capture.capture());
            assertThat(capture.getValue().getPayload()).doesNotContain("customerName", "customerEmail", "Usuario Demo");
            assertThat(codec.decode(capture.getValue().getPayload().getBytes(java.nio.charset.StandardCharsets.UTF_8))).isEqualTo(event);
        }
    }
    @Test void databaseFailureDoesNotCreateOutbox() {
        OrderRepository orders = mock(OrderRepository.class);
        OutboxRepository outbox = mock(OutboxRepository.class);
        when(orders.save(any())).thenThrow(new IllegalStateException());
        assertThatThrownBy(() -> new OrderService(orders, outbox, mock(EventCodec.class)).createDemoOrder())
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(outbox);
    }
}
