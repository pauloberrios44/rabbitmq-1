package com.example.consumer;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "orders")
public class Order {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long sourceOrderId;
    private Long customerId;
    private String customerName;
    private String customerEmail;
    @Column(precision = 19, scale = 2)
    private BigDecimal amount;
    private String status;
    private Instant createdAt;

    protected Order() {}

    public Order(Long customerId, String customerName, String customerEmail, BigDecimal amount) {
        this.customerId = customerId;
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.amount = amount;
        this.status = "CREATED";
        this.createdAt = Instant.now();
    }

    public static Order from(OrderMessage message) {
        Order order = new Order(message.customerId(), message.customerName(), message.customerEmail(), message.amount());
        order.sourceOrderId = message.orderId();
        order.status = message.status();
        order.createdAt = message.createdAt();
        return order;
    }
    public Long getSourceOrderId() { return sourceOrderId; }
    public Long getId() { return id; }
    public Long getCustomerId() { return customerId; }
    public String getCustomerName() { return customerName; }
    public String getCustomerEmail() { return customerEmail; }
    public BigDecimal getAmount() { return amount; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
