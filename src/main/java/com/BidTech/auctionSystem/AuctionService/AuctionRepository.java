package com.BidTech.auctionSystem.AuctionService;

import org.springframework.data.jpa.repository.JpaRepository;

/*
 * AuctionRepository
 * -----------------
 * Handles database operations for Auction entities.
 */

public interface AuctionRepository extends JpaRepository<Auction, Long> {
}