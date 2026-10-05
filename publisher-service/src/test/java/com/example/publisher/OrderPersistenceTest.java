package com.example.publisher;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;

class OrderPersistenceTest {
    private static ConfigurableApplicationContext context;
    @BeforeAll static void start() {
        context = new SpringApplicationBuilder(Application.class).run(
    "--spring.datasource.url=jdbc:h2:mem:publisher;DB_CLOSE_DELAY=-1",
    "--spring.datasource.username=sa", "--spring.datasource.password=",
    "--spring.datasource.driver-class-name=org.h2.Driver",
    "--spring.jpa.hibernate.ddl-auto=create-drop", "--demo.create-order=false",
    "--spring.rabbitmq.host=127.0.0.1", "--spring.rabbitmq.username=test",
    "--spring.rabbitmq.password=test", "--spring.rabbitmq.listener.simple.auto-startup=false",
    "--spring.rabbitmq.dynamic=false"
        );
    }
    @AfterAll static void stop() { if (context != null) context.close(); }
    @Test void persistsAndReadsOrder() {
        OrderRepository repository = context.getBean(OrderRepository.class);
        Order order = repository.saveAndFlush(new Order(1001L, "Demo",
                "demo@example.com", new BigDecimal("25000.00")));
        Order stored = repository.findById(order.getId()).orElseThrow();
        assertThat(stored.getCustomerId()).isEqualTo(1001L);
        assertThat(stored.getAmount()).isEqualByComparingTo("25000");
        assertThat(stored.getStatus()).isEqualTo("CREATED");
    }
}
