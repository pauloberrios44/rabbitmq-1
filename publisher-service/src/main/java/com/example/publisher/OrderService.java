package com.example.publisher;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class OrderService {
    private final OrderRepository orders;
    private final OutboxRepository outbox;
    private final EventCodec codec;
    public OrderService(OrderRepository orders, OutboxRepository outbox, EventCodec codec) {
        this.orders = orders;
        this.outbox = outbox;
        this.codec = codec;
    }
    @Transactional
    public OrderCreatedEvent createDemoOrder() {
        Order order = orders.save(new Order(1001L, "Usuario Demo",
                "usuario.demo@example.com", new BigDecimal("25000.00")));
        OrderCreatedEvent event = new OrderCreatedEvent(UUID.randomUUID(), order.getId(),
                UUID.randomUUID(), Instant.now(), "OrderCreated",
                new OrderCreatedEvent.Payload(order.getCustomerId(), order.getAmount()));
        outbox.save(new OutboxEvent(event.eventId(), event.correlationId(), codec.encode(event)));
        return event;
    }
}
