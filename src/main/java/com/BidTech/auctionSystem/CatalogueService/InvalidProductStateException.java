package com.BidTech.auctionSystem.CatalogueService;

/**
 * Exception thrown when an operation is attempted on a product in an invalid state
 */
public class InvalidProductStateException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    public InvalidProductStateException(String message) {
        super(message);
    }
    
    public InvalidProductStateException(String message, Throwable cause) {
        super(message, cause);
    }
}
