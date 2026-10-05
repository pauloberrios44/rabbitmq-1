package com.example.publisher;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class OrderPublisher {
    private final RabbitTemplate template;
    public OrderPublisher(RabbitTemplate template) { this.template = template; }

    public void publish(OutboxEvent event) throws Exception {
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        properties.setContentEncoding("UTF-8");
        properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        properties.setMessageId(event.getEventId());
        properties.setCorrelationId(event.getCorrelationId());
        // A fresh confirm identifier is needed for every attempt, even for the same eventId.
        CorrelationData confirmation = new CorrelationData(UUID.randomUUID().toString());
        Message message = new Message(event.getPayload().getBytes(StandardCharsets.UTF_8), properties);
        template.send(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY, message, confirmation);
        CorrelationData.Confirm result = confirmation.getFuture().get(5, TimeUnit.SECONDS);
        if (!result.ack() || confirmation.getReturned() != null) {
            throw new IllegalStateException("Broker did not accept routing");
        }
    }
}
