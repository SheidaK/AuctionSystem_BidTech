package com.BidTech.auctionSystem;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class NotificationListener {

    private final List<String> notifications = new ArrayList<>();

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