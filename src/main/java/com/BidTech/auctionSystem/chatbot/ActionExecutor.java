package com.BidTech.auctionSystem.chatbot;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.BidTech.auctionSystem.AuctionService.Auction;
import com.BidTech.auctionSystem.AuctionService.AuctionRepository;
import com.BidTech.auctionSystem.AuctionService.Bid;
import com.BidTech.auctionSystem.AuctionService.BidRepository;
import com.BidTech.auctionSystem.AuctionService.InvalidBidException;
import com.BidTech.auctionSystem.CatalogueService.ProductRepository;
import com.BidTech.auctionSystem.CatalogueService.ProductStatus;
import com.BidTech.auctionSystem.payment.repository.PaymentRepository;

/**
 * ActionExecutor — executes resolved intents against the live BidTech data layer.
 *
 * <p>This class has two categories of methods:
 * <ul>
 *   <li><b>Read methods</b> — fetch live data and return a plain-English summary string
 *       that is injected into the Ollama system prompt before the model responds.</li>
 *   <li><b>Write methods</b> — modify state (place a bid, process a payment).
 *       These are only called after the user has explicitly confirmed the action.</li>
 * </ul>
 *
 * <p>All methods return strings rather than domain objects so that ChatService can
 * directly embed the results into the Ollama prompt without any additional formatting.
 */
