package com.BidTech.auctionSystem.AuctionService;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * BidRepository — Spring Data JPA repository for {@link Bid} entities.
 *
 * <p>In addition to the standard CRUD operations inherited from {@link JpaRepository},
 * this repository provides a custom query method to retrieve all bids for a specific auction.
 *
 * <p>This repository is bound to the {@code auctionEntityManagerFactory} and
 * {@code AUCTION.db} via {@link com.BidTech.auctionSystem.config.AuctionDbConfig}.
 */
public interface BidRepository extends JpaRepository<Bid, Long> {

    /**
     * Retrieves all bids associated with a specific auction, in insertion order.
     *
     * <p>Spring Data JPA automatically generates the SQL:
     * {@code SELECT * FROM bid WHERE auction_id = ?}
     *
     * @param auctionId the ID of the auction whose bids to retrieve
     * @return a list of all bids for the given auction (empty list if none)
     */
    List<Bid> findByAuctionId(Long auctionId);
}