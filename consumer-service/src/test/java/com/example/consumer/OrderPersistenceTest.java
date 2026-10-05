package com.example.consumer;

import org.junit.jupiter.api.*;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class OrderPersistenceTest {
    private static ConfigurableApplicationContext context;
    @BeforeAll static void start() {
        context = new SpringApplicationBuilder(Application.class).run(
                "--spring.datasource.url=jdbc:h2:mem:consumer;DB_CLOSE_DELAY=-1",
                "--spring.datasource.username=sa", "--spring.datasource.password=test",
                "--spring.datasource.driver-class-name=org.h2.Driver", "--spring.jpa.hibernate.ddl-auto=create-drop",
                "--spring.rabbitmq.host=127.0.0.1", "--spring.rabbitmq.port=35672",
                "--spring.rabbitmq.username=test", "--spring.rabbitmq.password=test",
                "--spring.rabbitmq.virtual-host=/test", "--spring.rabbitmq.ssl.enabled=false",
                "--spring.rabbitmq.listener.simple.auto-startup=false", "--spring.rabbitmq.dynamic=false");
    }
    @AfterAll static void stop() { if (context != null) context.close(); }
    @Test void persistsMinimalBusinessData() {
        OrderRepository repository = context.getBean(OrderRepository.class);
        OrderCreatedEvent event = new OrderCreatedEvent(UUID.randomUUID(), 42L, UUID.randomUUID(), Instant.now(),
                "OrderCreated", new OrderCreatedEvent.Payload(1001L, new BigDecimal("25000")));
        Order order = repository.saveAndFlush(Order.from(event));
        assertThat(repository.findById(order.getId()).orElseThrow().getSourceOrderId()).isEqualTo(42L);
    }
}
