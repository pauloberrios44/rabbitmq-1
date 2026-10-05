package com.example.consumer;

import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.*;
import org.springframework.dao.TransientDataAccessResourceException;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderConsumerTest {
    private final EventCodec codec = mock(EventCodec.class);
    private final OrderService service = mock(OrderService.class);
    private final Channel channel = mock(Channel.class);
    private final OrderConsumer consumer = new OrderConsumer(codec, service, false, 0);
    private final OrderCreatedEvent event = new OrderCreatedEvent(UUID.randomUUID(), 1L, UUID.randomUUID(),
            Instant.now(), "OrderCreated", new OrderCreatedEvent.Payload(1001L, BigDecimal.TEN));
    private Message message() {
        MessageProperties props = new MessageProperties(); props.setDeliveryTag(7L);
        return new Message(new byte[0], props);
    }
    @Test void acknowledgesOnlyAfterProcessing() throws Exception {
        when(codec.decode(any())).thenReturn(event); when(service.process(event)).thenReturn(true);
        consumer.receive(message(), channel);
        var sequence = inOrder(service, channel);
        sequence.verify(service).process(event); sequence.verify(channel).basicAck(7L, false);
        verify(channel, never()).basicReject(anyLong(), anyBoolean());
    }
    @Test void duplicateIsAcknowledged() throws Exception {
        when(codec.decode(any())).thenReturn(event); when(service.process(event)).thenReturn(false);
        consumer.receive(message(), channel);
        verify(channel).basicAck(7L, false);
    }
    @Test void transientFailureRetriesThreeTotalAttemptsThenRejectsToDlx() throws Exception {
        when(codec.decode(any())).thenReturn(event);
        when(service.process(event)).thenThrow(new TransientDataAccessResourceException("Temporary"));
        consumer.receive(message(), channel);
        verify(service, times(3)).process(event);
        verify(channel).basicReject(7L, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }
    @Test void transientFailureCanRecover() throws Exception {
        when(codec.decode(any())).thenReturn(event);
        when(service.process(event)).thenThrow(new TransientDataAccessResourceException("Temporary")).thenReturn(true);
        consumer.receive(message(), channel);
        verify(service, times(2)).process(event); verify(channel).basicAck(7L, false);
    }
    @Test void invalidEventFailsImmediatelyByDefault() throws Exception {
        when(codec.decode(any())).thenThrow(new InvalidEventException());
        consumer.receive(message(), channel);
        verify(codec).decode(any()); verifyNoInteractions(service);
        verify(channel).basicReject(7L, false);
    }
    @Test void demoInvalidEventShowsThreeAttempts() throws Exception {
        when(codec.decode(any())).thenThrow(new InvalidEventException());
        new OrderConsumer(codec, service, true, 0).receive(message(), channel);
        verify(codec, times(3)).decode(any()); verify(channel).basicReject(7L, false);
    }
    @Test void malformedJsonNeverRetriesEvenInDemo() throws Exception {
        when(codec.decode(any())).thenThrow(new MalformedEventException());
        new OrderConsumer(codec, service, true, 0).receive(message(), channel);
        verify(codec).decode(any()); verifyNoInteractions(service); verify(channel).basicReject(7L, false);
    }
    @Test void acknowledgementFailureDoesNotRepeatProcessing() throws Exception {
        when(codec.decode(any())).thenReturn(event); when(service.process(event)).thenReturn(true);
        doThrow(new IOException("Channel closed")).when(channel).basicAck(7L, false);
        assertThatThrownBy(() -> consumer.receive(message(), channel)).isInstanceOf(IOException.class);
        verify(service).process(event); verify(channel, never()).basicReject(anyLong(), anyBoolean());
    }
    @Test void unknownPermanentFailureIsRejected() throws Exception {
        when(codec.decode(any())).thenReturn(event);
        when(service.process(event)).thenThrow(new IllegalStateException());
        consumer.receive(message(), channel);
        verify(service).process(event); verify(channel).basicReject(7L, false);
    }
}
