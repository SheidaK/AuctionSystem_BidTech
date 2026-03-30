package com.BidTech.auctionSystem.chatbot;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

/**
 * IntentResolver — classifies a user's natural language message into an {@link Intent}.
 *
 * <p>Uses keyword and regex pattern matching to determine what the user wants to do.
 * This is intentionally simple (no ML) — the goal is to extract enough signal to
 * fetch the right live data before handing off to Ollama for the actual response.
 *
 * <p>Matching priority (highest to lowest):
 * <ol>
 *   <li>Write intents (PLACE_BID, PROCESS_PAYMENT) — checked first because they
 *       contain specific action verbs and require confirmation</li>
 *   <li>Bid recommendation — checked before general auction queries because it
 *       contains "bid" but is a read intent, not a write intent</li>
 *   <li>Specific read intents (auction, product, payment queries)</li>
 *   <li>GENERAL_QUESTION — fallback for anything not matched above</li>
 * </ol>
 *
 * <p>Extracted parameters (auction ID, amount, category) are stored in the
 * returned {@link ResolvedIntent} alongside the intent enum value.
 */
@Component
public class IntentResolver {

    // ── Regex patterns for extracting parameters from messages ────────────────

    /** Matches an auction ID in phrases like "auction 3", "auction #5", "auction number 2" */
    private static final Pattern AUCTION_ID_PATTERN =
        Pattern.compile("auction\\s*#?(\\d+)", Pattern.CASE_INSENSITIVE);

    /** Matches a dollar amount in phrases like "$200", "200 dollars", "200.00" */
    private static final Pattern AMOUNT_PATTERN =
        Pattern.compile("\\$?(\\d+(?:\\.\\d{1,2})?)", Pattern.CASE_INSENSITIVE);

    /** Matches a payment/transaction ID — long numeric string */
    private static final Pattern TRANSACTION_ID_PATTERN =
        Pattern.compile("\\b(\\d{10,})\\b");

    /** Matches a payment ID in phrases like "payment 1", "payment #3" */
    private static final Pattern PAYMENT_ID_PATTERN =
        Pattern.compile("payment\\s*#?(\\d+)", Pattern.CASE_INSENSITIVE);

