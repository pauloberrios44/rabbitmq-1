package com.example.publisher;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class OutboxProcessorTest {
    @Test void confirmedEventIsMarkedSent() throws Exception {
        OutboxRepository repository = mock(OutboxRepository.class);
        OrderPublisher publisher = mock(OrderPublisher.class);
        OutboxEvent event = new OutboxEvent(UUID.randomUUID(), UUID.randomUUID(), "{}");
        when(repository.findPending(any())).thenReturn(List.of(event));
        new OutboxProcessor(repository, publisher).publishPending();
        verify(publisher).publish(event);
        assertThat(event.getSentAt()).isNotNull();
        assertThat(event.getAttempts()).isEqualTo(1);
    }
    @Test void failedEventRemainsPendingAndLaterRecovers() throws Exception {
        OutboxRepository repository = mock(OutboxRepository.class);
        OrderPublisher publisher = mock(OrderPublisher.class);
        OutboxEvent event = new OutboxEvent(UUID.randomUUID(), UUID.randomUUID(), "{}");
        when(repository.findPending(any())).thenReturn(List.of(event));
        doThrow(new IllegalStateException()).doNothing().when(publisher).publish(event);
        OutboxProcessor processor = new OutboxProcessor(repository, publisher);
        processor.publishPending();
        assertThat(event.getSentAt()).isNull();
        processor.publishPending();
        assertThat(event.getSentAt()).isNotNull();
        assertThat(event.getAttempts()).isEqualTo(2);
    }
}
