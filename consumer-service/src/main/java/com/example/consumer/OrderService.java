package com.example.consumer;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
    private final OrderRepository orders;
    private final ProcessedEventRepository processed;
    public OrderService(OrderRepository orders, ProcessedEventRepository processed) {
        this.orders = orders; this.processed = processed;
    }
    @Transactional
    public boolean process(OrderCreatedEvent event) {
        if (processed.claim(event.eventId().toString()) == 0) return false;
        orders.saveAndFlush(Order.from(event));
        // Both writes commit together; an exception rolls back the idempotency marker too.
        return true;
    }
}
