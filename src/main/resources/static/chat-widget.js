/**
 * chat-widget.js — BidTech AI Chatbot Widget
 *
 * Provides a floating chat panel on every page that lets users interact with
 * the BidTech platform using natural language.
 *
 * Features:
 * - Floating toggle button (bottom-right corner)
 * - Scrollable chat bubble panel (user right, assistant left)
 * - Typing indicator while waiting for Ollama response
 * - Confirmation quick-reply buttons for write actions (bid, payment)
 * - localStorage persistence of conversation history across page navigation
 * - Clear chat button
 * - Enter key support
 *
 * All API calls use relative URLs so they route through the Nginx load balancer.
 * The load balancer proxies /api/chat to the Spring Boot backend on port 8080.
 */

// ── Constants ─────────────────────────────────────────────────────────────────

/** localStorage key for conversation history */
const STORAGE_KEY_HISTORY = 'bidtech_chat_history';

/** localStorage key for the authenticated user's ID */
const STORAGE_KEY_USER_ID = 'bidtech_chat_userId';

/** localStorage key for a pending write action awaiting confirmation */
const STORAGE_KEY_PENDING = 'bidtech_chat_pending';

/** Maximum number of history messages to send to Ollama per request.
 *  Keeps the context window manageable — older messages are dropped. */
const MAX_HISTORY = 10;

/** Prefix in the response string that signals a write action needs confirmation.
 *  Format: CONFIRM_ACTION|INTENT|param1|param2|display message */
const CONFIRM_PREFIX = 'CONFIRM_ACTION|';

// ── Widget HTML injection ─────────────────────────────────────────────────────

/**
 * Injects the chat widget HTML into the page body.
 * Called once on DOMContentLoaded — adds the toggle button and panel.
 */
function injectWidget() {
    const html = `
    <!-- ── Chat Toggle Button ── floating in bottom-right corner ── -->
    <button id="chat-toggle-btn" title="Open BidTech Assistant"
        style="position:fixed;bottom:24px;right:24px;z-index:9999;
               width:56px;height:56px;border-radius:50%;border:none;cursor:pointer;
               background:linear-gradient(135deg,#667eea,#764ba2);
               color:white;font-size:24px;box-shadow:0 4px 16px rgba(102,126,234,0.5);
               transition:transform 0.2s;">
        💬
    </button>

    <!-- ── Chat Panel ── slides up when toggle is clicked ── -->
    <div id="chat-panel"
        style="display:none;position:fixed;bottom:92px;right:24px;z-index:9998;
               width:360px;max-height:520px;border-radius:16px;overflow:hidden;
               box-shadow:0 8px 32px rgba(0,0,0,0.25);
               display:flex;flex-direction:column;font-family:'Segoe UI',sans-serif;">

        <!-- Header -->
        <div style="background:linear-gradient(135deg,#4a3f8f,#5a2d82);
                    color:white;padding:14px 16px;display:flex;
                    justify-content:space-between;align-items:center;">
            <span style="font-weight:700;font-size:15px;">🤖 BidTech Assistant</span>
            <div style="display:flex;gap:8px;">
                <button id="chat-clear-btn"
                    style="background:rgba(255,255,255,0.15);border:none;color:white;
                           padding:4px 10px;border-radius:6px;cursor:pointer;font-size:12px;">
                    Clear
                </button>
                <button id="chat-close-btn"
                    style="background:none;border:none;color:white;
                           font-size:20px;cursor:pointer;line-height:1;">
                    ✕
                </button>
            </div>
        </div>

        <!-- Messages area -->
        <div id="chat-messages"
            style="flex:1;overflow-y:auto;padding:16px;background:#f8f9fa;
                   display:flex;flex-direction:column;gap:10px;min-height:200px;max-height:340px;">
        </div>

        <!-- Typing indicator — shown while waiting for Ollama response -->
        <div id="chat-typing"
            style="display:none;padding:8px 16px;background:#f8f9fa;">
            <div style="background:white;border-radius:12px;padding:10px 14px;
                        display:inline-flex;gap:4px;box-shadow:0 1px 4px rgba(0,0,0,0.1);">
                <span class="dot" style="width:8px;height:8px;border-radius:50%;
                    background:#667eea;animation:bounce 1.2s infinite;"></span>
                <span class="dot" style="width:8px;height:8px;border-radius:50%;
                    background:#667eea;animation:bounce 1.2s infinite 0.2s;"></span>
                <span class="dot" style="width:8px;height:8px;border-radius:50%;
                    background:#667eea;animation:bounce 1.2s infinite 0.4s;"></span>
            </div>
        </div>

        <!-- Input area -->
        <div style="padding:12px;background:white;border-top:1px solid #e9ecef;
                    display:flex;gap:8px;">
            <input id="chat-input" type="text" placeholder="Ask me anything..."
                style="flex:1;padding:10px 14px;border:2px solid #dee2e6;border-radius:10px;
                       font-size:14px;outline:none;font-family:inherit;"
                onfocus="this.style.borderColor='#667eea'"
                onblur="this.style.borderColor='#dee2e6'">
            <button id="chat-send-btn"
                style="padding:10px 16px;background:linear-gradient(135deg,#667eea,#764ba2);
                       color:white;border:none;border-radius:10px;cursor:pointer;
                       font-size:14px;font-weight:600;">
                Send
            </button>
        </div>
    </div>

    <!-- Bounce animation for typing dots -->
    <style>
        @keyframes bounce {
            0%, 60%, 100% { transform: translateY(0); }
            30% { transform: translateY(-6px); }
        }
        #chat-toggle-btn:hover { transform: scale(1.1); }
    </style>`;

    // Inject into body
    const container = document.createElement('div');
    container.innerHTML = html;
    document.body.appendChild(container);
}

