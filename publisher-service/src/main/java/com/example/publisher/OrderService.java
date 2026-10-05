package com.example.publisher;

import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
public class OrderService {
    private final OrderRepository repository;
    private final OrderPublisher publisher;
    public OrderService(OrderRepository repository, OrderPublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
    }
    // save() commits its own transaction before publishing: intentionally basic.
    public Order createDemoOrder() {
        Order order = repository.save(new Order(1001L, "Usuario Demo",
                "usuario.demo@example.com", new BigDecimal("25000.00")));
        publisher.publish(order);
        return order;
    }
}
