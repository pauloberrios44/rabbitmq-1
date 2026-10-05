package com.example.consumer;

import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.CannotCreateTransactionException;
import java.io.IOException;

@Component
public class OrderConsumer {
    private static final Logger log = LoggerFactory.getLogger(OrderConsumer.class);
    private static final int MAX_ATTEMPTS = 3;
    private final EventCodec codec;
    private final OrderService service;
    private final boolean retryInvalid;
    private final long delay;
    public OrderConsumer(EventCodec codec, OrderService service,
            @Value("${demo.retry-invalid:false}") boolean retryInvalid,
            @Value("${consumer.retry-delay-ms:500}") long delay) {
        if (delay < 0 || delay > 10000) throw new IllegalArgumentException("Invalid retry delay");
        this.codec = codec; this.service = service; this.retryInvalid = retryInvalid; this.delay = delay;
    }
    @RabbitListener(queues = RabbitMQConfig.QUEUE)
    public void receive(Message message, Channel channel) throws IOException {
        long tag = message.getMessageProperties().getDeliveryTag();
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            final OrderCreatedEvent event;
            final boolean inserted;
            try {
                event = codec.decode(message.getBody());
                inserted = service.process(event);
            } catch (RuntimeException failure) {
                boolean recoverable = failure instanceof TransientDataAccessException
                        || failure instanceof DataAccessResourceFailureException
                        || failure instanceof CannotCreateTransactionException;
                boolean demoRetry = retryInvalid && failure instanceof InvalidEventException
                        && !(failure instanceof MalformedEventException);
                log.warn("Consumo fallido deliveryTag={} attempt={}/{} category={}",
                        tag, attempt, MAX_ATTEMPTS, recoverable ? "TRANSIENT" : "PERMANENT");
                if ((!recoverable && !demoRetry) || attempt == MAX_ATTEMPTS) {
                    channel.basicReject(tag, false);
                    log.warn("Mensaje rechazado hacia DLQ deliveryTag={}", tag);
                    return;
                }
                try { Thread.sleep(delay); }
                catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    // Closing/recovering the channel lets RabbitMQ re-deliver the unacked message.
                    throw new IOException("Consumer interrupted", interrupted);
                }
                continue;
            }
            // Outside processing catch: an ACK I/O failure must never trigger business retries.
            channel.basicAck(tag, false);
            log.info("{} eventId={} correlationId={}", inserted ? "Orden procesada" : "Duplicado ignorado",
                    event.eventId(), event.correlationId());
            return;
        }
    }
}
