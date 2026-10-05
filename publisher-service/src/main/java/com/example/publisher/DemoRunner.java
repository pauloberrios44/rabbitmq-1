package com.example.publisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "demo.create-order", havingValue = "true", matchIfMissing = true)
public class DemoRunner implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DemoRunner.class);
    private final OrderService service;
    public DemoRunner(OrderService service) { this.service = service; }
    @Override public void run(String... args) {
        Order order = service.createDemoOrder();
        log.info("Orden creada y enviada: orderId={}", order.getId());
    }
}
