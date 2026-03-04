package com.BidTech.auctionSystem.AuctionService;

/*
 * Exception thrown when an auction does not exist.
 */

public class AuctionNotFoundException extends RuntimeException {

    public AuctionNotFoundException(Long id) {
        super("Auction not found with id " + id);
    }
}