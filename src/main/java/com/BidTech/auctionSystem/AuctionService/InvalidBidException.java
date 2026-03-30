package com.BidTech.auctionSystem.AuctionService;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * InvalidBidException — thrown when a bid fails validation rules.
 *
 * <p>This exception is thrown by {@link AuctionService#submitBid} when:
 * <ul>
 *   <li>The auction has already ended (inactive or past end time)</li>
 *   <li>The bid amount is not strictly greater than the current highest bid</li>
 * </ul>
 *
 * <p>The message passed to the constructor describes the specific reason for rejection.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidBidException extends RuntimeException {

    /**
     * Creates a new exception with a descriptive message.
     *
     * @param message a human-readable explanation of why the bid is invalid
     */
    public InvalidBidException(String message) {
        super(message);
    }
}