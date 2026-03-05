package com.BidTech.auctionSystem.AuctionService;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/*
 * Bid Entity
 * ----------
 * Represents a single bid placed by a user.
 * All bids are stored so that bid history can be retrieved.
 */

@Entity
public class Bid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Auction associated with this bid
    private Long auctionId;

    // User placing the bid
    private Long userId;

    // Amount of the bid
    private double amount;

    // Time when bid was placed
    private LocalDateTime timestamp;

    public Bid() {}

    public Bid(Long auctionId, Long userId, double amount) {
        this.auctionId = auctionId;
        this.userId = userId;
        this.amount = amount;
        this.timestamp = LocalDateTime.now();
    }

    public Long getAuctionId() {
        return auctionId;
    }

    public Long getUserId() {
        return userId;
    }

    public double getAmount() {
        return amount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}