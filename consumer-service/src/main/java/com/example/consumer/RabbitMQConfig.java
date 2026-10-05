package com.example.consumer;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    public static final String EXCHANGE = "orders.exchange";
    public static final String QUEUE = "orders.created.queue";
    public static final String ROUTING_KEY = "orders.created";
    public static final String DLX = "orders.dlx";
    public static final String DLQ = "orders.created.dlq";

    @Bean DirectExchange ordersExchange() { return new DirectExchange(EXCHANGE); }
    @Bean DirectExchange deadLetterExchange() { return new DirectExchange(DLX); }
    @Bean Queue ordersQueue() {
        return QueueBuilder.durable(QUEUE).deadLetterExchange(DLX)
                .deadLetterRoutingKey(ROUTING_KEY).build();
    }
    @Bean Queue deadLetterQueue() { return QueueBuilder.durable(DLQ).build(); }
    @Bean Binding ordersBinding() {
        return BindingBuilder.bind(ordersQueue()).to(ordersExchange()).with(ROUTING_KEY);
    }
    @Bean Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with(ROUTING_KEY);
    }
}
