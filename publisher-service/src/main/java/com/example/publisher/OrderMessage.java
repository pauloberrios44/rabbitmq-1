package com.example.publisher;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderMessage(Long orderId, Long customerId, String customerName,
        String customerEmail, BigDecimal amount, String status, Instant createdAt) {}
