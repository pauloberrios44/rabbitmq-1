package com.example.consumer;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderServiceTest {
    private final OrderRepository orders = mock(OrderRepository.class);
    private final ProcessedEventRepository processed = mock(ProcessedEventRepository.class);
    private final OrderService service = new OrderService(orders, processed);
    private final OrderCreatedEvent event = new OrderCreatedEvent(UUID.randomUUID(), 1L, UUID.randomUUID(),
            Instant.now(), "OrderCreated", new OrderCreatedEvent.Payload(1001L, BigDecimal.TEN));
    @Test void duplicateDoesNotCreateBusinessRecord() {
        when(processed.claim(anyString())).thenReturn(0);
        assertThat(service.process(event)).isFalse(); verifyNoInteractions(orders);
    }
    @Test void newEventClaimsAndPersists() {
        when(processed.claim(anyString())).thenReturn(1);
        assertThat(service.process(event)).isTrue(); verify(orders).saveAndFlush(any(Order.class));
    }
}
