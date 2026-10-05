package com.example.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class OrderConsumer {
    private static final Logger log = LoggerFactory.getLogger(OrderConsumer.class);
    private final JsonMapper jsonMapper;
    private final OrderService service;
    public OrderConsumer(JsonMapper jsonMapper, OrderService service) {
        this.jsonMapper = jsonMapper;
        this.service = service;
    }
    @RabbitListener(queues = RabbitMQConfig.QUEUE)
    public void receive(String json) {
        OrderMessage message = jsonMapper.readValue(json, OrderMessage.class);
        log.info("Orden recibida: orderId={}", message.orderId());
        service.save(message);
    }
}
