package com.BidTech.auctionSystem.AuctionService;

/*
 * Strategy Pattern
 * ----------------
 * Different strategies determine how an auction ends.
 */

public interface AuctionEndStrategy {

    void endAuction(Auction auction);
}