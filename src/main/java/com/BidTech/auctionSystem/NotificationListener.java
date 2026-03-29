package com.BidTech.auctionSystem;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class NotificationListener {

    private final List<String> notifications = new ArrayList<>();

    @RabbitListener(queues = "notification.queue")
    public void handleEvent(Map<String, Object> event) {

        String type = (String) event.get("type");
        String message = "";

        switch (type) {

            case "BidPlaced":
                message = "New bid on auction " + event.get("auctionId") +
                        ": $" + event.get("amount");
                break;

            case "AuctionEnded":
                message = "Auction " + event.get("auctionId") +
                        " ended. Winner: " + event.get("winnerId");
                break;

            case "PaymentCompleted":
                message = "Payment completed for auction " + event.get("auctionId");
                break;
        }

        notifications.add(message);

        System.out.println("🔔 " + message);
    }

    public List<String> getNotifications() {
        return notifications;
    }
}