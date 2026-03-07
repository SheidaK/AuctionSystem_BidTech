package com.BidTech.auctionSystem.AuctionService;

/*
 * Observer Pattern
 * Observers are notified whenever a new highest bid occurs.
 */

public interface AuctionObserver {

    void update(Long auctionId, double newHighestBid);
}