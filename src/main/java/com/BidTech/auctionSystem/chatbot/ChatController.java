package com.BidTech.auctionSystem.chatbot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ChatController — REST controller for the AI chatbot endpoint.
 *
 * <p>Exposes a single endpoint: {@code POST /api/chat}
 *
 * <p>This controller's only responsibility is HTTP handling — it receives the
 * request, delegates entirely to {@link ChatService}, and returns the response.
 * All business logic (intent resolution, data fetching, Ollama calls) lives in
 * ChatService, keeping this class thin and easy to test.
 *
 * <p>The endpoint is called by {@code chat-widget.js} on every user message.
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    /**
     * The service that orchestrates intent resolution, data fetching, and Ollama calls.
     * All business logic is delegated here — the controller only handles HTTP.
     */
    @Autowired
    private ChatService chatService;

    /**
     * Processes a chat message and returns the AI assistant's response.
     *
     * <p>Request body: {@link ChatRequest} JSON with message, history, userId,
     * and optional confirmation fields (confirmed, pendingIntent, pendingParams).
     *
     * <p>Response: plain text string — the assistant's response to display in the widget.
     * Returns 200 OK on success (including when Ollama is unavailable — the error
     * message is returned as the response body, not as an HTTP error code, so the
     * widget can display it gracefully without crashing).
     *
     * @param request the chat request from the browser widget
     * @return 200 OK with the assistant's response text
     */
    @PostMapping
    public ResponseEntity<String> chat(@RequestBody ChatRequest request) {
        // Delegate entirely to ChatService — controller does not contain any logic.
        // ChatService handles all error cases internally and always returns a string,
        // so we always return 200 OK. The widget checks the response content for errors.
        String response = chatService.processChat(request);
        return ResponseEntity.ok(response);
    }
}
