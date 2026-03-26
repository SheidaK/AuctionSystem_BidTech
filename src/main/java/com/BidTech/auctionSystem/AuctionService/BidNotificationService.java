package com.BidTech.auctionSystem.AuctionService;

import org.springframework.stereotype.Component;

/**
 * BidNotificationService — concrete implementation of {@link AuctionObserver}.
 *
 * <p>This observer is notified whenever a new highest bid is placed on an auction.
 * Currently it logs the notification to standard output. In a production system,
 * this could be extended to send email notifications, push notifications, or
 * WebSocket messages to connected clients.
 *
 * <p>Registered as a Spring {@code @Component} so it can be auto-wired wherever needed.
 */
@Component
public class BidNotificationService implements AuctionObserver {

    /**
     * Handles a new highest bid event by printing a notification message.
     *
     * @param auctionId     the ID of the auction where the bid was placed
     * @param newHighestBid the new highest bid amount in dollars
     */
    @Override
    public void update(Long auctionId, double newHighestBid) {
        System.out.println(
                "Notification: Auction " + auctionId +
                        " has a new highest bid of $" + newHighestBid
        );
    }
}