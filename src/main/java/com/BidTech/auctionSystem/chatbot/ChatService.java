package com.BidTech.auctionSystem.chatbot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * ChatService — orchestrates the full chatbot request/response cycle.
 *
 * <p>For each incoming {@link ChatRequest}, this service:
 * <ol>
 *   <li>Determines whether the user is authenticated (visitor vs logged-in mode)</li>
 *   <li>Resolves the user's intent via {@link IntentResolver}</li>
 *   <li>Fetches relevant live data via {@link ActionExecutor} (read intents)</li>
 *   <li>Executes write actions after confirmation (PLACE_BID, PROCESS_PAYMENT)</li>
 *   <li>Builds the Ollama request with the system prompt + conversation history</li>
 *   <li>Calls the Ollama REST API and returns the assistant's response</li>
 * </ol>
 *
 * <p>Ollama is called at {@code http://ollama:11434/api/chat} — the hostname "ollama"
 * is resolved by Docker's internal DNS within the bidtech-network bridge network.
 */
@Service
public class ChatService {

    /** Ollama API endpoint — uses Docker service name, not localhost */
    private static final String OLLAMA_URL = "http://ollama:11434/api/chat";

    /** Model name to use — read from OLLAMA_MODEL env var, defaults to llama3.2 */
    @org.springframework.beans.factory.annotation.Value("${OLLAMA_MODEL:gemma3:1b}")
    private String model;

    @Autowired
    private IntentResolver intentResolver;

    @Autowired
    private ActionExecutor actionExecutor;

    // RestTemplate is used for synchronous HTTP calls to the Ollama API.
    // A new instance is created here rather than injected because we don't need
    // any special configuration (timeouts, interceptors) for this use case.
    private final RestTemplate restTemplate = new RestTemplate();

    // ── System Prompt Templates ───────────────────────────────────────────────

    /**
     * System prompt for unauthenticated visitors.
     * Strict anti-hallucination rules — no live data, no made-up products.
     */
    private static final String VISITOR_SYSTEM_PROMPT =
        "You are a helpful assistant for BidTech, an online auction platform. " +
        "You can answer general questions about how the platform works: " +
        "how to register, how bidding works, how payments are processed, " +
        "how long auctions last, and similar topics. " +
        "If the user asks about specific live data (auctions, bids, products, payments), " +
        "tell them they need to log in to access that information. " +
        "CRITICAL RULE: NEVER make up product names, prices, auction details, or any data. " +
        "If you don't have the information, say so honestly. " +
        "Keep responses concise and friendly.";

    /** Human agent handoff message — returned for any non-auction-search request */
    private static final String HUMAN_AGENT_MSG =
        "I can only help with searching auctions and products right now. " +
        "For anything else, let me connect you with a human agent who can assist you. 🧑‍💼\n\n" +
        "You can try asking me things like:\n" +
        "• \"Show me all products\"\n" +
        "• \"Any laptops in auction?\"\n" +
        "• \"Show me electronics\"\n" +
        "• \"What's the highest bid on auction 1?\"\n" +
        "• \"How much time is left on auction 2?\"";

    /**
     * Processes a chat request. All search intents use IntentResolver keywords
     * and query the database directly — no Ollama involved in search responses.
     */
    public String processChat(ChatRequest request) {
        String message = request.getMessage();
        Long userId = request.getUserId();
        boolean isAuthenticated = userId != null;

        // ── Visitor mode — prompt to log in for search, general Q&A via Ollama ──
        if (!isAuthenticated) {
            IntentResolver.ResolvedIntent resolved = intentResolver.resolve(message, false);
            if (isAuctionSearchIntent(resolved.getIntent())) {
                return "To search auctions and products, please log in first. " +
                    "You can register at the Users page. 🔐";
            }
            return callOllama(VISITOR_SYSTEM_PROMPT, request.getHistory(), message);
        }

        // ── Authenticated mode — resolve intent ──
        IntentResolver.ResolvedIntent resolved = intentResolver.resolve(message, true);
        Intent intent = resolved.getIntent();
        Map<String, String> params = resolved.getParams();

        // ── Non-search intents → human agent ──
        if (!isAuctionSearchIntent(intent)) {
            return HUMAN_AGENT_MSG;
        }

        // ── AMBIGUOUS → ask for clarification ──
        if (intent == Intent.AMBIGUOUS) {
            return resolved.getClarificationQuestion();
        }

        // ── Auction-specific intents (status, highest bid, etc.) → direct DB result ──
        if (intent != Intent.SEARCH_PRODUCTS && intent != Intent.LIST_ACTIVE_PRODUCTS
                && intent != Intent.GET_PRODUCT_BY_CATEGORY) {
            return fetchLiveData(intent, params, userId);
        }

        // ── SEARCH_PRODUCTS → use IntentResolver's keyword directly, no Ollama ──
        // IntentResolver already extracted the keyword (e.g. "laptop") from the message.
        // We query the DB directly — Ollama is not involved in the search response.
        if (intent == Intent.SEARCH_PRODUCTS) {
            String keyword = params.getOrDefault("keyword", "");
            if (keyword.isEmpty()) {
                return "📦 Here are all available products:\n\n" + actionExecutor.fetchActiveProducts();
            }
            String results = actionExecutor.searchProducts(keyword);
            // Use startsWith for consistent checking — ActionExecutor always starts with "No products found"
            if (results.startsWith("No products found")) {
                return "No auctions currently available matching '" + keyword + "'. " +
                    "Try asking \"show me all products\" to see the full catalogue.";
            }
            return "🔍 Here are the results:\n\n" + results;
        }

        // ── LIST_ACTIVE_PRODUCTS / GET_PRODUCT_BY_CATEGORY → direct DB result ──
        return "📦 " + fetchLiveData(intent, params, userId);
    }

    /**
     * Returns true if the intent is an auction/product search intent that the chatbot handles.
     * All other intents (payments, write actions, general questions) get the human agent message.
     */
    private boolean isAuctionSearchIntent(Intent intent) {
        switch (intent) {
            case LIST_ACTIVE_PRODUCTS:
            case SEARCH_PRODUCTS:
            case GET_PRODUCT_BY_CATEGORY:
            case GET_AUCTION_STATUS:
            case LIST_ACTIVE_AUCTIONS:
            case GET_HIGHEST_BID:
            case GET_REMAINING_TIME:
            case GET_BID_HISTORY:
            case GET_BID_RECOMMENDATION:
            case AMBIGUOUS:  // Ambiguous within auction context — we'll ask for clarification
                return true;
            default:
                return false;
        }
    }

    /**
     * Fetches live data for a read intent and returns a summary string for prompt injection.
     *
     * @param intent the resolved read intent
     * @param params extracted parameters (auctionId, category, etc.)
     * @param userId the authenticated user's ID
     * @return a plain-English data summary for injection into the Ollama system prompt
     */
    private String fetchLiveData(Intent intent, Map<String, String> params, Long userId) {
        switch (intent) {
            case LIST_ACTIVE_PRODUCTS:
                return actionExecutor.fetchActiveProducts();

            case SEARCH_PRODUCTS:
                // Keyword-based search — uses the catalogue's JPQL search query
                // to find products matching the extracted keyword by name or description
                return actionExecutor.searchProducts(
                    params.getOrDefault("keyword", ""));

            case GET_PRODUCT_BY_CATEGORY:
                return actionExecutor.fetchProductsByCategory(
                    params.getOrDefault("category", "Other"));

            case GET_AUCTION_STATUS:
                return actionExecutor.fetchAuctionStatus(
                    Long.parseLong(params.getOrDefault("auctionId", "0")));

            case LIST_ACTIVE_AUCTIONS:
                return actionExecutor.fetchActiveAuctions();

            case GET_HIGHEST_BID:
                return actionExecutor.fetchHighestBid(
                    Long.parseLong(params.getOrDefault("auctionId", "0")));

            case GET_REMAINING_TIME:
                return actionExecutor.fetchRemainingTime(
                    Long.parseLong(params.getOrDefault("auctionId", "0")));

            case GET_BID_HISTORY:
                return actionExecutor.fetchBidHistory(
                    Long.parseLong(params.getOrDefault("auctionId", "0")));

            case GET_BID_RECOMMENDATION:
                // This fetches highest bid + history + remaining time and computes
                // statistical signals (increment, velocity, expected bids) for Ollama
                return actionExecutor.fetchBidRecommendation(
                    Long.parseLong(params.getOrDefault("auctionId", "0")));

            case GET_PAYMENT_STATUS:
                return actionExecutor.fetchPaymentStatus(
                    params.getOrDefault("transactionId", ""));

            case GET_RECEIPT:
                return actionExecutor.fetchReceipt(
                    Long.parseLong(params.getOrDefault("paymentId", "0")));

            default:
                // GENERAL_QUESTION — still inject active products so Ollama has real data
                // to reference instead of hallucinating. This is the key anti-hallucination fix:
                // even for general questions, the model sees what's actually in the catalogue.
                return "Current catalogue snapshot (use this data, do not invent products):\n" +
                    actionExecutor.fetchActiveProducts();
        }
    }

    /**
     * Returns true if the intent requires live data (and therefore needs authentication).
     * Used to detect visitors asking about live data so we can prompt them to log in.
     *
     * @param intent the resolved intent
     * @return true if the intent requires authenticated live data access
     */
    /**
     * Calls the Ollama REST API with the given system prompt, conversation history,
     * and current user message.
     *
     * <p>The full message list sent to Ollama is:
     * <ol>
     *   <li>System message (injected prompt with live data)</li>
     *   <li>Previous conversation history (for context)</li>
     *   <li>Current user message</li>
     * </ol>
     *
     * <p>Uses {@code stream: false} so we wait for the complete response before returning.
     * The Ollama hostname is "ollama" — resolved by Docker's internal DNS.
     *
     * @param systemPrompt the system prompt with injected live data
     * @param history      previous conversation messages from localStorage
     * @param userMessage  the current user message
     * @return the assistant's response text, or a friendly error if Ollama is unavailable
     */
    @SuppressWarnings("unchecked")
    private String callOllama(String systemPrompt, List<ChatMessage> history, String userMessage) {
        try {
            // Build the messages array: system + history + current user message
            List<Map<String, String>> messages = new ArrayList<>();

            // System message — injected first so it sets the context for all subsequent messages
            messages.add(Map.of("role", "system", "content", systemPrompt));

            // Conversation history — provides context so Ollama can refer to previous exchanges
            if (history != null) {
                history.forEach(h -> messages.add(Map.of("role", h.getRole(), "content", h.getContent())));
            }

            // Current user message — the question being answered in this turn
            messages.add(Map.of("role", "user", "content", userMessage));

            // Build the Ollama request body
            Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", messages,
                "stream", false   // Wait for the full response — streaming requires SSE handling
            );

            // POST to Ollama — hostname "ollama" is resolved by Docker's internal DNS
            Map<String, Object> response = restTemplate.postForObject(
                OLLAMA_URL, requestBody, Map.class);

            if (response == null) return "I received an empty response. Please try again.";

            // Extract the assistant's message content from the Ollama response structure:
            // { "message": { "role": "assistant", "content": "..." } }
            Map<String, Object> messageObj = (Map<String, Object>) response.get("message");
            if (messageObj == null) return "I couldn't parse the response. Please try again.";

            return (String) messageObj.get("content");

        } catch (RestClientException e) {
            // Ollama is not running or unreachable — return a friendly error.
            // This is non-fatal: the rest of the application continues to work.
            return "The AI assistant is currently unavailable. " +
                "Please make sure Ollama is running and try again later.";
        } catch (Exception e) {
            // Unexpected error — log and return a safe message
            return "Something went wrong with the AI assistant. Please try again.";
        }
    }
}
