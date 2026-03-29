package com.BidTech.auctionSystem.AuctionService;

import java.time.LocalDateTime;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * Auction — JPA entity representing a single auction event.
 *
 * <p>An auction is created for a specific catalogue item (identified by {@code itemId}).
 * It tracks the current highest bid, the user who placed that bid, and when the auction
 * is scheduled to end. Auctions last one hour from the moment they are created.
 *
 * <p>This entity is persisted in {@code AUCTION.db} via the {@code auctionEntityManagerFactory}
 * configured in {@link com.BidTech.auctionSystem.config.AuctionDbConfig}.
 *
 * <p><b>Lifecycle:</b>
 * <ol>
 *   <li>Created with {@code active = true} and {@code endTime = now + 1 hour}</li>
 *   <li>Bids are placed; {@code highestBid} and {@code highestBidderId} are updated</li>
 *   <li>Ended by calling {@link #endAuction()}, which sets {@code active = false}</li>
 * </ol>
 */
@Entity
public class Auction {

    /**
     * Auto-generated primary key for the auction record.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The ID of the catalogue item being auctioned.
     * This references a {@code Product} in the CatalogueService, but there is no
     * foreign-key constraint because the two services use separate databases.
     */
    private Long itemId;

    /**
     * The ID of the user who currently holds the highest bid.
     * {@code null} if no bids have been placed yet.
     */
    private Long highestBidderId;

    /**
     * The current highest bid amount in dollars.
     * Initialised to the starting price when the auction is created.
     */
    private double highestBid;

    /**
     * The date and time when this auction is scheduled to end.
     * Set to {@code now + 1 hour} at creation time.
     */
    private LocalDateTime endTime;

    /**
     * Whether the auction is currently active.
     * {@code true} means bids can still be placed; {@code false} means the auction has ended.
     */
    private boolean active;

    /**
     * Default no-argument constructor required by JPA.
     * Do not use this directly — use {@link #Auction(Long, double)} instead.
     */
    public Auction() {
    }

    /**
     * Creates a new active auction for the given item.
     *
     * @param itemId       the ID of the catalogue item being auctioned
     * @param startingPrice the opening bid amount; becomes the initial {@code highestBid}
     */
    public Auction(Long itemId, double startingPrice) {
        this.itemId = itemId;
        this.highestBid = startingPrice;
        this.endTime = LocalDateTime.now().plusHours(1); // auction lasts 1 hour
        this.active = true;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    /**
     * Returns the unique identifier of this auction.
     *
     * @return the auction ID
     */
    public Long getId() {
        return id;
    }

    /**
     * Returns the ID of the catalogue item being auctioned.
     *
     * @return the item ID
     */
    public Long getItemId() {
        return itemId;
    }

    /**
     * Returns the ID of the user who currently holds the highest bid.
     *
     * @return the highest bidder's user ID, or {@code null} if no bids placed
     */
    public Long getHighestBidderId() {
        return highestBidderId;
    }

    /**
     * Returns the current highest bid amount.
     *
     * @return the highest bid in dollars
     */
    public double getHighestBid() {
        return highestBid;
    }

    /**
     * Returns the scheduled end time of this auction.
     *
     * @return the auction end time
     */
    public LocalDateTime getEndTime() {
        return endTime;
    }

    /**
     * Returns whether this auction is currently active.
     *
     * @return {@code true} if the auction is open for bids; {@code false} if ended
     */
    public boolean isActive() {
        return active;
    }

    // -------------------------------------------------------------------------
    // Setters
    // -------------------------------------------------------------------------

    /**
     * Updates the user ID of the current highest bidder.
     *
     * @param highestBidderId the user ID of the new highest bidder
     */
    public void setHighestBidderId(Long highestBidderId) {
        this.highestBidderId = highestBidderId;
    }

    /**
     * Updates the current highest bid amount.
     *
     * @param highestBid the new highest bid in dollars
     */
    public void setHighestBid(double highestBid) {
        this.highestBid = highestBid;
    }

    // -------------------------------------------------------------------------
    // Business methods
    // -------------------------------------------------------------------------

    /**
     * Ends this auction by setting {@code active} to {@code false}.
     * Once ended, no further bids can be placed.
     * The highest bidder at this point is the winner.
     */
    public void endAuction() {
        this.active = false;
    }
}
