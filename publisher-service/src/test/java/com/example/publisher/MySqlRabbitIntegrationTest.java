package com.example.publisher;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.*;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "RUN_INFRA_TESTS", matches = "true")
class MySqlRabbitIntegrationTest {
    private static ConfigurableApplicationContext context;
    private static OrderService service;
    private static OutboxProcessor processor;
    private static OutboxRepository outbox;
    private static JdbcTemplate jdbc;
    private static RabbitAdmin admin;
    private static RabbitTemplate rabbit;
    @BeforeAll static void start() {
        String url = System.getenv("TEST_MYSQL_URL");
        if (url == null || !url.matches("jdbc:mysql://[^/]+/orders_test([?].*)?"))
            throw new IllegalStateException("Dedicated orders_test database required");
        if (!"integration".equals(System.getenv("RABBITMQ_VHOST"))) throw new IllegalStateException("Dedicated integration vhost required");
        context = new SpringApplicationBuilder(Application.class).run("--spring.datasource.url=" + url,
                "--spring.jpa.hibernate.ddl-auto=create-drop", "--demo.create-order=false", "--outbox.enabled=false");
        service = context.getBean(OrderService.class); processor = context.getBean(OutboxProcessor.class);
        outbox = context.getBean(OutboxRepository.class); jdbc = context.getBean(JdbcTemplate.class);
        admin = context.getBean(RabbitAdmin.class); rabbit = context.getBean(RabbitTemplate.class);
        admin.initialize();
    }
    @AfterAll static void close() { if (context != null) context.close(); }
    @BeforeEach void clear() {
        jdbc.update("DELETE FROM outbox_events"); jdbc.update("DELETE FROM orders");
        admin.purgeQueue(RabbitMQConfig.QUEUE); admin.purgeQueue(RabbitMQConfig.DLQ);
    }
    @Test void commitsBothRecordsAndPublishesConfirmedEvent() {
        var event = service.createDemoOrder();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM orders", Integer.class)).isEqualTo(1);
        assertThat(outbox.findAll()).hasSize(1);
        assertThat(outbox.findAll().get(0).getSentAt()).isNull();
        processor.publishPending();
        assertThat(outbox.findAll().get(0).getSentAt()).isNotNull();
        Message delivered = rabbit.receive(RabbitMQConfig.QUEUE, 5000);
        assertThat(delivered).isNotNull();
        assertThat(context.getBean(EventCodec.class).decode(delivered.getBody())).isEqualTo(event);
    }
    @Test void routingFailureStaysPendingAndRecoversWithoutLosingOrder() {
        Binding binding = context.getBean("ordersBinding", Binding.class);
        admin.removeBinding(binding);
        try {
            service.createDemoOrder(); processor.publishPending();
            assertThat(outbox.findAll().get(0).getSentAt()).isNull();
            assertThat(outbox.findAll().get(0).getAttempts()).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM orders", Integer.class)).isEqualTo(1);
        } finally { admin.declareBinding(binding); }
        processor.publishPending();
        assertThat(outbox.findAll().get(0).getSentAt()).isNotNull();
        assertThat(rabbit.receive(RabbitMQConfig.QUEUE, 5000)).isNotNull();
    }
    @Test void outboxInsertFailureRollsBackOrderInMySql() {
        jdbc.execute("ALTER TABLE outbox_events ADD CONSTRAINT test_reject_outbox CHECK (attempts > 0)");
        try {
            assertThatThrownBy(service::createDemoOrder).isInstanceOf(RuntimeException.class);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM orders", Integer.class)).isZero();
            assertThat(outbox.count()).isZero();
        } finally { jdbc.execute("ALTER TABLE outbox_events DROP CHECK test_reject_outbox"); }
    }
}
