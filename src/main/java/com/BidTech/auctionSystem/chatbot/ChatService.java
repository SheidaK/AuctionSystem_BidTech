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
    @org.springframework.beans.factory.annotation.Value("${OLLAMA_MODEL:llama3.2}")
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

    /**
     * System prompt template for authenticated users.
     * Includes strict anti-hallucination rules and rephrase guidance.
     */
    private static final String AUTH_SYSTEM_PROMPT_TEMPLATE =
        "You are a helpful assistant for BidTech, an online auction platform. " +
        "The user is authenticated (userId: {userId}). " +
        "\n\nCRITICAL RULES — YOU MUST FOLLOW THESE:\n" +
        "1. ONLY use the live data provided below to answer questions about products, auctions, bids, and payments.\n" +
        "2. NEVER invent, fabricate, or hallucinate product names, prices, auction details, or any data.\n" +
        "3. If the live data below does not contain what the user is asking about, say: " +
        "\"Based on our current catalogue, I don't see that item. Here's what we currently have available:\" " +
        "and then list what IS in the data.\n" +
        "4. If you cannot answer the question with the data provided, suggest the user rephrase their question. " +
        "Offer specific rephrasing examples like:\n" +
        "   - \"Show me all products\" to see the full catalogue\n" +
        "   - \"What's the highest bid on auction 1?\" for auction details\n" +
        "   - \"How much time is left on auction 2?\" for remaining time\n" +
        "   - \"Bid $200 on auction 3\" to place a bid\n" +
        "   - \"What should I bid to win auction 1?\" for bid recommendations\n" +
        "\n\nAvailable actions you can perform on their behalf (with confirmation):\n" +
        "- Place a bid: say something like 'bid $X on auction Y'\n" +
        "- Process a payment: say something like 'pay for auction Y'\n" +
        "\nLive data retrieved for this query:\n{liveData}\n\n" +
        "Answer the user's question using ONLY the live data above. " +
        "If the data says there are no matching items, tell the user honestly — do not make up alternatives.";

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

    /** System prompt for keyword extraction — Ollama returns ONLY search keywords, nothing else */
    private static final String KEYWORD_EXTRACTION_PROMPT =
        "You are a keyword extractor for an auction search system. " +
        "The user is looking for products or auctions. " +
        "Your ONLY job is to extract 1-3 search keywords from the user's message. " +
        "Return ONLY the keywords separated by commas. No sentences, no explanations, no data. " +
        "Examples:\n" +
        "  User: 'any laptop in auction?' → laptop\n" +
        "  User: 'dell laptop under 1000' → dell,laptop\n" +
        "  User: 'show me watches' → watch\n" +
        "  User: 'I want a vintage rolex' → vintage,rolex\n" +
        "  User: 'electronics for sale' → electronics\n" +
        "  User: 'any cameras available?' → camera\n" +
        "  User: 'show me all products' → all\n" +
        "  User: 'what do you have?' → all\n" +
        "Return ONLY keywords. Nothing else.";

    /**
     * Processes a chat request. For auction search intents, uses Ollama ONLY to extract
     * search keywords, then queries the database and returns results directly.
     * Ollama never generates the user-facing response for search queries.
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
            // These intents already have params extracted by IntentResolver — no Ollama needed
            return fetchLiveData(intent, params, userId);
        }

        // ── Search intents → use Ollama ONLY for keyword extraction ──────────
        // Step 1: Ask Ollama to extract search keywords from the user's message.
        //         Ollama returns ONLY keywords like "laptop" or "dell,laptop" — no prose.
        String keywords = extractKeywordsViaOllama(request.getHistory(), message);

        // Step 2: Use the extracted keywords to query the catalogue database.
        //         If Ollama returned "all", fetch the full catalogue.
        //         Otherwise, search by each keyword and combine results.
        if (keywords == null || keywords.isBlank() || keywords.trim().equalsIgnoreCase("all")) {
            return "📦 Here are all available products:\n\n" + actionExecutor.fetchActiveProducts();
        }

        // Search with each keyword and collect results
        String[] keywordList = keywords.split(",");
        StringBuilder allResults = new StringBuilder();
        boolean foundAny = false;

        for (String kw : keywordList) {
            String trimmed = kw.trim().toLowerCase();
            if (trimmed.isEmpty()) continue;

            // Check if it's a category name first
            String[] categories = {"electronics", "jewelry", "art", "books", "other"};
            boolean isCategory = false;
            for (String cat : categories) {
                if (trimmed.equals(cat)) {
                    String catResult = actionExecutor.fetchProductsByCategory(
                        cat.substring(0, 1).toUpperCase() + cat.substring(1));
                    if (!catResult.contains("No products found")) {
                        allResults.append(catResult).append("\n");
                        foundAny = true;
                    }
                    isCategory = true;
                    break;
                }
            }

            // If not a category, do a keyword search by name/description
            if (!isCategory) {
                var results = actionExecutor.searchProducts(trimmed);
                if (!results.contains("No products found")) {
                    allResults.append(results).append("\n");
                    foundAny = true;
                }
            }
        }

        // Step 3: Return the database results directly — no Ollama in the response path
        if (foundAny) {
            return "🔍 Here are the results:\n\n" + allResults.toString().trim();
        } else {
            return "No auctions currently available matching your search. " +
                "Try asking \"show me all products\" to see the full catalogue.";
        }
    }

    /**
     * Calls Ollama with the keyword extraction prompt to get search keywords from the user's message.
     * Returns ONLY the keywords (e.g. "laptop" or "dell,laptop") — no prose.
     * Falls back to null if Ollama is unavailable.
     */
    private String extractKeywordsViaOllama(List<ChatMessage> history, String userMessage) {
        try {
            String raw = callOllama(KEYWORD_EXTRACTION_PROMPT, history, userMessage);
            if (raw == null) return null;
            // Clean up — Ollama might add quotes, periods, or extra whitespace
            return raw.replaceAll("[\"'.]", "").trim();
        } catch (Exception e) {
            // Ollama unavailable — fall back to null (will show full catalogue)
            return null;
        }
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
