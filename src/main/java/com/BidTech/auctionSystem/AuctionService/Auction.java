package com.BidTech.auctionSystem.AuctionService;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/*
 * Auction Entity
 * --------------
 * Represents an auction in the system.
 * Each auction is associated with an item and maintains
 * information about the highest bid and when the auction ends.
 *
 * This entity is stored in the AuctionDB database.
 */

@Entity
public class Auction {

    // Primary key for the auction
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ID of the item being auctioned (comes from Catalogue service)
    private Long itemId;

    // ID of the user currently holding the highest bid
    private Long highestBidderId;

    // Current highest bid amount
    private double highestBid;

    // End time of the auction
    private LocalDateTime endTime;

    // Indicates whether the auction is active or ended
    private boolean active;

    // Default constructor required by JPA
    public Auction() {
    }

    public Auction(Long itemId, double startingPrice, LocalDateTime endTime) {
        this.itemId = itemId;
        this.highestBid = startingPrice;
        this.endTime = endTime;
        this.active = true;
    }

    // Getters and setters

    public Long getId() {
        return id;
    }

    public Long getItemId() {
        return itemId;
    }

    public Long getHighestBidderId() {
        return highestBidderId;
    }

    public void setHighestBidderId(Long highestBidderId) {
        this.highestBidderId = highestBidderId;
    }

    public double getHighestBid() {
        return highestBid;
    }

    public void setHighestBid(double highestBid) {
        this.highestBid = highestBid;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public boolean isActive() {
        return active;
    }

    public void endAuction() {
        this.active = false;
    }
}