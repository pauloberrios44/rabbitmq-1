package com.example.publisher;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    public static final String EXCHANGE = "orders.exchange";
    public static final String QUEUE = "orders.created.queue";
    public static final String ROUTING_KEY = "orders.created";

    @Bean DirectExchange ordersExchange() { return new DirectExchange(EXCHANGE); }
    @Bean Queue ordersQueue() { return new Queue(QUEUE, true); }
    @Bean Binding ordersBinding(Queue ordersQueue, DirectExchange ordersExchange) {
        return BindingBuilder.bind(ordersQueue).to(ordersExchange).with(ROUTING_KEY);
    }
}