// ── State ─────────────────────────────────────────────────────────────────────

/** Whether the chat panel is currently open */
let panelOpen = false;

// ── Initialisation ────────────────────────────────────────────────────────────

document.addEventListener('DOMContentLoaded', () => {
    injectWidget();
    bindEvents();
    loadHistoryFromStorage(); // Restore conversation from previous page navigation
});

/**
 * Binds all event listeners to the widget elements.
 * Called once after the widget HTML is injected into the DOM.
 */
function bindEvents() {
    // Toggle button — opens/closes the panel
    document.getElementById('chat-toggle-btn').addEventListener('click', togglePanel);

    // Close button — closes the panel
    document.getElementById('chat-close-btn').addEventListener('click', closePanel);

    // Clear button — removes all messages from UI and localStorage
    document.getElementById('chat-clear-btn').addEventListener('click', clearChat);

    // Send button — sends the current input message
    document.getElementById('chat-send-btn').addEventListener('click', sendMessage);

    // Enter key support — pressing Enter in the input sends the message
    document.getElementById('chat-input').addEventListener('keydown', e => {
        if (e.key === 'Enter') sendMessage();
    });
}

// ── Panel open/close ──────────────────────────────────────────────────────────

/** Toggles the chat panel open or closed. */
function togglePanel() {
    panelOpen ? closePanel() : openPanel();
}

/** Opens the chat panel. */
function openPanel() {
    document.getElementById('chat-panel').style.display = 'flex';
    panelOpen = true;
    // Scroll to the bottom so the latest message is visible
    scrollToBottom();
}

/** Closes the chat panel without clearing history. */
function closePanel() {
    document.getElementById('chat-panel').style.display = 'none';
    panelOpen = false;
}

// ── Message rendering ─────────────────────────────────────────────────────────

/**
 * Appends a message bubble to the chat messages area.
 *
 * @param {string} text    - The message text to display
 * @param {string} role    - 'user' (right-aligned blue) or 'assistant' (left-aligned grey)
 * @param {boolean} isHtml - If true, render text as HTML (for confirmation buttons)
 */
