package com.BidTech.auctionSystem.AuctionService;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * AuctionRepository — Spring Data JPA repository for {@link Auction} entities.
 *
 * <p>Extending {@link JpaRepository} gives us all standard CRUD operations for free:
 * {@code save()}, {@code findById()}, {@code findAll()}, {@code deleteById()}, etc.
 *
 * <p>This repository is bound to the {@code auctionEntityManagerFactory} and
 * {@code AUCTION.db} via {@link com.BidTech.auctionSystem.config.AuctionDbConfig}.
 */
public interface AuctionRepository extends JpaRepository<Auction, Long> {
}