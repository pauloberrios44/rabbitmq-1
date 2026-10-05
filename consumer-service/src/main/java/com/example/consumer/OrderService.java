package com.example.consumer;

import org.springframework.stereotype.Service;

@Service
public class OrderService {
    private final OrderRepository repository;
    public OrderService(OrderRepository repository) { this.repository = repository; }
    public Order save(OrderMessage message) { return repository.save(Order.from(message)); }
}
