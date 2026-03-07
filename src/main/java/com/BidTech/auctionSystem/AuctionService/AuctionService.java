package com.BidTech.auctionSystem.AuctionService;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuctionService {

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;

    public AuctionService(AuctionRepository auctionRepository,
                          BidRepository bidRepository) {
        this.auctionRepository = auctionRepository;
        this.bidRepository = bidRepository;
    }

    //Create auction
    public Auction createAuction(Long itemId, double startingPrice) {

        Auction auction = new Auction(itemId, startingPrice);

        return auctionRepository.save(auction);
    }

    //Submit bid
    public Bid submitBid(Long auctionId, Long userId, double amount) {

        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new AuctionNotFoundException(auctionId));

        if (!auction.isActive() || LocalDateTime.now().isAfter(auction.getEndTime())) {
            throw new InvalidBidException("Auction has ended.");
        }

        if (amount <= auction.getHighestBid()) {
            throw new InvalidBidException("Bid must be higher than current highest bid.");
        }

        Bid bid = new Bid(auctionId, userId, amount);

        bidRepository.save(bid);
        auction.setHighestBid(amount);
        auction.setHighestBidderId(userId);

        auctionRepository.save(auction);

        return bid;
    }

    //Get highest bid
    public double getHighestBid(Long auctionId) {

        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new AuctionNotFoundException(auctionId));

        return auction.getHighestBid();
    }

    //Bid history
    public List<Bid> getBidHistory(Long auctionId) {

        return bidRepository.findByAuctionId(auctionId);
    }

    //Remaining time
    public long remainingTime(Long auctionId) {

        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new AuctionNotFoundException(auctionId));

        if (!auction.isActive()) {
            return 0;
        }

        long seconds = Duration.between(
                LocalDateTime.now(),
                auction.getEndTime()
        ).getSeconds();

        return Math.max(seconds, 0);
    }

    //End auction
    public Auction endAuction(Long auctionId) {

        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new AuctionNotFoundException(auctionId));

        auction.endAuction();

        return auctionRepository.save(auction);
    }
}