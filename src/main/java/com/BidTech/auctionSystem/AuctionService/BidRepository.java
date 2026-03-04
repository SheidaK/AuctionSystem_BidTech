package com.BidTech.auctionSystem.AuctionService;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/*
 * BidRepository
 * -------------
 * Used to store and retrieve bid history.
 */

public interface BidRepository extends JpaRepository<Bid, Long> {

    // Retrieve all bids associated with an auction
    List<Bid> findByAuctionId(Long auctionId);
}