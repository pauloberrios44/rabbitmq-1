package com.example.publisher;

import org.junit.jupiter.api.*;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.*;
import jakarta.validation.Validator;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderPersistenceTest {
    private static ConfigurableApplicationContext context;
    @TestConfiguration(proxyBeanMethods = false)
    static class TestBeans {
        @Bean @Primary EventCodec testCodec(Validator validator) { return spy(new EventCodec(validator)); }
    }
    @BeforeAll static void start() {
        context = new SpringApplicationBuilder(Application.class, TestBeans.class).run(
                "--spring.datasource.url=jdbc:h2:mem:publisher;DB_CLOSE_DELAY=-1",
                "--spring.datasource.username=sa", "--spring.datasource.password=test",
                "--spring.datasource.driver-class-name=org.h2.Driver", "--spring.jpa.hibernate.ddl-auto=create-drop",
                "--demo.create-order=false", "--outbox.enabled=false",
                "--spring.rabbitmq.host=127.0.0.1", "--spring.rabbitmq.port=35672",
                "--spring.rabbitmq.username=test", "--spring.rabbitmq.password=test",
                "--spring.rabbitmq.virtual-host=/test", "--spring.rabbitmq.ssl.enabled=false",
                "--spring.rabbitmq.dynamic=false");
    }
    @AfterAll static void stop() { if (context != null) context.close(); }
    @BeforeEach void resetData() {
        reset(context.getBean(EventCodec.class));
        context.getBean(OutboxRepository.class).deleteAll();
        context.getBean(OrderRepository.class).deleteAll();
    }
    @Test void commitsOrderAndOutboxTogether() {
        var event = context.getBean(OrderService.class).createDemoOrder();
        assertThat(context.getBean(OrderRepository.class).findById(event.orderId())).isPresent();
        assertThat(context.getBean(OutboxRepository.class).count()).isEqualTo(1);
    }
    @Test void failureAfterOrderSaveRollsBackBothWrites() {
        doThrow(new IllegalStateException("Cannot serialize")).when(context.getBean(EventCodec.class)).encode(any());
        assertThatThrownBy(() -> context.getBean(OrderService.class).createDemoOrder()).isInstanceOf(IllegalStateException.class);
        assertThat(context.getBean(OrderRepository.class).count()).isZero();
        assertThat(context.getBean(OutboxRepository.class).count()).isZero();
    }
}