function appendMessage(text, role, isHtml = false) {
    const container = document.getElementById('chat-messages');
    const wrapper = document.createElement('div');

    // User messages are right-aligned with brand colour; assistant messages are left grey
    const isUser = role === 'user';
    wrapper.style.cssText = `display:flex;justify-content:${isUser ? 'flex-end' : 'flex-start'};`;

    const bubble = document.createElement('div');
    bubble.style.cssText = `
        max-width:80%;padding:10px 14px;border-radius:${isUser ? '16px 16px 4px 16px' : '16px 16px 16px 4px'};
        font-size:14px;line-height:1.5;
        background:${isUser ? 'linear-gradient(135deg,#667eea,#764ba2)' : 'white'};
        color:${isUser ? 'white' : '#333'};
        box-shadow:0 1px 4px rgba(0,0,0,0.1);`;

    if (isHtml) {
        bubble.innerHTML = text;
    } else {
        bubble.textContent = text;
    }

    wrapper.appendChild(bubble);
    container.appendChild(wrapper);
    scrollToBottom();
}

/** Scrolls the messages area to the bottom so the latest message is visible. */
function scrollToBottom() {
    const container = document.getElementById('chat-messages');
    container.scrollTop = container.scrollHeight;
}

// ── Typing indicator ──────────────────────────────────────────────────────────

/** Shows the animated typing indicator while waiting for Ollama's response. */
function showTyping() {
    document.getElementById('chat-typing').style.display = 'block';
    scrollToBottom();
}

/** Hides the typing indicator after the response arrives. */
function hideTyping() {
    document.getElementById('chat-typing').style.display = 'none';
}

// ── Send message ──────────────────────────────────────────────────────────────

/**
 * Reads the input field, appends the user message, and sends it to the backend.
 * Shows the typing indicator immediately (within ~0ms) for responsiveness.
 * @returns {Promise<void>}
 */
async function sendMessage() {
    const input = document.getElementById('chat-input');
    const text = input.value.trim();
    if (!text) return;

    input.value = '';
    appendMessage(text, 'user');

    // Show typing indicator immediately — target is within 200ms of user sending
    showTyping();

    // Read userId from localStorage — set by the login flow when a user logs in
    const userId = localStorage.getItem(STORAGE_KEY_USER_ID);
    const history = getHistory();

    try {
        // All API calls use relative URLs — routed through the Nginx load balancer.
        // The load balancer proxies /api/chat to the Spring Boot backend on port 8080.
        const response = await fetch('/api/chat', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                message: text,
                history: history.slice(-MAX_HISTORY), // Send only the last N messages
                userId: userId ? parseInt(userId) : null,
                confirmed: false,
                pendingIntent: null,
                pendingParams: null
            })
        });

        hideTyping();

        if (!response.ok) {
            appendMessage('Sorry, something went wrong. Please try again.', 'assistant');
            return;
        }

        const assistantText = await response.text();
        handleResponse(assistantText, text);

    } catch (err) {
        hideTyping();
        appendMessage('Could not reach the assistant. Please check your connection.', 'assistant');
    }
}

/**
 * Handles the response from the backend.
 * If the response starts with CONFIRM_ACTION|, shows confirmation buttons.
 * Otherwise, displays the response as a normal assistant message.
 *
 * @param {string} responseText - The raw response string from the backend
 * @param {string} userMessage  - The original user message (for history)
 */
function handleResponse(responseText, userMessage) {
    if (responseText.startsWith(CONFIRM_PREFIX)) {
        // Parse the confirmation response format:
        // CONFIRM_ACTION|INTENT|param1|param2|display message
        const parts = responseText.split('|');
        const intent  = parts[1];
        const param1  = parts[2]; // auctionId
        const param2  = parts[3]; // amount
        const display = parts.slice(4).join('|'); // The human-readable message

        // Store the pending action in localStorage so it survives the confirmation round-trip
        localStorage.setItem(STORAGE_KEY_PENDING, JSON.stringify({
            intent, params: { auctionId: param1, amount: param2 }
        }));

        // Show the confirmation message with Confirm/Cancel quick-reply buttons
        const confirmHtml = `
            <div>${display}</div>
            <div style="display:flex;gap:8px;margin-top:10px;">
                <button onclick="confirmAction()"
                    style="padding:6px 14px;background:#28a745;color:white;border:none;
                           border-radius:8px;cursor:pointer;font-size:13px;font-weight:600;">
                    ✅ Confirm
                </button>
                <button onclick="cancelAction()"
                    style="padding:6px 14px;background:#dc3545;color:white;border:none;
                           border-radius:8px;cursor:pointer;font-size:13px;font-weight:600;">
                    ❌ Cancel
                </button>
            </div>`;
        appendMessage(confirmHtml, 'assistant', true);

    } else {
        // Normal response — display as assistant bubble and save to history
        appendMessage(responseText, 'assistant');
        saveToHistory(userMessage, responseText);
    }
}

