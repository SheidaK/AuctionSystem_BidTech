package com.BidTech.auctionSystem.AuctionService;

/**
 * AuctionObserver — Observer pattern interface for bid change notifications.
 *
 * <p>Any class that wants to be notified when a new highest bid is placed should
 * implement this interface. The concrete implementation {@link BidNotificationService}
 * currently logs the notification to the console.
 *
 * <p>This follows the <b>Observer design pattern</b>: observers register interest in
 * auction events and are notified when those events occur, without the auction logic
 * needing to know the details of what each observer does.
 */
public interface AuctionObserver {

    /**
     * Called when a new highest bid has been placed on an auction.
     *
     * @param auctionId      the ID of the auction where the bid was placed
     * @param newHighestBid  the new highest bid amount in dollars
     */
    void update(Long auctionId, double newHighestBid);
}