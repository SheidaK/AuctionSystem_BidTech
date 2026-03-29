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

    /** Model name to use — must be pulled via 'ollama pull llama3.2' in deploy.ps1 */
    private static final String MODEL = "llama3.2";

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
     * No live data is included — visitors can only ask general questions.
     */
    private static final String VISITOR_SYSTEM_PROMPT =
        "You are a helpful assistant for BidTech, an online auction platform. " +
        "You can answer general questions about how the platform works: " +
        "how to register, how bidding works, how payments are processed, " +
        "how long auctions last, and similar topics. " +
        "If the user asks about specific live data (auctions, bids, products, payments), " +
        "tell them they need to log in to access that information. " +
        "Keep responses concise and friendly.";

    /**
     * System prompt template for authenticated users.
     * The {liveData} placeholder is replaced with fetched data before calling Ollama.
     */
    private static final String AUTH_SYSTEM_PROMPT_TEMPLATE =
        "You are a helpful assistant for BidTech, an online auction platform. " +
        "The user is authenticated (userId: {userId}). " +
        "You can help the user interact with the platform using natural language. " +
        "Available actions you can perform on their behalf (with confirmation): " +
        "- Place a bid: say something like 'bid $X on auction Y' " +
        "- Process a payment: say something like 'pay for auction Y' " +
        "Live data retrieved for this query:\n{liveData}\n" +
        "Answer the user's question using only the data above. Do not invent data. " +
        "After performing an action, explain what you did in plain English.";

    /**
     * Processes a chat request and returns the assistant's response string.
     *
     * @param request the incoming chat request from the browser widget
     * @return the assistant's response text, or a friendly error message if Ollama is unavailable
     */
    public String processChat(ChatRequest request) {
        String message = request.getMessage();
        Long userId = request.getUserId();
        boolean isAuthenticated = userId != null;

        // ── Visitor mode ──────────────────────────────────────────────────────
        // Unauthenticated users get the general system prompt with no live data.
        // We still call Ollama so the model can answer general platform questions.
        if (!isAuthenticated) {
            // Check if the visitor is asking about live data — if so, prompt to log in
            IntentResolver.ResolvedIntent resolved = intentResolver.resolve(message, false);
            if (isLiveDataIntent(resolved.getIntent())) {
                // Return a canned response without calling Ollama — no need to waste a model call
                return "To access live auction data, products, and payments, " +
                    "please log in first. You can register at /users.html.";
            }
            return callOllama(VISITOR_SYSTEM_PROMPT, request.getHistory(), message);
        }

        // ── Authenticated mode — confirmed write action ───────────────────────
        // The user previously saw a confirmation prompt and clicked "Confirm".
        // Execute the pending write action and report the result.
        if (request.isConfirmed() && request.getPendingIntent() != null) {
            return executeConfirmedAction(request);
        }

        // ── Authenticated mode — resolve intent ───────────────────────────────
        IntentResolver.ResolvedIntent resolved = intentResolver.resolve(message, true);
        Intent intent = resolved.getIntent();
        Map<String, String> params = resolved.getParams();

        // ── AMBIGUOUS: ask for clarification without calling Ollama ───────────
        // When the intent is unclear, return the clarification question directly.
        // This avoids wasting an Ollama call on an unanswerable question.
        if (intent == Intent.AMBIGUOUS) {
            return resolved.getClarificationQuestion();
        }

        // ── Write intents: return confirmation prompt ─────────────────────────
        // We never execute write actions without explicit user confirmation.
        // Return a confirmation message; the widget will show Confirm/Cancel buttons.
        if (intent == Intent.PLACE_BID) {
            String auctionId = params.getOrDefault("auctionId", "?");
            String amount    = params.getOrDefault("amount", "?");
            return String.format(
                "CONFIRM_ACTION|PLACE_BID|%s|%s|" +
                "I can place a bid of $%s on auction #%s for you. " +
                "Please confirm to proceed.",
                auctionId, amount, amount, auctionId);
        }
        if (intent == Intent.PROCESS_PAYMENT) {
            String auctionId = params.getOrDefault("auctionId", "?");
            String amount    = params.getOrDefault("amount", "?");
            return String.format(
                "CONFIRM_ACTION|PROCESS_PAYMENT|%s|%s|" +
                "I can process a payment of $%s for auction #%s. " +
                "Please confirm to proceed.",
                auctionId, amount, amount, auctionId);
        }

        // ── Read intents: fetch live data and inject into prompt ──────────────
        String liveData = fetchLiveData(intent, params, userId);
        String systemPrompt = AUTH_SYSTEM_PROMPT_TEMPLATE
            .replace("{userId}", userId.toString())
            .replace("{liveData}", liveData);

        return callOllama(systemPrompt, request.getHistory(), message);
    }

    /**
     * Executes a previously confirmed write action (PLACE_BID or PROCESS_PAYMENT).
     * Called when the request has confirmed=true and a pendingIntent set.
     *
     * @param request the confirmed request with pendingIntent and pendingParams
     * @return a plain-English result string from ActionExecutor, then passed to Ollama
     */
    private String executeConfirmedAction(ChatRequest request) {
        String pendingIntent = request.getPendingIntent();
        Map<String, String> params = request.getPendingParams();
        Long userId = request.getUserId();

        String actionResult;

        if ("PLACE_BID".equals(pendingIntent)) {
            // Extract auctionId and amount from the stored pending params
            Long auctionId = Long.parseLong(params.getOrDefault("auctionId", "0"));
            double amount  = Double.parseDouble(params.getOrDefault("amount", "0"));
            actionResult = actionExecutor.placeBid(auctionId, userId, amount);

        } else if ("PROCESS_PAYMENT".equals(pendingIntent)) {
            Long auctionId = Long.parseLong(params.getOrDefault("auctionId", "0"));
            double amount  = Double.parseDouble(params.getOrDefault("amount", "0"));
            actionResult = actionExecutor.processPayment(auctionId, userId, amount);

        } else {
            // Unknown pending intent — should not happen in normal flow
            return "I couldn't find the action to confirm. Please try again.";
        }

        // Inject the action result into the system prompt so Ollama can explain
        // what happened in a natural, conversational way
        String systemPrompt = AUTH_SYSTEM_PROMPT_TEMPLATE
            .replace("{userId}", userId.toString())
            .replace("{liveData}", "Action result: " + actionResult);

        return callOllama(systemPrompt, request.getHistory(), request.getMessage());
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
                // GENERAL_QUESTION — no live data needed, Ollama answers from system prompt
                return "No specific live data required for this question.";
        }
    }

    /**
     * Returns true if the intent requires live data (and therefore needs authentication).
     * Used to detect visitors asking about live data so we can prompt them to log in.
     *
     * @param intent the resolved intent
     * @return true if the intent requires authenticated live data access
     */
    private boolean isLiveDataIntent(Intent intent) {
        switch (intent) {
            case LIST_ACTIVE_PRODUCTS:
            case GET_PRODUCT_BY_CATEGORY:
            case GET_AUCTION_STATUS:
            case GET_HIGHEST_BID:
            case GET_REMAINING_TIME:
            case GET_BID_HISTORY:
            case GET_BID_RECOMMENDATION:
            case GET_PAYMENT_STATUS:
            case GET_RECEIPT:
            case PLACE_BID:
            case PROCESS_PAYMENT:
                return true;
            default:
                return false;
        }
    }

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
                "model", MODEL,
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
