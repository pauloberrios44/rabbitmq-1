package com.example.consumer;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;
import java.math.BigDecimal;
import java.time.Instant;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderConsumerTest {
    private final OrderRepository repository = mock(OrderRepository.class);
    private final JsonMapper mapper = JsonMapper.builder().build();
    private final OrderConsumer consumer = new OrderConsumer(mapper, new OrderService(repository));
    @Test void convertsReceivedJsonAndPersists() {
        OrderMessage message = new OrderMessage(42L, 1001L, "Demo", "demo@example.com",
                new BigDecimal("25000"), "CREATED", Instant.now());
        consumer.receive(mapper.writeValueAsString(message));
        ArgumentCaptor<Order> order = ArgumentCaptor.forClass(Order.class);
        verify(repository).save(order.capture());
        assertThat(order.getValue().getSourceOrderId()).isEqualTo(42L);
        assertThat(order.getValue().getAmount()).isEqualByComparingTo("25000");
    }
    @Test void malformedJsonDoesNotPersist() {
        assertThatThrownBy(() -> consumer.receive("{broken")).isInstanceOf(RuntimeException.class);
        verifyNoInteractions(repository);
    }
    @Test void persistenceFailurePropagatesToListener() {
        when(repository.save(any())).thenThrow(new IllegalStateException("Database unavailable"));
        OrderMessage message = new OrderMessage(42L, 1001L, "Demo", "demo@example.com",
                BigDecimal.TEN, "CREATED", Instant.now());
        assertThatThrownBy(() -> consumer.receive(mapper.writeValueAsString(message)))
                .isInstanceOf(IllegalStateException.class);
    }
}
