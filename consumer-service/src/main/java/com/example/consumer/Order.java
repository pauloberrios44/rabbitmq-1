package com.example.consumer;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "orders")
public class Order {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long sourceOrderId;
    @Column(nullable = false)
    private Long customerId;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    @Column(nullable = false)
    private Instant createdAt;
    protected Order() {}
    public static Order from(OrderCreatedEvent event) {
        Order order = new Order();
        order.sourceOrderId = event.orderId();
        order.customerId = event.payload().customerId();
        order.amount = event.payload().amount();
        order.createdAt = event.occurredAt();
        return order;
    }
    public Long getId() { return id; }
    public Long getSourceOrderId() { return sourceOrderId; }
    public Long getCustomerId() { return customerId; }
    public BigDecimal getAmount() { return amount; }
    public Instant getCreatedAt() { return createdAt; }
}
