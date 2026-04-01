package com.BidTech.auctionSystem.AuctionService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.BidTech.auctionSystem.NotificationListener;
import com.BidTech.auctionSystem.RabbitMQConfig;
import org.springframework.stereotype.Service;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * AuctionService — core business logic for the Auction domain.
 *
 * <p>Handles auction creation, bid submission, bid history retrieval,
 * remaining time calculation, and auction termination.
 * All persistence is delegated to {@link AuctionRepository} and {@link BidRepository}.
 */
@Service
public class AuctionService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /** Repository for persisting and retrieving {@link Auction} entities. */
    private final AuctionRepository auctionRepository;

    /** Repository for persisting and retrieving {@link Bid} entities. */
    private final BidRepository bidRepository;

    /**
     * Constructor injection — Spring automatically provides the repository beans.
     *
     * @param auctionRepository the auction data access object
     * @param bidRepository     the bid data access object
     */
    public AuctionService(AuctionRepository auctionRepository,
                          BidRepository bidRepository) {
        this.auctionRepository = auctionRepository;
        this.bidRepository = bidRepository;
    }

    /**
     * Creates a new auction for a catalogue item.
     * The auction starts active with a 1-hour duration.
     *
     * @param itemId        the ID of the catalogue item to auction
     * @param startingPrice the opening bid amount
     * @return the saved {@link Auction} entity with its generated ID
     */
    public Auction createAuction(Long itemId, double startingPrice, LocalDateTime endDateTime, String itemName) {
        Auction auction = new Auction(itemId, startingPrice);
        auction.setEndTime(endDateTime);
        Auction savedAuction = auctionRepository.save(auction);

        Map<String, Object> event = new HashMap<>();
        event.put("type", "AuctionCreated");
        event.put("itemId", itemId);
        event.put("itemName", itemName); // Added to map
        event.put("startingPrice", startingPrice);
        event.put("endTime", endDateTime.toString());

        rabbitTemplate.convertAndSend("auction.events", "auction.created", event);

        return savedAuction;
    }

    /**
     * Submits a bid for an active auction.
     *
     * <p>Validation (in order):
     * <ol>
     *   <li>Auction must exist — throws {@link AuctionNotFoundException}</li>
     *   <li>Auction must be active and not past end time — throws {@link InvalidBidException}</li>
     *   <li>Bid amount must be strictly greater than current highest bid — throws {@link InvalidBidException}</li>
     * </ol>
     *
     * @param auctionId the ID of the auction to bid on
     * @param userId    the ID of the user placing the bid
     * @param amount    the bid amount in dollars
     * @return the saved {@link Bid} entity
     * @throws AuctionNotFoundException if no auction exists with the given ID
     * @throws InvalidBidException      if the auction has ended or the bid is too low
     */
    public Bid submitBid(Long auctionId, Long userId, double amount) {

        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new AuctionNotFoundException(auctionId));

        // Reject if auction is closed or time has expired
        if (!auction.isActive() || LocalDateTime.now().isAfter(auction.getEndTime())) {
            throw new InvalidBidException("Auction has ended.");
        }

        // LOW BID notification
        if (amount <= auction.getHighestBid()) {
            Map<String, Object> event = new HashMap<>();
            event.put("type", "BidRejected");
            event.put("auctionId", auctionId);
            event.put("userId", userId);
            event.put("attemptedAmount", amount);
            event.put("highestBid", auction.getHighestBid());

            rabbitTemplate.convertAndSend("auction.events", "bid.rejected", event);

            throw new InvalidBidException("Your bid must be higher than the currently highest bid.");
        }

        Bid bid = new Bid(auctionId, userId, amount);
        bidRepository.save(bid);

        // Update the auction's highest bid
        auction.setHighestBid(amount);
        auction.setHighestBidderId(userId);
        auctionRepository.save(auction);

        Map<String, Object> event = new HashMap<>();
        event.put("type", "BidPlaced");
        event.put("auctionId", auctionId);
        event.put("userId", userId);
        event.put("amount", amount);
        rabbitTemplate.convertAndSend("auction.events", "bid.placed", event);

        return bid;
    }

    /**
     * Returns the current highest bid amount for an auction.
     *
     * @param auctionId the ID of the auction
     * @return the highest bid in dollars
     * @throws AuctionNotFoundException if no auction exists with the given ID
     */
    public double getHighestBid(Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new AuctionNotFoundException(auctionId));
        return auction.getHighestBid();
    }

    /**
     * Returns the complete bid history for an auction.
     *
     * @param auctionId the ID of the auction
     * @return a list of all {@link Bid} records for this auction (may be empty)
     */
    public List<Bid> getBidHistory(Long auctionId) {
        return bidRepository.findByAuctionId(auctionId);
    }

    /**
     * Returns the number of seconds remaining until the auction ends.
     * Returns {@code 0} if the auction is inactive or already past its end time.
     *
     * @param auctionId the ID of the auction
     * @return remaining time in seconds, minimum 0
     * @throws AuctionNotFoundException if no auction exists with the given ID
     */
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

    /**
     * Ends an auction by marking it as inactive.
     * After this call, no further bids can be placed and the highest bidder is the winner.
     *
     * @param auctionId the ID of the auction to end
     * @return the updated {@link Auction} entity with {@code active = false}
     * @throws AuctionNotFoundException if no auction exists with the given ID
     */
    public Auction endAuction(Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new AuctionNotFoundException(auctionId));

        auction.endAuction();

        Map<String, Object> event = new HashMap<>();
        event.put("type", "AuctionEnded");
        event.put("auctionId", auctionId);
        event.put("winnerId", auction.getHighestBidderId());
        event.put("finalPrice", auction.getHighestBid());

        rabbitTemplate.convertAndSend("auction.events", "auction.ended", event);

        return auctionRepository.save(auction);
    }

    public List<Auction> getAllAuctions() {
        return auctionRepository.findAll();
    }

    @Scheduled(fixedRate = 5000) // runs every 5 seconds
    public void autoEndAuctions() {

        List<Auction> auctions = auctionRepository.findAll();

        for (Auction auction : auctions) {

            if (auction.isActive() &&
                    auction.getEndTime().isBefore(LocalDateTime.now())) {

                System.out.println("Auto-ending auction " + auction.getId());

                endAuction(auction.getId()); // reuse logic
            }
        }
    }
}
