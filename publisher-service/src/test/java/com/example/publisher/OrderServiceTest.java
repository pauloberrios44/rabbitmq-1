package com.example.publisher;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderServiceTest {
    private final OrderRepository repository = mock(OrderRepository.class);
    private final RabbitTemplate rabbit = mock(RabbitTemplate.class);
    private final JsonMapper mapper = JsonMapper.builder().build();
    private final OrderService service = new OrderService(repository, new OrderPublisher(rabbit, mapper));
    @Test void commitsOrderBeforePublishingJson() {
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));
        service.createDemoOrder();
        var sequence = inOrder(repository, rabbit);
        sequence.verify(repository).save(any());
        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        sequence.verify(rabbit).convertAndSend(eq("orders.exchange"), eq("orders.created"), json.capture());
        OrderMessage message = mapper.readValue(json.getValue(), OrderMessage.class);
        assertThat(message.customerId()).isEqualTo(1001L);
        assertThat(message.amount()).isEqualByComparingTo("25000");
        assertThat(message.createdAt()).isNotNull();
    }
    @Test void databaseFailureDoesNotPublish() {
        when(repository.save(any())).thenThrow(new IllegalStateException("Database unavailable"));
        assertThatThrownBy(service::createDemoOrder).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(rabbit);
    }
    @Test void brokerFailureIsVisibleAfterSave() {
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));
        doThrow(new org.springframework.amqp.AmqpException("Broker unavailable"))
                .when(rabbit).convertAndSend(anyString(), anyString(), anyString());
        assertThatThrownBy(service::createDemoOrder).isInstanceOf(org.springframework.amqp.AmqpException.class);
        verify(repository).save(any());
    }
}