    /**
     * Resolves a user message into a {@link ResolvedIntent} containing the intent
     * enum value and any extracted parameters (auction ID, amount, category, etc.).
     *
     * @param message the raw message text typed by the user
     * @param isAuthenticated true if the user is logged in (userId is non-null)
     * @return a {@link ResolvedIntent} with the classified intent and extracted params
     */
    public ResolvedIntent resolve(String message, boolean isAuthenticated) {
        // Normalise to lowercase for case-insensitive matching
        String lower = message.toLowerCase().trim();
        Map<String, String> params = new HashMap<>();

        // ── Write intents — checked first (highest priority) ──────────────────
        // These contain action verbs that clearly indicate the user wants to DO something.

        // PLACE_BID: "bid $200 on auction 3", "place a bid of 150 on auction 1", "I want to bid"
        if (lower.contains("bid") && !lower.contains("history") && !lower.contains("should")
                && !lower.contains("recommend") && !lower.contains("suggest")
                && !lower.contains("what") && !lower.contains("how much")) {

            // Only treat as a write intent if we can extract an auction ID or amount
            // — otherwise it's ambiguous (e.g. "bid" alone)
            Matcher auctionMatcher = AUCTION_ID_PATTERN.matcher(lower);
            Matcher amountMatcher  = AMOUNT_PATTERN.matcher(lower);

            if (auctionMatcher.find()) {
                params.put("auctionId", auctionMatcher.group(1));
                if (amountMatcher.find()) {
                    params.put("amount", amountMatcher.group(1));
                }
                // If we have an auction ID but no amount, mark ambiguous so we ask
                if (!params.containsKey("amount")) {
                    return new ResolvedIntent(Intent.AMBIGUOUS, params,
                        "How much would you like to bid on auction " + params.get("auctionId") + "?");
                }
                return new ResolvedIntent(Intent.PLACE_BID, params, null);
            }
            // "bid" mentioned but no auction ID — ask for clarification
            return new ResolvedIntent(Intent.AMBIGUOUS, params,
                "Which auction would you like to bid on, and how much?");
        }

        // PROCESS_PAYMENT: "pay for auction 1", "process payment", "complete purchase"
        if (lower.contains("pay") || lower.contains("purchase") || lower.contains("checkout")) {
            Matcher auctionMatcher = AUCTION_ID_PATTERN.matcher(lower);
            Matcher amountMatcher  = AMOUNT_PATTERN.matcher(lower);
            if (auctionMatcher.find()) {
                params.put("auctionId", auctionMatcher.group(1));
                if (amountMatcher.find()) {
                    params.put("amount", amountMatcher.group(1));
                }
                return new ResolvedIntent(Intent.PROCESS_PAYMENT, params, null);
            }
            // "pay" mentioned but no auction ID
            return new ResolvedIntent(Intent.AMBIGUOUS, params,
                "Which auction would you like to pay for?");
        }

        // ── Bid recommendation — read intent, checked before general auction queries ──
        // Phrases: "what should I bid", "how much to win", "recommend a bid", "bid suggestion"
        if ((lower.contains("should") && lower.contains("bid"))
                || lower.contains("recommend") && lower.contains("bid")
                || lower.contains("bid suggestion")
                || lower.contains("how much") && (lower.contains("win") || lower.contains("bid"))
                || lower.contains("what to bid")) {

            Matcher auctionMatcher = AUCTION_ID_PATTERN.matcher(lower);
            if (auctionMatcher.find()) {
                params.put("auctionId", auctionMatcher.group(1));
                return new ResolvedIntent(Intent.GET_BID_RECOMMENDATION, params, null);
            }
            // Recommendation requested but no auction ID specified
            return new ResolvedIntent(Intent.AMBIGUOUS, params,
                "Which auction would you like a bid recommendation for?");
        }

        // ── Specific read intents ─────────────────────────────────────────────

        // GET_BID_HISTORY: "bid history", "all bids", "who bid", "previous bids"
        if (lower.contains("bid history") || lower.contains("all bids")
                || lower.contains("who bid") || lower.contains("previous bids")
                || (lower.contains("bids") && lower.contains("auction"))) {
            Matcher m = AUCTION_ID_PATTERN.matcher(lower);
            if (m.find()) params.put("auctionId", m.group(1));
            return new ResolvedIntent(Intent.GET_BID_HISTORY, params, null);
        }

        // GET_HIGHEST_BID: "highest bid", "current bid", "top offer", "best bid"
        if (lower.contains("highest bid") || lower.contains("current bid")
                || lower.contains("top offer") || lower.contains("best bid")
                || lower.contains("highest offer")) {
            Matcher m = AUCTION_ID_PATTERN.matcher(lower);
            if (m.find()) params.put("auctionId", m.group(1));
            return new ResolvedIntent(Intent.GET_HIGHEST_BID, params, null);
        }

        // GET_REMAINING_TIME: "time left", "how long", "when does it end", "remaining time"
        if (lower.contains("time left") || lower.contains("how long")
                || lower.contains("when does it end") || lower.contains("remaining time")
                || lower.contains("time remaining") || lower.contains("ends")) {
            Matcher m = AUCTION_ID_PATTERN.matcher(lower);
            if (m.find()) params.put("auctionId", m.group(1));
            return new ResolvedIntent(Intent.GET_REMAINING_TIME, params, null);
        }

        // GET_AUCTION_STATUS: "auction status", "is auction X active", "tell me about auction X"
        if (lower.contains("auction status") || lower.contains("about auction")
                || (lower.contains("auction") && lower.contains("active"))
                || (lower.contains("auction") && lower.contains("status"))) {
            Matcher m = AUCTION_ID_PATTERN.matcher(lower);
            if (m.find()) params.put("auctionId", m.group(1));
            return new ResolvedIntent(Intent.GET_AUCTION_STATUS, params, null);
        }

        // GET_PRODUCT_BY_CATEGORY: "show electronics", "books for sale", "jewelry listings"
        // Check known categories explicitly
        String[] categories = {"electronics", "jewelry", "art", "books", "other"};
        for (String cat : categories) {
            if (lower.contains(cat)) {
                params.put("category", cat.substring(0, 1).toUpperCase() + cat.substring(1));
                return new ResolvedIntent(Intent.GET_PRODUCT_BY_CATEGORY, params, null);
            }
        }

        // ── SEARCH_PRODUCTS: keyword-based product search ───────────────────────
        // Triggered when the user mentions a specific product noun (laptop, watch, etc.)
        // Extracts the noun as the search keyword and queries the catalogue by name/description.
        // This MUST come before LIST_ACTIVE_PRODUCTS so "any laptop?" triggers a search,
        // not a full catalogue dump.
        String[] searchableNouns = {"laptop", "watch", "camera", "phone", "painting", "book",
            "computer", "tablet", "ring", "necklace", "dell", "canon", "rolex", "harry potter",
            "xps", "macbook", "iphone", "samsung", "vintage", "abstract"};
        for (String noun : searchableNouns) {
            if (lower.contains(noun)) {
                // Use the matched noun as the search keyword for the catalogue query
                params.put("keyword", noun);
                return new ResolvedIntent(Intent.SEARCH_PRODUCTS, params, null);
            }
        }

        // LIST_ACTIVE_PRODUCTS: broad matching for browsing the full catalogue.
        // Only triggers when no specific product noun was found above.
        if (lower.contains("product") || lower.contains("for sale") || lower.contains("active item")
                || lower.contains("browse") || lower.contains("catalogue") || lower.contains("catalog")
                || lower.contains("listing") || lower.contains("what's available")
                || lower.contains("available") || lower.contains("what can i")
                || lower.contains("show me") || lower.contains("find me")
                || lower.contains("search") || lower.contains("looking for")
                || lower.contains("in auction") || lower.contains("on auction")
                || lower.contains("for auction") || lower.contains("up for")
                || lower.contains("what do you have") || lower.contains("anything")
                || lower.contains("what's on") || lower.contains("inventory")
                || lower.contains("item") || lower.contains("thing") || lower.contains("stuff")
                || (lower.contains("what is") && lower.contains("sell"))) {
            return new ResolvedIntent(Intent.LIST_ACTIVE_PRODUCTS, params, null);
        }

        // GET_PAYMENT_STATUS: "payment status", "check payment", "transaction status"
        if (lower.contains("payment status") || lower.contains("check payment")
                || lower.contains("transaction status") || lower.contains("transaction id")) {
            Matcher m = TRANSACTION_ID_PATTERN.matcher(lower);
            if (m.find()) params.put("transactionId", m.group(1));
            return new ResolvedIntent(Intent.GET_PAYMENT_STATUS, params, null);
        }

        // GET_RECEIPT: "show receipt", "get receipt", "receipt for payment X"
        if (lower.contains("receipt")) {
            Matcher m = PAYMENT_ID_PATTERN.matcher(lower);
            if (m.find()) params.put("paymentId", m.group(1));
            return new ResolvedIntent(Intent.GET_RECEIPT, params, null);
        }

        // ── Fallback ──────────────────────────────────────────────────────────
        // No specific intent matched — treat as a general question about the platform.
        // Ollama will answer using only the system prompt (no live data injected).
        return new ResolvedIntent(Intent.GENERAL_QUESTION, params, null);
    }

    /**
     * ResolvedIntent — the result of intent classification.
     * Bundles the intent enum, extracted parameters, and an optional clarification
     * question to show the user when the intent is AMBIGUOUS.
     */
    public static class ResolvedIntent {

        /** The classified intent. */
        private final Intent intent;

        /**
         * Parameters extracted from the message text.
         * Keys depend on the intent: "auctionId", "amount", "category",
         * "transactionId", "paymentId".
         */
        private final Map<String, String> params;

        /**
         * Clarification question to show the user when intent is AMBIGUOUS.
         * Null for all other intents.
         */
        private final String clarificationQuestion;

        public ResolvedIntent(Intent intent, Map<String, String> params, String clarificationQuestion) {
            this.intent = intent;
            this.params = params;
            this.clarificationQuestion = clarificationQuestion;
        }

        public Intent getIntent() { return intent; }
        public Map<String, String> getParams() { return params; }
        public String getClarificationQuestion() { return clarificationQuestion; }
    }
}
