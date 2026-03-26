package com.BidTech.auctionSystem.AuctionService;

/**
 * AuctionEndStrategy — Strategy pattern interface for ending auctions.
 *
 * <p>Different implementations of this interface can define different behaviours
 * for how an auction ends. For example:
 * <ul>
 *   <li>{@link StandardAuctionEndStrategy} — simply marks the auction as inactive</li>
 *   <li>A future "ReserveNotMet" strategy could cancel the auction if the reserve price
 *       was not reached</li>
 * </ul>
 *
 * <p>This follows the <b>Strategy design pattern</b>: the algorithm for ending an auction
 * is encapsulated behind this interface, allowing it to be swapped without changing
 * the calling code.
 */
public interface AuctionEndStrategy {

    /**
     * Executes the auction-ending logic for the given auction.
     *
     * @param auction the auction to end; implementations should update its state
     */
    void endAuction(Auction auction);
}