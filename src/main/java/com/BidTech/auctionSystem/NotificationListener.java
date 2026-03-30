package com.BidTech.auctionSystem;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class NotificationListener {

    private final List<String> notifications = new ArrayList<>();

    /**
     * Receives messages from RabbitMQ queue and stores them in memory for the UI.
     */
    @RabbitListener(queues = "notification.queue")
    public void handleEvent(Map<String, Object> event) {
        String type = (String) event.get("type");
        String message = "";

        if ("BidPlaced".equals(type)) {
            message = "New bid on auction " + event.get("auctionId") + ": $" + event.get("amount");
        } else if ("AuctionEnded".equals(type)) {
            message = "Auction " + event.get("auctionId") + " ended. Winner: User #" + event.get("winnerId");
        } else if ("PaymentCompleted".equals(type)) {
            message = "Payment completed for auction " + event.get("auctionId") + ". TX: " + event.get("transactionId");
        } else {
            message = "Notification: " + event.toString();
        }

        notifications.add(message);
        System.out.println("🔔 " + message);
    }

    /**
     * Add a notification message
     */
    public void addNotification(String message) {
        notifications.add(message);
        System.out.println("🔔 " + message);
    }

    /**
     * Returns all notifications
     */
    public List<String> getNotifications() {
        return new ArrayList<>(notifications);
    }

    /**
     * Clears all notifications
     */
    public void clearNotifications() {
        notifications.clear();
    }
}