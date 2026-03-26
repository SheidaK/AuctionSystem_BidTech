package com.BidTech.auctionSystem.AuctionService;

import org.springframework.stereotype.Component;

/**
 * StandardAuctionEndStrategy — the default implementation of {@link AuctionEndStrategy}.
 *
 * <p>This strategy ends an auction by calling {@link Auction#endAuction()}, which sets
 * the auction's {@code active} flag to {@code false}. No additional logic is applied
 * (e.g., no reserve price check, no automatic winner notification).
 *
 * <p>Registered as a Spring {@code @Component} so it can be injected wherever an
 * {@link AuctionEndStrategy} is needed.
 */
@Component
public class StandardAuctionEndStrategy implements AuctionEndStrategy {

    /**
     * Ends the auction by marking it as inactive.
     *
     * @param auction the auction to end
     */
    @Override
    public void endAuction(Auction auction) {
        auction.endAuction();
    }
}