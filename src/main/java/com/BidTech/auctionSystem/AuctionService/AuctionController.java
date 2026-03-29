package com.BidTech.auctionSystem.AuctionService;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * AuctionController — REST controller that exposes the Auction Service API.
 *
 * <p>All endpoints are prefixed with {@code /auction}. This controller delegates
 * all business logic to {@link AuctionService} and simply maps HTTP requests to
 * service method calls.
 *
 * <p><b>Endpoints summary:</b>
 * <ul>
 *   <li>{@code POST /auction/create} — create a new auction</li>
 *   <li>{@code POST /auction/{id}/bid} — place a bid</li>
 *   <li>{@code GET  /auction/{id}/highest} — get current highest bid</li>
 *   <li>{@code GET  /auction/{id}/history} — get full bid history</li>
 *   <li>{@code GET  /auction/{id}/remaining} — get remaining time in seconds</li>
 *   <li>{@code POST /auction/{id}/end} — manually end an auction</li>
 * </ul>
 */
@RestController
@RequestMapping("/auction")
public class AuctionController {

    /** The service layer that contains all auction business logic. */
    private final AuctionService auctionService;

    /**
     * Constructor injection of the auction service.
     *
     * @param auctionService the auction business logic service
     */
    public AuctionController(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    /**
     * Creates a new auction for a catalogue item.
     *
     * <p>Example: {@code POST /auction/create?itemId=5&startingPrice=100.00}
     *
     * @param itemId        the ID of the catalogue item to auction
     * @param startingPrice the opening bid amount
     * @return the created {@link Auction} entity as JSON
     */
    @PostMapping("/create")
    public Auction createAuction(
            @RequestParam Long itemId,
            @RequestParam double startingPrice) {
        return auctionService.createAuction(itemId, startingPrice);
    }

    /**
     * Places a bid on an active auction.
     *
     * <p>Example: {@code POST /auction/1/bid?userId=3&amount=150.00}
     *
     * @param auctionId the ID of the auction to bid on
     * @param userId    the ID of the user placing the bid
     * @param amount    the bid amount in dollars
     * @return the saved {@link Bid} entity as JSON
     */
    @PostMapping("/{auctionId}/bid")
    public Bid placeBid(
            @PathVariable Long auctionId,
            @RequestParam Long userId,
            @RequestParam double amount) {
        return auctionService.submitBid(auctionId, userId, amount);
    }

    /**
     * Returns the current highest bid amount for an auction.
     *
     * <p>Example: {@code GET /auction/1/highest}
     *
     * @param auctionId the ID of the auction
     * @return the highest bid as a plain number
     */
    @GetMapping("/{auctionId}/highest")
    public double getHighestBid(@PathVariable Long auctionId) {
        return auctionService.getHighestBid(auctionId);
    }

    /**
     * Returns the complete bid history for an auction.
     *
     * <p>Example: {@code GET /auction/1/history}
     *
     * @param auctionId the ID of the auction
     * @return a list of all {@link Bid} records as JSON
     */
    @GetMapping("/{auctionId}/history")
    public List<Bid> getBidHistory(@PathVariable Long auctionId) {
        return auctionService.getBidHistory(auctionId);
    }

    /**
     * Returns the number of seconds remaining until the auction ends.
     *
     * <p>Example: {@code GET /auction/1/remaining}
     *
     * @param auctionId the ID of the auction
     * @return remaining time in seconds (0 if ended)
     */
    @GetMapping("/{auctionId}/remaining")
    public long remainingTime(@PathVariable Long auctionId) {
        return auctionService.remainingTime(auctionId);
    }

    /**
     * Manually ends an auction, marking it as inactive.
     *
     * <p>Example: {@code POST /auction/1/end}
     *
     * @param auctionId the ID of the auction to end
     * @return the updated {@link Auction} entity as JSON
     */
    @PostMapping("/{auctionId}/end")
    public Auction endAuction(@PathVariable Long auctionId) {
        return auctionService.endAuction(auctionId);
    }

    @GetMapping
    public List<Auction> getAllAuctions() {
        return auctionService.getAllAuctions();
    }
}