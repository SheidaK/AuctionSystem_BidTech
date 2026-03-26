package com.BidTech.auctionSystem.AuctionService;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * Bid — JPA entity representing a single bid placed by a user in an auction.
 *
 * <p>Every time a user places a bid, a new {@code Bid} record is created and saved.
 * This gives us a complete bid history for any auction, which can be retrieved via
 * {@link BidRepository#findByAuctionId(Long)}.
 *
 * <p>Bids are persisted in {@code AUCTION.db} alongside {@link Auction} records.
 *
 * <p><b>Validation:</b> Bids are validated in {@link AuctionService#submitBid} before
 * a {@code Bid} entity is created. A bid is only saved if:
 * <ul>
 *   <li>The auction is still active</li>
 *   <li>The current time is before the auction's end time</li>
 *   <li>The bid amount is strictly greater than the current highest bid</li>
 * </ul>
 */
@Entity
public class Bid {

    /**
     * Auto-generated primary key for this bid record.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The ID of the auction this bid belongs to.
     * Used to retrieve bid history via {@link BidRepository#findByAuctionId(Long)}.
     */
    private Long auctionId;

    /**
     * The ID of the user who placed this bid.
     * References a {@code User} in the IAMService, but no foreign-key constraint
     * exists because the services use separate databases.
     */
    private Long userId;

    /**
     * The dollar amount of this bid.
     * Must be strictly greater than the auction's current {@code highestBid} at the
     * time of submission.
     */
    private double amount;

    /**
     * The exact date and time when this bid was placed.
     * Set automatically to {@code LocalDateTime.now()} at construction time.
     */
    private LocalDateTime timestamp;

    /**
     * Default no-argument constructor required by JPA.
     */
    public Bid() {}

    /**
     * Creates a new bid record.
     *
     * @param auctionId the ID of the auction being bid on
     * @param userId    the ID of the user placing the bid
     * @param amount    the bid amount in dollars
     */
    public Bid(Long auctionId, Long userId, double amount) {
        this.auctionId = auctionId;
        this.userId = userId;
        this.amount = amount;
        this.timestamp = LocalDateTime.now();
    }

    // -------------------------------------------------------------------------
    // Getters (no setters needed — bids are immutable once created)
    // -------------------------------------------------------------------------

    /**
     * Returns the ID of the auction this bid belongs to.
     *
     * @return the auction ID
     */
    public Long getAuctionId() {
        return auctionId;
    }

    /**
     * Returns the ID of the user who placed this bid.
     *
     * @return the user ID
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * Returns the dollar amount of this bid.
     *
     * @return the bid amount
     */
    public double getAmount() {
        return amount;
    }

    /**
     * Returns the timestamp when this bid was placed.
     *
     * @return the bid timestamp
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
