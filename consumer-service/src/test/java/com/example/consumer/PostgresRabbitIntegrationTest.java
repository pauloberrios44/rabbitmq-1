package com.example.consumer;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.*;
import org.springframework.jdbc.core.JdbcTemplate;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;
import java.time.Duration;

@EnabledIfEnvironmentVariable(named = "RUN_INFRA_TESTS", matches = "true")
class PostgresRabbitIntegrationTest {
    private static ConfigurableApplicationContext context;
    private static OrderService service;
    private static JdbcTemplate jdbc;
    private static RabbitTemplate rabbit;
    private static EventCodec codec;
    @BeforeAll static void start() {
        String url = System.getenv("TEST_POSTGRES_URL");
        if (url == null || !url.matches("jdbc:postgresql://[^/]+/orders_test([?].*)?"))
            throw new IllegalStateException("Dedicated orders_test database required");
        if (!"integration".equals(System.getenv("RABBITMQ_VHOST"))) throw new IllegalStateException("Dedicated integration vhost required");
        context = new SpringApplicationBuilder(Application.class).run(
                "--spring.datasource.url=" + url, "--spring.jpa.hibernate.ddl-auto=create-drop",
                "--demo.retry-invalid=true", "--consumer.retry-delay-ms=10");
        service = context.getBean(OrderService.class);
        jdbc = context.getBean(JdbcTemplate.class);
        rabbit = context.getBean(RabbitTemplate.class);
        codec = context.getBean(EventCodec.class);
    }
    @AfterAll static void close() { if (context != null) context.close(); }
    @BeforeEach void clear() {
        context.getBean(RabbitAdmin.class).purgeQueue(RabbitMQConfig.QUEUE);
        context.getBean(RabbitAdmin.class).purgeQueue(RabbitMQConfig.DLQ);
        jdbc.update("DELETE FROM orders");
        jdbc.update("DELETE FROM processed_events");
    }
    private OrderCreatedEvent event() {
        return new OrderCreatedEvent(UUID.randomUUID(), 100L, UUID.randomUUID(), Instant.now(),
                "OrderCreated", new OrderCreatedEvent.Payload(1001L, new BigDecimal("25000.00")));
    }
    @Test void concurrentDuplicatesProduceExactlyOneBusinessRow() throws Exception {
        OrderCreatedEvent event = event();
        ExecutorService pool = Executors.newFixedThreadPool(6);
        try {
            List<Callable<Boolean>> calls = new ArrayList<>();
            for (int i = 0; i < 12; i++) calls.add(() -> service.process(event));
            int inserted = 0;
            for (Future<Boolean> result : pool.invokeAll(calls)) if (result.get()) inserted++;
            assertThat(inserted).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM orders", Integer.class)).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM processed_events", Integer.class)).isEqualTo(1);
        } finally { pool.shutdownNow(); }
    }
    @Test void failedBusinessWriteRollsBackIdempotencyMarker() {
        var event = event();
        jdbc.execute("ALTER TABLE orders ADD CONSTRAINT test_amount CHECK (amount < 100)");
        try {
            assertThatThrownBy(() -> service.process(event)).isInstanceOf(RuntimeException.class);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM processed_events", Integer.class)).isZero();
            assertThat(jdbc.queryForObject("SELECT count(*) FROM orders", Integer.class)).isZero();
        } finally { jdbc.execute("ALTER TABLE orders DROP CONSTRAINT test_amount"); }
        assertThat(service.process(event)).isTrue();
    }
    @Test void realBrokerDeliversAndDuplicateIsIgnored() {
        String json = codec.encode(event());
        rabbit.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY, json);
        rabbit.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY, json);
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            assertThat(jdbc.queryForObject("SELECT count(*) FROM orders", Integer.class)).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM processed_events", Integer.class)).isEqualTo(1);
        });
        await().during(Duration.ofSeconds(1)).atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(jdbc.queryForObject("SELECT count(*) FROM orders", Integer.class)).isEqualTo(1));
    }
    @Test void invalidEventIsDeadLetteredByRealRabbitMq() {
        String invalid = codec.encode(event()).replace("\"amount\":25000.00", "\"amount\":-1");
        assertThatThrownBy(() -> codec.decode(invalid.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                .isInstanceOf(InvalidEventException.class);
        rabbit.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY, invalid);
        Message dead = rabbit.receive(RabbitMQConfig.DLQ, 15000);
        assertThat(dead).isNotNull();
        assertThat(dead.getMessageProperties().getXDeathHeader()).isNotEmpty();
        assertThat(dead.getMessageProperties().getXDeathHeader().get(0).get("reason")).isEqualTo("rejected");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM orders", Integer.class)).isZero();
    }
    @Test void malformedJsonIsDeadLettered() {
        rabbit.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY, "{bad-json");
        assertThat(rabbit.receive(RabbitMQConfig.DLQ, 15000)).isNotNull();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM processed_events", Integer.class)).isZero();
    }
}
