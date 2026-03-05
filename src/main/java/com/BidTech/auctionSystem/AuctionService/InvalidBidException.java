package com.BidTech.auctionSystem.AuctionService;

/*
 * Exception thrown when a bid is invalid.
 */

public class InvalidBidException extends RuntimeException {

    public InvalidBidException(String message) {
        super(message);
    }
}