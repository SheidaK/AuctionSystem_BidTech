package com.BidTech.auctionSystem.IAMService;


public class UserNotFoundException extends RuntimeException {

    /**
     * Creates a new exception identifying the missing user.
     *
     * @param id the ID of the user that was not found
     */
    public UserNotFoundException(Long id) {
        super("Could not find user " + id);
    }
}