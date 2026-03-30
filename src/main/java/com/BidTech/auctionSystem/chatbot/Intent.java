package com.BidTech.auctionSystem.chatbot;

/**
 * Intent — all supported natural language intents for the BidTech chatbot.
 *
 * <p>Each enum value represents a distinct user goal that the chatbot can handle.
 * Read intents are safe to execute immediately; write intents require a confirmation
 * round-trip before any action is taken.
 *
 * <p>The {@link IntentResolver} classifies a user message into one of these intents
 * using keyword and pattern matching. The {@link ChatService} then routes the request
 * to the appropriate {@link ActionExecutor} method based on the resolved intent.
 */
public enum Intent {

    // ── Read intents — safe, no confirmation needed ───────────────────────────
    // These intents only fetch data; they never modify any state.

    /** Trigger phrases: "show products", "what's for sale", "active items", "browse catalogue" */
    LIST_ACTIVE_PRODUCTS,

    /** Trigger phrases: "show electronics", "books for sale", "jewelry listings", "category X" */
    GET_PRODUCT_BY_CATEGORY,

    /** Trigger phrases: "auction status", "is auction X active", "tell me about auction X" */
    GET_AUCTION_STATUS,

    /** Trigger phrases: "highest bid", "current bid", "top offer", "best bid on auction X" */
    GET_HIGHEST_BID,

    /** Trigger phrases: "time left", "how long", "when does it end", "remaining time on auction X" */
    GET_REMAINING_TIME,

    /** Trigger phrases: "bid history", "all bids", "who bid", "previous bids on auction X" */
    GET_BID_HISTORY,

    /**
     * Trigger phrases: "what should I bid", "how much to win", "bid suggestion",
     * "recommend a bid", "what do I need to bid to win auction X"
     * Fetches highest bid + full history + remaining time, computes signals,
     * and asks Ollama to reason about a competitive bid amount.
     */
    GET_BID_RECOMMENDATION,

    /** Trigger phrases: "payment status", "check payment", "transaction status for X" */
    GET_PAYMENT_STATUS,

    /** Trigger phrases: "show receipt", "get receipt", "receipt for payment X" */
    GET_RECEIPT,

    // ── Write intents — require explicit user confirmation before execution ────
    // These intents modify state (place a bid or process a payment).
    // ChatService returns a confirmation prompt when these are detected with confirmed=false.

    /**
     * Trigger phrases: "bid $X on auction Y", "place a bid", "offer $X", "I want to bid"
     * Requires params: auctionId, amount (and userId from the request).
     */
    PLACE_BID,

    /**
     * Trigger phrases: "pay for auction X", "process payment", "complete purchase", "pay now"
     * Requires params: auctionId, amount (and userId from the request).
     */
    PROCESS_PAYMENT,

    // ── Fallback intents ──────────────────────────────────────────────────────

    /** Any question about how the platform works that doesn't require live data. */
    GENERAL_QUESTION,

    /**
     * Visitor (unauthenticated) asking about live data.
     * ChatService returns a prompt to log in rather than fetching any data.
     */
    NEEDS_LOGIN,

    /**
     * Intent could not be determined with sufficient confidence.
     * ChatService returns a clarification question to the user without calling Ollama.
     * Example: "bid on it" with no auction ID mentioned.
     */
    AMBIGUOUS
}
