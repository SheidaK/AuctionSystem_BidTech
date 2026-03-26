package com.BidTech.auctionSystem.AuctionService;

/**
 * AuctionNotFoundException — thrown when an auction cannot be found by its ID.
 *
 * <p>This is an unchecked exception (extends {@link RuntimeException}) that is thrown
 * by {@link AuctionService} when a requested auction ID does not exist in the database.
 *
 * <p>Spring's default exception handling will return a 500 response for this exception.
 * For a production system, a {@code @ControllerAdvice} handler should map this to a
 * 404 Not Found response.
 */
public class AuctionNotFoundException extends RuntimeException {

    /**
     * Creates a new exception with a message identifying the missing auction.
     *
     * @param id the ID of the auction that was not found
     */
    public AuctionNotFoundException(Long id) {
        super("Auction not found with id " + id);
    }
}