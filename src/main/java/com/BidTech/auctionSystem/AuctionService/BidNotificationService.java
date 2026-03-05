package com.BidTech.auctionSystem.AuctionService;

import org.springframework.stereotype.Component;

/*
 * Concrete Observer
 * -----------------
 * Handles notifications when bids change.
 */

@Component
public class BidNotificationService implements AuctionObserver {

    @Override
    public void update(Long auctionId, double newHighestBid) {
        System.out.println(
                "Notification: Auction " + auctionId +
                        " has a new highest bid of $" + newHighestBid
        );
    }
}