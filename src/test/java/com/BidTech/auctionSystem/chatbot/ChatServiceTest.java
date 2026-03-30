package com.BidTech.auctionSystem.chatbot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * ChatServiceTest — property-based tests for the ChatService orchestration logic.
 *
 * <p>These tests verify the correctness properties defined in the ai-chatbot requirements:
 * <ul>
 *   <li>Visitor requests never include live data in the system prompt</li>
 *   <li>Authenticated requests with auction keywords trigger the correct intent and data fetch</li>
 *   <li>GET_BID_RECOMMENDATION triggers all three data fetches</li>
 *   <li>Write intents with confirmed=false return a confirmation prompt, not an Ollama call</li>
 *   <li>Write intents with confirmed=true call the ActionExecutor write method</li>
 *   <li>Ollama connection errors return a friendly error string (no exception propagated)</li>
 * </ul>
 *
 * <p>Ollama is mocked — these tests verify ChatService logic, not the LLM itself.
 * ActionExecutor is also mocked to isolate ChatService from the database layer.
 */
@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private IntentResolver intentResolver;

    @Mock
    private ActionExecutor actionExecutor;

    @InjectMocks
    private ChatService chatService;

    private List<ChatMessage> emptyHistory;

    @BeforeEach
    void setUp() {
        emptyHistory = new ArrayList<>();
    }

    // ── Helper: build a ChatRequest ───────────────────────────────────────────

    private ChatRequest request(String message, Long userId) {
        ChatRequest req = new ChatRequest();
        req.setMessage(message);
        req.setHistory(emptyHistory);
        req.setUserId(userId);
        req.setConfirmed(false);
        return req;
    }

    private ChatRequest confirmedRequest(String pendingIntent, Map<String, String> params, Long userId) {
        ChatRequest req = new ChatRequest();
        req.setMessage("confirmed");
        req.setHistory(emptyHistory);
        req.setUserId(userId);
        req.setConfirmed(true);
        req.setPendingIntent(pendingIntent);
        req.setPendingParams(params);
        return req;
    }

    // ── Test 1: Visitor requests never include live data ──────────────────────

    /**
     * Property: A visitor (userId=null) asking about live data must receive a
     * login prompt, not actual product/auction/payment data.
     * Correctness property #1 from requirements.
     */
    @Test
    void visitorAskingAboutLiveData_shouldReceiveLoginPrompt_notLiveData() {
        // Arrange: IntentResolver classifies the message as a live data intent
        IntentResolver.ResolvedIntent resolved = new IntentResolver.ResolvedIntent(
            Intent.LIST_ACTIVE_PRODUCTS, Map.of(), null);
        when(intentResolver.resolve(anyString(), eq(false))).thenReturn(resolved);

        ChatRequest req = request("What products are active?", null); // null = visitor

        // Act
        String response = chatService.processChat(req);

        // Assert: response must mention logging in, not contain product data
        assertThat(response).containsIgnoringCase("log in");
        // ActionExecutor must never be called for a visitor asking about live data
        verifyNoInteractions(actionExecutor);
    }

    /**
     * Property: A visitor asking a general question (not live data) should get
     * a response without being prompted to log in.
     */
    @Test
    void visitorAskingGeneralQuestion_shouldNotRequireLogin() {
        // Arrange: IntentResolver classifies as a general question
        IntentResolver.ResolvedIntent resolved = new IntentResolver.ResolvedIntent(
            Intent.GENERAL_QUESTION, Map.of(), null);
        when(intentResolver.resolve(anyString(), eq(false))).thenReturn(resolved);

        ChatRequest req = request("How does bidding work?", null);

        // Act — Ollama will fail (not running in tests) but ChatService handles it gracefully
        String response = chatService.processChat(req);

        // Assert: response is not a login prompt — general questions are answered directly
        // (Ollama unavailable returns a friendly error, not a login prompt)
        assertThat(response).doesNotContain("log in");
        verifyNoInteractions(actionExecutor);
    }

    // ── Test 2: Authenticated requests trigger correct intent and data fetch ──

    /**
     * Property: An authenticated user asking about active products must trigger
     * a call to ActionExecutor.fetchActiveProducts().
     * Correctness property #2 from requirements.
     */
    @Test
    void authenticatedUser_askingAboutProducts_shouldFetchActiveProducts() {
        // Arrange
        IntentResolver.ResolvedIntent resolved = new IntentResolver.ResolvedIntent(
            Intent.LIST_ACTIVE_PRODUCTS, Map.of(), null);
        when(intentResolver.resolve(anyString(), eq(true))).thenReturn(resolved);
        when(actionExecutor.fetchActiveProducts()).thenReturn("Active products: ...");

        ChatRequest req = request("What products are active?", 3L);

        // Act
        chatService.processChat(req);

        // Assert: ActionExecutor.fetchActiveProducts() was called exactly once
        verify(actionExecutor, times(1)).fetchActiveProducts();
    }

    /**
     * Property: An authenticated user asking about a specific auction's highest bid
     * must trigger a call to ActionExecutor.fetchHighestBid() with the correct auction ID.
     */
    @Test
    void authenticatedUser_askingAboutHighestBid_shouldFetchHighestBid() {
        // Arrange
        IntentResolver.ResolvedIntent resolved = new IntentResolver.ResolvedIntent(
            Intent.GET_HIGHEST_BID, Map.of("auctionId", "1"), null);
        when(intentResolver.resolve(anyString(), eq(true))).thenReturn(resolved);
        when(actionExecutor.fetchHighestBid(1L)).thenReturn("Highest bid on auction #1 is $500.");

        ChatRequest req = request("What's the highest bid on auction 1?", 3L);

        // Act
        chatService.processChat(req);

        // Assert: fetchHighestBid called with the correct auction ID
        verify(actionExecutor, times(1)).fetchHighestBid(1L);
    }

    // ── Test 3: GET_BID_RECOMMENDATION triggers all three data fetches ────────

    /**
     * Property: A bid recommendation request must trigger fetchBidRecommendation()
     * which internally fetches highest bid, history, and remaining time.
     * Correctness property #11 from requirements.
     */
    @Test
    void bidRecommendationIntent_shouldCallFetchBidRecommendation() {
        // Arrange
        IntentResolver.ResolvedIntent resolved = new IntentResolver.ResolvedIntent(
            Intent.GET_BID_RECOMMENDATION, Map.of("auctionId", "3"), null);
        when(intentResolver.resolve(anyString(), eq(true))).thenReturn(resolved);
        when(actionExecutor.fetchBidRecommendation(3L))
            .thenReturn("Bid recommendation data for Auction #3: ...");

        ChatRequest req = request("What should I bid to win auction 3?", 3L);

        // Act
        chatService.processChat(req);

        // Assert: fetchBidRecommendation called with the correct auction ID
        verify(actionExecutor, times(1)).fetchBidRecommendation(3L);
        // No other fetch methods should be called — recommendation bundles all signals
        verify(actionExecutor, never()).fetchHighestBid(any());
        verify(actionExecutor, never()).fetchBidHistory(any());
        verify(actionExecutor, never()).fetchRemainingTime(any());
    }

    // ── Test 4: Write intent with confirmed=false returns confirmation prompt ─

    /**
     * Property: A PLACE_BID intent with confirmed=false must return a confirmation
     * prompt string, not call ActionExecutor.placeBid().
     * Correctness property #5 from requirements.
     */
    @Test
    void placeBidIntent_notConfirmed_shouldReturnConfirmationPrompt_notExecute() {
        // Arrange
        IntentResolver.ResolvedIntent resolved = new IntentResolver.ResolvedIntent(
            Intent.PLACE_BID, Map.of("auctionId", "3", "amount", "200"), null);
        when(intentResolver.resolve(anyString(), eq(true))).thenReturn(resolved);

        ChatRequest req = request("bid $200 on auction 3", 3L);
        req.setConfirmed(false); // Not yet confirmed

        // Act
        String response = chatService.processChat(req);

        // Assert: response contains the confirmation prefix — widget will show Confirm/Cancel
        assertThat(response).startsWith("CONFIRM_ACTION|PLACE_BID");
        // placeBid must NOT be called — action requires confirmation first
        verify(actionExecutor, never()).placeBid(any(), any(), anyDouble());
    }

    /**
     * Property: A PROCESS_PAYMENT intent with confirmed=false must return a
     * confirmation prompt, not execute the payment.
     */
    @Test
    void processPaymentIntent_notConfirmed_shouldReturnConfirmationPrompt() {
        // Arrange
        IntentResolver.ResolvedIntent resolved = new IntentResolver.ResolvedIntent(
            Intent.PROCESS_PAYMENT, Map.of("auctionId", "1", "amount", "500"), null);
        when(intentResolver.resolve(anyString(), eq(true))).thenReturn(resolved);

        ChatRequest req = request("pay for auction 1", 3L);

        // Act
        String response = chatService.processChat(req);

        // Assert
        assertThat(response).startsWith("CONFIRM_ACTION|PROCESS_PAYMENT");
        verify(actionExecutor, never()).processPayment(any(), any(), anyDouble());
    }

    // ── Test 5: Write intent with confirmed=true calls ActionExecutor ─────────

    /**
     * Property: A confirmed PLACE_BID request must call ActionExecutor.placeBid()
     * with the correct parameters.
     * Correctness property #6 from requirements.
     */
    @Test
    void placeBidIntent_confirmed_shouldCallPlaceBid() {
        // Arrange
        when(actionExecutor.placeBid(3L, 3L, 200.0))
            .thenReturn("Bid placed successfully! Bid ID: 7 — Amount: $200.00");

        ChatRequest req = confirmedRequest(
            "PLACE_BID",
            Map.of("auctionId", "3", "amount", "200.0"),
            3L);

        // Act
        chatService.processChat(req);

        // Assert: placeBid called with the correct auction ID, user ID, and amount
        verify(actionExecutor, times(1)).placeBid(3L, 3L, 200.0);
    }

    // ── Test 6: Ambiguous intent returns clarification question ───────────────

    /**
     * Property: An AMBIGUOUS intent must return the clarification question directly,
     * without calling Ollama or ActionExecutor.
     * Correctness property #9 from requirements.
     */
    @Test
    void ambiguousIntent_shouldReturnClarificationQuestion_notCallOllama() {
        // Arrange
        IntentResolver.ResolvedIntent resolved = new IntentResolver.ResolvedIntent(
            Intent.AMBIGUOUS, Map.of(), "Which auction would you like to bid on?");
        when(intentResolver.resolve(anyString(), eq(true))).thenReturn(resolved);

        ChatRequest req = request("bid on it", 3L);

        // Act
        String response = chatService.processChat(req);

        // Assert: response is the clarification question, not an Ollama response
        assertThat(response).isEqualTo("Which auction would you like to bid on?");
        verifyNoInteractions(actionExecutor);
    }

    // ── Test 7: Ollama connection error returns friendly error string ─────────

    /**
     * Property: If Ollama is unavailable (connection refused), ChatService must
     * return a friendly error string — never propagate the exception to the controller.
     * Correctness property #4 from requirements.
     *
     * <p>This test verifies the error handling by using a real ChatService instance
     * where Ollama is not running (the default test environment).
     */
    @Test
    void ollamaUnavailable_shouldReturnFriendlyErrorString_notThrow() {
        // Arrange: visitor asking a general question — will try to call Ollama
        IntentResolver.ResolvedIntent resolved = new IntentResolver.ResolvedIntent(
            Intent.GENERAL_QUESTION, Map.of(), null);
        when(intentResolver.resolve(anyString(), eq(false))).thenReturn(resolved);

        ChatRequest req = request("How does bidding work?", null);

        // Act — Ollama is not running in the test environment, so RestTemplate will throw
        String response = chatService.processChat(req);

        // Assert: response is a friendly error message, not an exception
        assertThat(response).isNotNull();
        assertThat(response).isNotEmpty();
        // The response should mention "unavailable" or "try again" — not a stack trace
        assertThat(response).satisfiesAnyOf(
            r -> assertThat(r).containsIgnoringCase("unavailable"),
            r -> assertThat(r).containsIgnoringCase("try again"),
            r -> assertThat(r).containsIgnoringCase("wrong")
        );
    }
}
