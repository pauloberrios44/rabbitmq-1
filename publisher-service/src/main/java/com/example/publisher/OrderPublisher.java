package com.example.publisher;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class OrderPublisher {
    private final RabbitTemplate rabbitTemplate;
    private final JsonMapper jsonMapper;

    public OrderPublisher(RabbitTemplate rabbitTemplate, JsonMapper jsonMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.jsonMapper = jsonMapper;
    }
    public void publish(Order order) {
        OrderMessage message = new OrderMessage(order.getId(), order.getCustomerId(),
                order.getCustomerName(), order.getCustomerEmail(), order.getAmount(),
                order.getStatus(), order.getCreatedAt());
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY,
                jsonMapper.writeValueAsString(message));
    }
}
