package com.BidTech.auctionSystem.AuctionService;

import org.springframework.web.bind.annotation.*;
import java.util.List;

/*
 * AuctionController
 * -----------------
 * Exposes REST endpoints for interacting with the Auction service.
 */

@RestController
@RequestMapping("/auction")
public class AuctionController {

    private final AuctionService auctionService;

    public AuctionController(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    /*
     * Place a bid
     */
    @PostMapping("/{auctionId}/bid")
    public Bid placeBid(
            @PathVariable Long auctionId,
            @RequestParam Long userId,
            @RequestParam double amount) {

        return auctionService.submitBid(auctionId, userId, amount);
    }

    /*
     * Get highest bid
     */
    @GetMapping("/{auctionId}/highest")
    public double getHighestBid(@PathVariable Long auctionId) {
        return auctionService.getHighestBid(auctionId);
    }

    /*
     * Get bid history
     */
    @GetMapping("/{auctionId}/history")
    public List<Bid> getBidHistory(@PathVariable Long auctionId) {
        return auctionService.getBidHistory(auctionId);
    }

    /*
     * Get remaining auction time
     */
    @GetMapping("/{auctionId}/remaining")
    public long remainingTime(@PathVariable Long auctionId) {
        return auctionService.remainingTime(auctionId);
    }

    /*
     * End auction
     */
    @PostMapping("/{auctionId}/end")
    public Auction endAuction(@PathVariable Long auctionId) {
        return auctionService.endAuction(auctionId);
    }
}