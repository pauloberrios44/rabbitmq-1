package com.example.publisher;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderPublisherTest {
    @Test void waitsForConfirmAndUsesPersistentJson() throws Exception {
        RabbitTemplate template = mock(RabbitTemplate.class);
        doAnswer(call -> {
            Message message = call.getArgument(2);
            assertThat(message.getMessageProperties().getDeliveryMode()).isEqualTo(MessageDeliveryMode.PERSISTENT);
            assertThat(message.getMessageProperties().getContentType()).isEqualTo("application/json");
            CorrelationData correlation = call.getArgument(3);
            correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(template).send(eq("orders.exchange"), eq("orders.created"), any(Message.class), any(CorrelationData.class));
        new OrderPublisher(template).publish(event());
    }
    @Test void nackLeavesPublicationFailed() {
        RabbitTemplate template = mock(RabbitTemplate.class);
        doAnswer(call -> {
            CorrelationData c = call.getArgument(3);
            c.getFuture().complete(new CorrelationData.Confirm(false, "nack"));
            return null;
        }).when(template).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));
        assertThatThrownBy(() -> new OrderPublisher(template).publish(event())).isInstanceOf(IllegalStateException.class);
    }
    @Test void ackWithMandatoryReturnIsNotSuccess() {
        RabbitTemplate template = mock(RabbitTemplate.class);
        doAnswer(call -> {
            CorrelationData c = call.getArgument(3);
            c.setReturned(new ReturnedMessage(call.getArgument(2), 312, "NO_ROUTE", "orders.exchange", "orders.created"));
            c.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(template).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));
        assertThatThrownBy(() -> new OrderPublisher(template).publish(event())).isInstanceOf(IllegalStateException.class);
    }
    @Test void connectionFailurePropagates() {
        RabbitTemplate template = mock(RabbitTemplate.class);
        doThrow(new org.springframework.amqp.AmqpException("offline")).when(template)
                .send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));
        assertThatThrownBy(() -> new OrderPublisher(template).publish(event()))
                .isInstanceOf(org.springframework.amqp.AmqpException.class);
    }
    @Test void unconfirmedMessageTimesOut() {
        RabbitTemplate template = mock(RabbitTemplate.class);
        assertThatThrownBy(() -> new OrderPublisher(template).publish(event()))
                .isInstanceOf(java.util.concurrent.TimeoutException.class);
    }
    private OutboxEvent event() { return new OutboxEvent(UUID.randomUUID(), UUID.randomUUID(), "{}"); }
}
