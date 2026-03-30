package com.BidTech.auctionSystem;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
        return ResponseEntity.ok(notificationListener.getNotifications());
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

    @GetMapping("/test")
    public ResponseEntity<String> testNotification() {
        notificationListener.addNotification("Test notification: Payment completed successfully!");
        return ResponseEntity.ok("Test notification added");
    }
}