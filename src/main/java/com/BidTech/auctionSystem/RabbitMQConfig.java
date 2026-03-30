package com.BidTech.auctionSystem;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.core.Queue; // <-- 1. Add this import

/**
 * RabbitMQConfig — configures the RabbitMQ message broker beans.
 *
 * <p>Key responsibilities:
 * <ol>
 *   <li>Declares the 'notification.queue' so it is auto-created in RabbitMQ on startup</li>
 *   <li>Provides a RabbitAdmin bean that triggers queue declaration before listeners start</li>
 *   <li>Configures JSON message conversion for pub/sub messages</li>
 * </ol>
 *
 * <p>Without the RabbitAdmin bean, the @RabbitListener in NotificationListener does a
 * passive declaration (check-only) which fails with 404 if the queue doesn't exist yet.
 * RabbitAdmin performs an active declaration — it creates the queue if missing.
 */
@Configuration
public class RabbitMQConfig {

    /** Queue name — must match the @RabbitListener annotation in NotificationListener. */
    public static final String NOTIFICATION_QUEUE = "notification.queue";
    public static final String AUCTION_EVENTS_EXCHANGE = "auction.events";
    public static final String AUCTION_QUEUE = "auction.events";

    @Bean
    public TopicExchange auctionEventsExchange() {
        return new TopicExchange(AUCTION_EVENTS_EXCHANGE);
    }

    /**
     * RabbitAdmin auto-declares all Queue, Exchange, and Binding beans on startup.
     * This ensures the notification queue exists before any listener tries to consume from it.
     *
     * @param connectionFactory the RabbitMQ connection factory (auto-configured by Spring Boot)
     * @return the RabbitAdmin bean
     */
    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        // setAutoStartup(true) ensures queues are declared as soon as the connection is ready
        admin.setAutoStartup(true);
        return admin;
    }

    /**
     * Declares the notification queue as a durable queue.
     * RabbitAdmin will create this queue in RabbitMQ if it doesn't exist.
     * durable=true means the queue survives RabbitMQ restarts.
     *
     * @return the Queue bean
     */
    @Bean
    public Queue notificationQueue() {
        return new Queue(NOTIFICATION_QUEUE, true);
    }

    @Bean
    public Queue auctionQueue() {
        return new Queue(AUCTION_QUEUE, true);
    }

    @Bean
    public Binding notificationBinding(Queue notificationQueue, TopicExchange auctionEventsExchange) {
        return BindingBuilder
                .bind(notificationQueue)
                .to(auctionEventsExchange)
                .with("#");   // catch all routing keys
    }

    /**
     * Configures JSON message conversion so RabbitMQ messages are
     * serialised/deserialised as JSON rather than Java serialisation.
     *
     * @return the Jackson message converter bean
     */
    @Bean
    public JacksonJsonMessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
