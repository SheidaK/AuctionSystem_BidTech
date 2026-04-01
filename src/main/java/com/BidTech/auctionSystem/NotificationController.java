package com.BidTech.auctionSystem;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationListener notificationListener;

    /**
     * Returns all current notifications.
     * Called by the UI when the page loads or Refresh button is clicked.
     */
    @GetMapping
    public ResponseEntity<List<String>> getNotifications() {
        try {
            return ResponseEntity.ok(notificationListener.getNotifications());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(List.of("Error loading notifications: " + e.getMessage()));
        }
    }

    /**
     * Clears all notifications.
     * Called when user clicks "Clear All" button.
     */
    @PostMapping("/clear")
    public ResponseEntity<Void> clearNotifications() {
        notificationListener.clearNotifications();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/count")
    public ResponseEntity<Integer> getNotificationCount() {
        return ResponseEntity.ok(notificationListener.getNotifications().size());
    }

    @GetMapping("/test")
    public ResponseEntity<String> testNotification() {
        notificationListener.addNotification("Test notification: Payment completed successfully!");
        return ResponseEntity.ok("Test notification added");
    }
}