// ── Confirmation flow ─────────────────────────────────────────────────────────

/**
 * Called when the user clicks the ✅ Confirm button.
 * Resends the request with confirmed=true and the stored pending action.
 * @returns {Promise<void>}
 */
async function confirmAction() {
    const pendingRaw = localStorage.getItem(STORAGE_KEY_PENDING);
    if (!pendingRaw) return;

    const pending = JSON.parse(pendingRaw);
    localStorage.removeItem(STORAGE_KEY_PENDING); // Clear pending after use

    appendMessage('✅ Confirmed', 'user');
    showTyping();

    const userId = localStorage.getItem(STORAGE_KEY_USER_ID);

    try {
        const response = await fetch('/api/chat', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                message: 'confirmed',
                history: getHistory().slice(-MAX_HISTORY),
                userId: userId ? parseInt(userId) : null,
                confirmed: true,                    // Signal to ChatService to execute the action
                pendingIntent: pending.intent,      // The action to execute (PLACE_BID, etc.)
                pendingParams: pending.params       // The parameters for the action
            })
        });

        hideTyping();
        const text = await response.text();
        appendMessage(text, 'assistant');
        saveToHistory('confirmed', text);

    } catch (err) {
        hideTyping();
        appendMessage('Could not complete the action. Please try again.', 'assistant');
    }
}

/**
 * Called when the user clicks the ❌ Cancel button.
 * Clears the pending action and shows a cancellation message.
 */
function cancelAction() {
    localStorage.removeItem(STORAGE_KEY_PENDING);
    appendMessage('❌ Cancelled', 'user');
    appendMessage('No problem — the action was cancelled. Is there anything else I can help with?', 'assistant');
}

// ── localStorage persistence ──────────────────────────────────────────────────

/**
 * Returns the current conversation history from localStorage.
 * @returns {Array} Array of {role, content} message objects
 */
function getHistory() {
    const raw = localStorage.getItem(STORAGE_KEY_HISTORY);
    return raw ? JSON.parse(raw) : [];
}

/**
 * Saves a user/assistant exchange to localStorage so it persists across page navigation.
 * @param {string} userMessage      - The user's message text
 * @param {string} assistantMessage - The assistant's response text
 */
function saveToHistory(userMessage, assistantMessage) {
    const history = getHistory();
    history.push({ role: 'user',      content: userMessage });
    history.push({ role: 'assistant', content: assistantMessage });

    // Keep only the last MAX_HISTORY * 2 entries (pairs of user+assistant)
    // to prevent localStorage from growing unbounded
    const trimmed = history.slice(-(MAX_HISTORY * 2));
    localStorage.setItem(STORAGE_KEY_HISTORY, JSON.stringify(trimmed));
}

/**
 * Loads conversation history from localStorage and renders it in the chat panel.
 * Called on page load so the conversation persists across page navigation.
 */
function loadHistoryFromStorage() {
    const history = getHistory();
    history.forEach(msg => {
        // Only render user and assistant messages — skip system messages
        if (msg.role === 'user' || msg.role === 'assistant') {
            appendMessage(msg.content, msg.role);
        }
    });
}

/**
 * Clears all messages from the UI and removes conversation history from localStorage.
 * Also clears any pending confirmation action.
 */
function clearChat() {
    document.getElementById('chat-messages').innerHTML = '';
    localStorage.removeItem(STORAGE_KEY_HISTORY);
    localStorage.removeItem(STORAGE_KEY_PENDING);
    appendMessage('Chat cleared. How can I help you?', 'assistant');
}