@Component
public class ActionExecutor {

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    /**
     * Searches products by keyword in name and description.
     * Uses the catalogue's JPQL search query for case-insensitive partial matching.
     *
     * @param keyword the search term (e.g. "laptop", "dell", "rolex")
     * @return a plain-English summary of matching products, or a "no results" message
     */
    public String searchProducts(String keyword) {
        var results = productRepository.searchByNameOrDescription(keyword);
        if (results.isEmpty()) {
            return "No products found matching '" + keyword + "'.";
        }

        // Deduplicate by name — seeder may create duplicates
        java.util.LinkedHashMap<String, com.BidTech.auctionSystem.CatalogueService.Product> unique = new java.util.LinkedHashMap<>();
        for (var p : results) {
            if (!unique.containsKey(p.getName())) {
                unique.put(p.getName(), p);
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Found ").append(unique.size()).append(" result(s) for '").append(keyword).append("':\n\n");
        int index = 1;
        for (var p : unique.values()) {
            sb.append(String.format(
                "%d. %s\n" +
                "   📂 Category: %s\n" +
                "   💰 Starting Price: $%.2f\n" +
                "   📋 Status: %s\n" +
                "   📝 %s\n\n",
                index++,
                p.getName(),
                p.getCategory(),
                p.getStartingPrice() != null ? p.getStartingPrice().doubleValue() : 0.0,
                p.getStatus(),
                p.getDescription() != null ? p.getDescription() : "No description"));
        }
        return sb.toString().trim();
    }

    // ── Read Methods ──────────────────────────────────────────────────────────

    /**
     * Fetches all active products and returns a formatted summary for Ollama.
     *
     * @return a plain-English list of active products, or a "no products" message
     */
    public String fetchActiveProducts() {
        var products = productRepository.findByStatus(ProductStatus.ACTIVE);
        if (products.isEmpty()) {
            return "There are currently no active products in the catalogue.";
        }

        // Deduplicate by product name — the seeder may create duplicates on restart.
        // Keep only the first occurrence of each product name.
        java.util.LinkedHashMap<String, com.BidTech.auctionSystem.CatalogueService.Product> unique = new java.util.LinkedHashMap<>();
        for (var p : products) {
            if (!unique.containsKey(p.getName())) {
                unique.put(p.getName(), p);
            }
        }

        // Format as a clean, readable list with emoji and clear structure
        StringBuilder sb = new StringBuilder();
        sb.append("Found ").append(unique.size()).append(" active product(s):\n\n");
        int index = 1;
        for (var p : unique.values()) {
            sb.append(String.format(
                "%d. %s\n" +
                "   📂 Category: %s\n" +
                "   💰 Starting Price: $%.2f\n" +
                "   📦 Condition: %s\n\n",
                index++,
                p.getName(),
                p.getCategory(),
                p.getStartingPrice() != null ? p.getStartingPrice().doubleValue() : 0.0,
                p.getCondition() != null ? p.getCondition() : "Not specified"));
        }
        return sb.toString().trim();
    }

    /**
     * Fetches products in a specific category and returns a formatted summary.
     *
     * @param category the category name (e.g. "Electronics", "Jewelry")
     * @return a plain-English list of products in that category
     */
    public String fetchProductsByCategory(String category) {
        var products = productRepository.findByCategory(category);
        if (products.isEmpty()) {
            return "No products found in the " + category + " category.";
        }

        // Deduplicate by name
        java.util.LinkedHashMap<String, com.BidTech.auctionSystem.CatalogueService.Product> unique = new java.util.LinkedHashMap<>();
        for (var p : products) {
            if (!unique.containsKey(p.getName())) {
                unique.put(p.getName(), p);
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Found ").append(unique.size()).append(" product(s) in ").append(category).append(":\n\n");
        int index = 1;
        for (var p : unique.values()) {
            sb.append(String.format(
                "%d. %s\n" +
                "   💰 Starting Price: $%.2f\n" +
                "   📋 Status: %s\n\n",
                index++,
                p.getName(),
                p.getStartingPrice() != null ? p.getStartingPrice().doubleValue() : 0.0,
                p.getStatus()));
        }
        return sb.toString().trim();
    }

    /**
     * Fetches all currently active auctions and returns a formatted summary.
     *
     * @return a readable list of active auctions, or a "no auctions" message
     */
    public String fetchActiveAuctions() {
        var auctions = auctionRepository.findByActiveTrue();
        if (auctions.isEmpty()) {
            return "There are currently no active auctions.\n" +
                "An admin needs to activate products and create auctions before they appear here.\n" +
                "Try \"show me all products\" to see what's in the catalogue.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Found ").append(auctions.size()).append(" active auction(s):\n\n");
        int index = 1;
        for (var a : auctions) {
            // Calculate remaining time for each auction
            long secsLeft = java.time.Duration.between(
                java.time.LocalDateTime.now(), a.getEndTime()).getSeconds();
            String timeStr = secsLeft <= 0 ? "Ending soon"
                : String.format("%dm %ds left", secsLeft / 60, secsLeft % 60);

            sb.append(String.format(
                "%d. Auction #%d\n" +
                "   📦 Item ID: %d\n" +
                "   💰 Current Highest Bid: $%.2f\n" +
                "   ⏱ Time Remaining: %s\n\n",
                index++, a.getId(), a.getItemId(),
                a.getHighestBid(), timeStr));
        }
        return sb.toString().trim();
    }

    /**
     * Fetches the status of a specific auction.
     *
     * @param auctionId the auction ID to look up
     * @return a plain-English summary of the auction's current state
     */
    public String fetchAuctionStatus(Long auctionId) {
        Optional<Auction> opt = auctionRepository.findById(auctionId);
        if (opt.isEmpty()) {
            return "Auction #" + auctionId + " was not found.";
        }
        Auction a = opt.get();
        return String.format(
            "Auction #%d: Item ID %d — Highest bid: $%.2f — Active: %s — Ends: %s",
            a.getId(), a.getItemId(), a.getHighestBid(),
            a.isActive() ? "Yes" : "No (ended)",
            a.getEndTime() != null ? a.getEndTime().toString() : "Unknown");
    }

    /**
     * Fetches the current highest bid for an auction.
     *
     * @param auctionId the auction ID
     * @return a plain-English statement of the current highest bid
     */
    public String fetchHighestBid(Long auctionId) {
        Optional<Auction> opt = auctionRepository.findById(auctionId);
        if (opt.isEmpty()) return "Auction #" + auctionId + " was not found.";
        Auction a = opt.get();
        return String.format("The current highest bid on auction #%d is $%.2f.",
            auctionId, a.getHighestBid());
    }

    /**
     * Fetches the remaining time for an auction in a human-readable format.
     *
     * @param auctionId the auction ID
     * @return a plain-English statement of time remaining, or "ended" if inactive
     */
    public String fetchRemainingTime(Long auctionId) {
        Optional<Auction> opt = auctionRepository.findById(auctionId);
        if (opt.isEmpty()) return "Auction #" + auctionId + " was not found.";
        Auction a = opt.get();
        if (!a.isActive()) return "Auction #" + auctionId + " has already ended.";

        // Calculate seconds remaining between now and the auction end time
        long secondsLeft = java.time.Duration.between(
            LocalDateTime.now(), a.getEndTime()).getSeconds();
        if (secondsLeft <= 0) return "Auction #" + auctionId + " has just ended.";

        long minutes = secondsLeft / 60;
        long seconds = secondsLeft % 60;
        return String.format("Auction #%d has %d minutes and %d seconds remaining.",
            auctionId, minutes, seconds);
    }

    /**
     * Fetches the full bid history for an auction.
     *
     * @param auctionId the auction ID
     * @return a plain-English list of all bids placed, or a "no bids" message
     */
    public String fetchBidHistory(Long auctionId) {
        List<Bid> bids = bidRepository.findByAuctionId(auctionId);
        if (bids.isEmpty()) {
            return "No bids have been placed on auction #" + auctionId + " yet.";
        }
        StringBuilder sb = new StringBuilder(
            "Bid history for auction #" + auctionId + " (" + bids.size() + " bids):\n");
        bids.forEach(b -> sb.append(String.format(
            "- User #%d bid $%.2f\n", b.getUserId(), b.getAmount())));
        return sb.toString();
    }

    /**
     * Computes bid recommendation signals for an auction and returns a structured
     * summary for Ollama to reason about.
     *
     * <p>Signals computed:
     * <ul>
     *   <li>Current highest bid</li>
     *   <li>Average bid increment across all bids in history</li>
     *   <li>Bid velocity (bids per minute over elapsed auction time)</li>
     *   <li>Estimated additional bids expected before close</li>
     *   <li>Confidence level (LOW if fewer than 3 bids)</li>
     * </ul>
     *
     * @param auctionId the auction ID to analyse
     * @return a structured plain-English summary for injection into the Ollama prompt
     */
    public String fetchBidRecommendation(Long auctionId) {
        Optional<Auction> opt = auctionRepository.findById(auctionId);
        if (opt.isEmpty()) return "Auction #" + auctionId + " was not found.";

        Auction auction = opt.get();
        if (!auction.isActive()) {
            return "Auction #" + auctionId + " has already ended — no bid recommendation possible.";
        }

        List<Bid> bids = bidRepository.findByAuctionId(auctionId);
        double highestBid = auction.getHighestBid();

        // Calculate seconds remaining between now and the auction end time
        long remainingSecs = java.time.Duration.between(
            LocalDateTime.now(), auction.getEndTime()).getSeconds();
        if (remainingSecs <= 0) {
            return "Auction #" + auctionId + " has just ended — no bid recommendation possible.";
        }

        // ── Signal 1: Average bid increment ──────────────────────────────────
        // Formula: (highest bid - first bid) / (number of bids - 1)
        // This tells us how much each bid typically increases the price.
        double avgIncrement = 0;
        if (bids.size() >= 2) {
            double firstBid = bids.get(0).getAmount();
            // Divide total price increase by number of increments (bids - 1)
            avgIncrement = (highestBid - firstBid) / (bids.size() - 1);
        }

        // ── Signal 2: Bid velocity ────────────────────────────────────────────
        // Formula: bids placed / elapsed minutes
        // Auctions last 1 hour (3600s). Elapsed = 3600 - remaining.
        // This tells us how frequently bids are being placed.
        long elapsedSecs = 3600 - remainingSecs;
        double bidsPerMinute = 0;
        if (elapsedSecs > 0 && !bids.isEmpty()) {
            bidsPerMinute = bids.size() / (elapsedSecs / 60.0);
        }

        // ── Signal 3: Expected additional bids before close ───────────────────
        // Formula: velocity * remaining minutes
        // Rounded to nearest integer — gives a rough count of future competition.
        int expectedMoreBids = (int) Math.round(bidsPerMinute * (remainingSecs / 60.0));

        // ── Signal 4: Confidence level ────────────────────────────────────────
        // With fewer than 3 bids, velocity and increment estimates are unreliable.
        // We flag this so Ollama can include a disclaimer in its response.
        boolean lowConfidence = bids.size() < 3;
        String confidence = lowConfidence ? "LOW (only " + bids.size() + " bid(s) — limited data)" : "NORMAL";

        // Build the structured summary string for Ollama prompt injection
        return String.format(
            "Bid recommendation data for Auction #%d:\n" +
            "- Current highest bid: $%.2f\n" +
            "- Number of bids placed: %d\n" +
            "- Bid amounts (oldest to newest): %s\n" +
            "- Average bid increment: $%.2f\n" +
            "- Time remaining: %d minutes\n" +
            "- Bid velocity: %.2f bids/minute\n" +
            "- Expected additional bids before close: ~%d\n" +
            "- Confidence: %s\n\n" +
            "Suggest a competitive bid amount to win this auction. " +
            "Explain your reasoning in 2-3 sentences. " +
            "Remind the user this is an estimate, not a guarantee. " +
            (lowConfidence ? "Note the low confidence due to limited bid history. " : ""),
            auctionId, highestBid, bids.size(),
            bids.stream().map(b -> String.format("$%.0f", b.getAmount()))
                .collect(Collectors.joining(", ")),
            avgIncrement, remainingSecs / 60, bidsPerMinute, expectedMoreBids, confidence);
    }

    /**
     * Fetches the status of a payment by transaction ID.
     *
     * @param transactionId the unique transaction ID string
     * @return a plain-English payment status statement
     */
    public String fetchPaymentStatus(String transactionId) {
        return paymentRepository.findByTransactionId(transactionId)
            .map(p -> String.format("Payment with transaction ID %s has status: %s.",
                transactionId, p.getStatus()))
            .orElse("No payment found with transaction ID " + transactionId + ".");
    }

    /**
     * Fetches a receipt summary by payment ID.
     *
     * @param paymentId the payment record ID
     * @return a plain-English receipt summary
     */
    public String fetchReceipt(Long paymentId) {
        return paymentRepository.findById(paymentId)
            .map(p -> String.format(
                "Receipt for payment #%d: Transaction ID %s — Amount: $%.2f — Status: %s.",
                p.getId(), p.getTransactionId(), p.getAmount(), p.getStatus()))
            .orElse("No payment found with ID " + paymentId + ".");
    }

    // ── Write Methods ─────────────────────────────────────────────────────────
    // These methods are only called after the user has confirmed the action.

    /**
     * Places a bid on an auction on behalf of the authenticated user.
     *
     * @param auctionId the auction to bid on
     * @param userId    the authenticated user placing the bid
     * @param amount    the bid amount in dollars
     * @return a plain-English result string (success or failure reason)
     * @throws InvalidBidException if the auction has ended or the bid is too low
     */
    public String placeBid(Long auctionId, Long userId, double amount) {
        Optional<Auction> opt = auctionRepository.findById(auctionId);
        if (opt.isEmpty()) return "Auction #" + auctionId + " was not found.";

        Auction auction = opt.get();

        // Validate auction is still active and not past end time
        if (!auction.isActive() || LocalDateTime.now().isAfter(auction.getEndTime())) {
            return "Auction #" + auctionId + " has ended — the bid could not be placed.";
        }

        // Validate bid is strictly higher than current highest
        if (amount <= auction.getHighestBid()) {
            return String.format(
                "Your bid of $%.2f is not higher than the current highest bid of $%.2f. " +
                "Please bid more than $%.2f.",
                amount, auction.getHighestBid(), auction.getHighestBid());
        }

        // Save the bid and update the auction's highest bid state
        Bid bid = new Bid(auctionId, userId, amount);
        bidRepository.save(bid);
        auction.setHighestBid(amount);
        auction.setHighestBidderId(userId);
        auctionRepository.save(auction);

        return String.format(
            "Bid placed successfully! Bid ID: %d — Amount: $%.2f on auction #%d. " +
            "You are now the highest bidder. Good luck! 🎉",
            bid.getId(), amount, auctionId);
    }

    /**
     * Processes a payment for an auction the user has won.
     *
     * @param auctionId the auction being paid for
     * @param userId    the authenticated user making the payment
     * @param amount    the payment amount in dollars
     * @return a plain-English result string (transaction ID on success, or failure reason)
     */
    public String processPayment(Long auctionId, Long userId, double amount) {
        if (amount <= 0) {
            return "Payment amount must be greater than zero.";
        }

        Optional<Auction> opt = auctionRepository.findById(auctionId);
        if (opt.isEmpty()) return "Auction #" + auctionId + " was not found.";

        Auction auction = opt.get();

        // Verify the user is the winner — must be highest bidder on an ended auction
        if (auction.isActive()) {
            return "Auction #" + auctionId + " is still active. " +
                "You can only pay after the auction has ended.";
        }
        if (!userId.equals(auction.getHighestBidderId())) {
            return "You are not the winner of auction #" + auctionId + ". " +
                "Only the highest bidder can process payment.";
        }

        // Generate a unique transaction ID from the current timestamp
        String transactionId = String.valueOf(System.currentTimeMillis());

        com.BidTech.auctionSystem.payment.model.Payment payment =
            new com.BidTech.auctionSystem.payment.model.Payment();
        payment.setAuctionId(auctionId);
        payment.setUserId(userId);
        payment.setAmount(amount);
        payment.setStatus("SUCCESS");
        payment.setTransactionId(transactionId);
        payment.setReceiptUrl("/receipt.html?paymentId=" + transactionId);
        paymentRepository.save(payment);

        return String.format(
            "Payment processed successfully! Transaction ID: %s — Amount: $%.2f. " +
            "Your receipt is available at /receipt.html",
            transactionId, amount);
    }
}
