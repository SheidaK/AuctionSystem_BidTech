package com.BidTech.auctionSystem.chatbot;

import java.util.List;
import java.util.Map;

/**
 * ChatRequest — the request body sent from the chat widget to POST /api/chat.
 *
 * <p>This DTO carries everything ChatService needs to:
 * <ol>
 *   <li>Determine whether the user is authenticated (userId)</li>
 *   <li>Resolve the user's intent from the message text</li>
 *   <li>Reconstruct the conversation context for Ollama (history)</li>
 *   <li>Execute a previously confirmed write action (confirmed + pendingIntent + pendingParams)</li>
 * </ol>
 */
public class ChatRequest {

    /**
     * The user's current message text.
     * Example: "What should I bid to win auction 3?"
     */
    private String message;

    /**
     * The conversation history from the browser's localStorage.
     * Sent as context to Ollama so the model can refer to previous exchanges.
     * Limited to the last N messages (default 10) to stay within the context window.
     */
    private List<ChatMessage> history;

    /**
     * The ID of the authenticated user, or null for unauthenticated visitors.
     * When null, ChatService uses the visitor system prompt and withholds live data.
     * When non-null, ChatService resolves intent and injects live data into the prompt.
     */
    private Long userId;

    /**
     * Whether the user has confirmed a pending write action (bid or payment).
     * Set to true by the widget when the user clicks the "✅ Confirm" quick-reply button.
     * When true, ChatService executes the action stored in pendingIntent + pendingParams.
     */
    private boolean confirmed;

    /**
     * The intent name awaiting confirmation, e.g. "PLACE_BID" or "PROCESS_PAYMENT".
     * Stored in the browser's localStorage after the confirmation prompt is shown,
     * then sent back here when the user confirms.
     * Null when confirmed is false.
     */
    private String pendingIntent;

    /**
     * The parameters for the pending write action.
     * Example for PLACE_BID: { "auctionId": "3", "amount": "200.00" }
     * Stored alongside pendingIntent in localStorage and sent back on confirmation.
     * Null when confirmed is false.
     */
    private Map<String, String> pendingParams;

    /** Default no-arg constructor required for JSON deserialisation. */
    public ChatRequest() {}

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<ChatMessage> getHistory() { return history; }
    public void setHistory(List<ChatMessage> history) { this.history = history; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public boolean isConfirmed() { return confirmed; }
    public void setConfirmed(boolean confirmed) { this.confirmed = confirmed; }

    public String getPendingIntent() { return pendingIntent; }
    public void setPendingIntent(String pendingIntent) { this.pendingIntent = pendingIntent; }

    public Map<String, String> getPendingParams() { return pendingParams; }
    public void setPendingParams(Map<String, String> pendingParams) { this.pendingParams = pendingParams; }
}
