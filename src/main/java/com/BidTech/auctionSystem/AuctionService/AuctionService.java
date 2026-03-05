package com.BidTech.auctionSystem.AuctionService;

import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

/*
 * AuctionService
 * --------------
 * Contains the core business logic of the auction system.
 * Implements operations described in the AuctionAPI from the report.
 */

@Service
public class AuctionService {

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;

    public AuctionService(AuctionRepository auctionRepository, BidRepository bidRepository) {
        this.auctionRepository = auctionRepository;
        this.bidRepository = bidRepository;
    }

    /*
     * submitBid
     * ---------
     * Validates and records a bid placed by a user.
     *
     * Conditions checked:
     * - Auction must exist
     * - Auction must still be active
     * - Bid must be greater than the current highest bid
     */
    public Bid submitBid(Long auctionId, Long userId, double bidAmount) {

        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new AuctionNotFoundException(auctionId));

        if (!auction.isActive() || LocalDateTime.now().isAfter(auction.getEndTime())) {
            throw new InvalidBidException("Auction has ended.");
        }

        if (bidAmount <= auction.getHighestBid()) {
            throw new InvalidBidException("Bid must be higher than current highest bid.");
        }

        // Create new bid record
        Bid bid = new Bid(auctionId, userId, bidAmount);
        bidRepository.save(bid);

        // Update auction highest bid
        auction.setHighestBid(bidAmount);
        auction.setHighestBidderId(userId);
        auctionRepository.save(auction);

        return bid;
    }

    /*
     * getHighestBid
     * -------------
     * Returns the highest bid amount for an auction.
     */
    public double getHighestBid(Long auctionId) {

        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new AuctionNotFoundException(auctionId));

        return auction.getHighestBid();
    }

    /*
     * getBidHistory
     * -------------
     * Returns all bids associated with an auction.
     */
    public List<Bid> getBidHistory(Long auctionId) {
        return bidRepository.findByAuctionId(auctionId);
    }

    /*
     * endAuction
     * ----------
     * Ends the auction and determines the winner.
     */
    public Auction endAuction(Long auctionId) {

        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new AuctionNotFoundException(auctionId));

        auction.endAuction();
        return auctionRepository.save(auction);
    }

    /*
     * remainingTime
     * -------------
     * Returns remaining time (in seconds) before the auction ends.
     */
    public long remainingTime(Long auctionId) {

        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new AuctionNotFoundException(auctionId));

        return java.time.Duration.between(LocalDateTime.now(), auction.getEndTime()).getSeconds();
    }
}