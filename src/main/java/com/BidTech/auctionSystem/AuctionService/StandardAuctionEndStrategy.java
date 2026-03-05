package com.BidTech.auctionSystem.AuctionService;

import org.springframework.stereotype.Component;

/*
 * Concrete Strategy
 * -----------------
 * Standard auction ending behavior.
 */

@Component
public class StandardAuctionEndStrategy implements AuctionEndStrategy {

    @Override
    public void endAuction(Auction auction) {
        auction.endAuction();
    }
}