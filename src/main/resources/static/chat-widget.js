/**
 * chat-widget.js — BidTech AI Chatbot Widget
 *
 * Features:
 * - Floating toggle button (bottom-right corner)
 * - Personalized greeting using logged-in user name or "Dear Guest"
 * - Scrollable chat bubble panel (user right, assistant left)
 * - Typing indicator while waiting for Ollama response
 * - Confirmation quick-reply buttons for write actions (bid, payment)
 * - localStorage persistence of conversation history across page navigation
 * - Idle timeout: auto-ends chat session after IDLE_TIMEOUT_SECONDS of inactivity
 * - Manual "End Chat" button to explicitly close the session
 * - Clear chat / Enter key support
 *
 * All API calls use relative URLs so they route through the Nginx load balancer.
 */

// ── Constants ─────────────────────────────────────────────────────────────────

/** localStorage key for conversation history */
const STORAGE_KEY_HISTORY = 'bidtech_chat_history';

/** localStorage key for the authenticated user's ID */
const STORAGE_KEY_USER_ID = 'bidtech_chat_userId';

/** localStorage key for a pending write action awaiting confirmation */
const STORAGE_KEY_PENDING = 'bidtech_chat_pending';

/** localStorage key tracking whether a chat session is active.
 *  When 'true', messages persist across pages. When absent, session has ended. */
const STORAGE_KEY_SESSION = 'bidtech_chat_session';

/** Maximum number of history messages to send to Ollama per request. */
const MAX_HISTORY = 10;

/** Seconds of inactivity before the chat session auto-ends.
 *  Resets on every user message or panel interaction. */
const IDLE_TIMEOUT_SECONDS = 300; // 5 minutes

/** Prefix in the response string that signals a write action needs confirmation. */
const CONFIRM_PREFIX = 'CONFIRM_ACTION|';

// ── Widget HTML injection ─────────────────────────────────────────────────────

/**
 * Injects the chat widget HTML into the page body.
 * Called once on DOMContentLoaded.
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

    <!-- ── Chat Panel ── hidden by default, shown only when toggle is clicked ── -->
    <div id="chat-panel"
        style="display:none;position:fixed;bottom:92px;right:24px;z-index:9998;
               width:360px;max-height:520px;border-radius:16px;overflow:hidden;
               box-shadow:0 8px 32px rgba(0,0,0,0.25);
               flex-direction:column;font-family:'Segoe UI',sans-serif;">

        <!-- Header with End Chat button -->
        <div style="background:linear-gradient(135deg,#4a3f8f,#5a2d82);
                    color:white;padding:14px 16px;display:flex;
                    justify-content:space-between;align-items:center;">
            <span style="font-weight:700;font-size:15px;">🤖 BidTech Assistant</span>
            <div style="display:flex;gap:6px;">
                <button id="chat-end-btn" title="End chat session and clear history"
                    style="background:rgba(220,53,69,0.8);border:none;color:white;
                           padding:4px 10px;border-radius:6px;cursor:pointer;font-size:12px;">
                    End Chat
                </button>
                <button id="chat-close-btn" title="Minimize (keeps session)"
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

        <!-- Typing indicator -->
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

    <style>
        @keyframes bounce {
            0%, 60%, 100% { transform: translateY(0); }
            30% { transform: translateY(-6px); }
        }
        #chat-toggle-btn:hover { transform: scale(1.1); }
    </style>`;

    const container = document.createElement('div');
    container.innerHTML = html;
    document.body.appendChild(container);
}

// ── State ─────────────────────────────────────────────────────────────────────

/** Whether the chat panel is currently visible */
let panelOpen = false;

/** Timer ID for the idle timeout — reset on every user interaction */
let idleTimer = null;

// ── Initialisation ────────────────────────────────────────────────────────────

document.addEventListener('DOMContentLoaded', () => {
    injectWidget();
    bindEvents();

    // If there's an active session with history, restore it across page navigation
    if (isSessionActive()) {
        loadHistoryFromStorage();
        resetIdleTimer(); // Keep the idle timer running for the restored session
    }
});

/**
 * Binds all event listeners to the widget elements.
 */
function bindEvents() {
    document.getElementById('chat-toggle-btn').addEventListener('click', togglePanel);
    document.getElementById('chat-close-btn').addEventListener('click', closePanel);
    document.getElementById('chat-send-btn').addEventListener('click', sendMessage);

    // "End Chat" button — explicitly ends the session and clears all history
    document.getElementById('chat-end-btn').addEventListener('click', endChatSession);

    // Enter key sends the message
    document.getElementById('chat-input').addEventListener('keydown', e => {
        if (e.key === 'Enter') sendMessage();
    });
}

// ── Session management ────────────────────────────────────────────────────────

/**
 * Returns true if a chat session is currently active.
 * A session is active from the first message until the user ends it
 * (manually or via idle timeout).
 * @returns {boolean}
 */
function isSessionActive() {
    return localStorage.getItem(STORAGE_KEY_SESSION) === 'true';
}

/**
 * Starts a new chat session — called on the first user message.
 * Sets the session flag and shows the personalized greeting.
 */
function startSession() {
    localStorage.setItem(STORAGE_KEY_SESSION, 'true');
    showGreeting();
    resetIdleTimer();
}

/**
 * Ends the chat session — clears all history, pending actions, and the session flag.
 * Called by the "End Chat" button or by the idle timeout.
 * @param {boolean} isIdle - true if ended by idle timeout (shows different message)
 */
function endChatSession(isIdle = false) {
    // Clear the idle timer so it doesn't fire again
    clearIdleTimer();

    // Show a farewell message before clearing
    const name = getUserDisplayName();
    const farewell = isIdle === true
        ? `Chat session ended due to inactivity. See you next time, ${name}! 👋`
        : `Chat session ended. See you next time, ${name}! 👋`;

    appendMessage(farewell, 'assistant');

    // Wait a moment so the user can read the farewell, then clear everything
    setTimeout(() => {
        document.getElementById('chat-messages').innerHTML = '';
        localStorage.removeItem(STORAGE_KEY_HISTORY);
        localStorage.removeItem(STORAGE_KEY_PENDING);
        localStorage.removeItem(STORAGE_KEY_SESSION);
        closePanel();
    }, 2000); // 2 second delay so the farewell is visible
}

// ── Idle timeout ──────────────────────────────────────────────────────────────

/**
 * Resets the idle timer. Called on every user interaction (message sent,
 * panel opened, confirmation clicked). If no interaction happens within
 * IDLE_TIMEOUT_SECONDS, the session auto-ends.
 */
function resetIdleTimer() {
    clearIdleTimer();
    // Only set the timer if a session is active — no timer when idle with no session
    if (isSessionActive()) {
        idleTimer = setTimeout(() => {
            // Auto-end the session due to inactivity
            if (isSessionActive()) {
                endChatSession(true); // true = idle timeout
            }
        }, IDLE_TIMEOUT_SECONDS * 1000);
    }
}

/** Clears the idle timer if one is running. */
function clearIdleTimer() {
    if (idleTimer) {
        clearTimeout(idleTimer);
        idleTimer = null;
    }
}

// ── Greeting ──────────────────────────────────────────────────────────────────

/**
 * Returns the display name for the current user.
 * Reads from the 'user' key in localStorage (set by the login page).
 * Falls back to "Dear Guest" for unauthenticated visitors.
 * @returns {string}
 */
function getUserDisplayName() {
    try {
        // The login page stores the full user object as JSON under 'user'
        const userRaw = localStorage.getItem('user');
        if (userRaw) {
            const user = JSON.parse(userRaw);
            // Use firstName if available, otherwise userName
            if (user.firstName) return user.firstName;
            if (user.userName) return user.userName;
        }
    } catch (e) {
        // JSON parse failed — fall through to guest
    }
    return 'Dear Guest';
}

/**
 * Shows a personalized greeting as the first message in a new session.
 * Uses the logged-in user's name or "Dear Guest" for visitors.
 */
function showGreeting() {
    const name = getUserDisplayName();
    const isLoggedIn = localStorage.getItem('user') !== null;

    let greeting;
    if (isLoggedIn) {
        greeting = `Hi ${name}! 👋 Welcome back to BidTech. I can help you search products and auctions — ` +
            `just ask me things like "show me all products", "any laptops?", or "what's the highest bid on auction 1?". ` +
            `For anything else, I'll connect you with a human agent.`;
    } else {
        greeting = `Hello, ${name}! 👋 Welcome to BidTech. I can answer general questions about ` +
            `how the platform works. Log in to search live auctions and products. ` +
            `How can I help you?`;
    }

    appendMessage(greeting, 'assistant');
}

// ── Panel open/close ──────────────────────────────────────────────────────────

/** Toggles the chat panel open or closed. */
function togglePanel() {
    panelOpen ? closePanel() : openPanel();
}

/**
 * Opens the chat panel. If no session is active, starts a new one with a greeting.
 * Resets the idle timer on open.
 */
function openPanel() {
    document.getElementById('chat-panel').style.display = 'flex';
    panelOpen = true;

    // Start a new session with greeting if none is active
    if (!isSessionActive()) {
        startSession();
    }

    resetIdleTimer(); // Opening the panel counts as user activity
    scrollToBottom();
}

/** Closes (minimizes) the chat panel without ending the session. */
function closePanel() {
    document.getElementById('chat-panel').style.display = 'none';
    panelOpen = false;
}

// ── Message rendering ─────────────────────────────────────────────────────────

/**
 * Appends a message bubble to the chat messages area.
 * @param {string} text    - The message text to display
 * @param {string} role    - 'user' (right-aligned blue) or 'assistant' (left-aligned grey)
 * @param {boolean} isHtml - If true, render text as HTML (for confirmation buttons)
 */
function appendMessage(text, role, isHtml = false) {
    const container = document.getElementById('chat-messages');
    const wrapper = document.createElement('div');

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

/** Scrolls the messages area to the bottom. */
function scrollToBottom() {
    const container = document.getElementById('chat-messages');
    container.scrollTop = container.scrollHeight;
}

// ── Typing indicator ──────────────────────────────────────────────────────────

function showTyping() {
    document.getElementById('chat-typing').style.display = 'block';
    scrollToBottom();
}

function hideTyping() {
    document.getElementById('chat-typing').style.display = 'none';
}

// ── Send message ──────────────────────────────────────────────────────────────

/**
 * Reads the input, appends the user message, sends to backend, handles response.
 * Resets the idle timer on every send.
 * @returns {Promise<void>}
 */
async function sendMessage() {
    const input = document.getElementById('chat-input');
    const text = input.value.trim();
    if (!text) return;

    // Start a session if one isn't active (e.g. user types before opening panel)
    if (!isSessionActive()) startSession();

    // Reset idle timer — user is actively chatting
    resetIdleTimer();

    input.value = '';
    appendMessage(text, 'user');
    showTyping();

    // Read userId from the 'user' object stored by the login page
    let userId = null;
    try {
        const userRaw = localStorage.getItem('user');
        if (userRaw) {
            const user = JSON.parse(userRaw);
            userId = user.id || null;
        }
    } catch (e) { /* not logged in */ }

    const history = getHistory();

    try {
        const response = await fetch('/api/chat', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                message: text,
                history: history.slice(-MAX_HISTORY),
                userId: userId,
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
 * Handles the backend response — confirmation flow or normal message.
 * @param {string} responseText - Raw response from backend
 * @param {string} userMessage  - Original user message (for history)
 */
function handleResponse(responseText, userMessage) {
    if (responseText.startsWith(CONFIRM_PREFIX)) {
        const parts = responseText.split('|');
        const intent  = parts[1];
        const param1  = parts[2];
        const param2  = parts[3];
        const display = parts.slice(4).join('|');

        localStorage.setItem(STORAGE_KEY_PENDING, JSON.stringify({
            intent, params: { auctionId: param1, amount: param2 }
        }));

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
        appendMessage(responseText, 'assistant');
        saveToHistory(userMessage, responseText);
    }
}

// ── Confirmation flow ─────────────────────────────────────────────────────────

/**
 * Executes a confirmed write action. Resets idle timer.
 * @returns {Promise<void>}
 */
async function confirmAction() {
    const pendingRaw = localStorage.getItem(STORAGE_KEY_PENDING);
    if (!pendingRaw) return;

    const pending = JSON.parse(pendingRaw);
    localStorage.removeItem(STORAGE_KEY_PENDING);

    resetIdleTimer(); // Confirmation counts as user activity

    appendMessage('✅ Confirmed', 'user');
    showTyping();

    let userId = null;
    try {
        const userRaw = localStorage.getItem('user');
        if (userRaw) userId = JSON.parse(userRaw).id || null;
    } catch (e) {}

    try {
        const response = await fetch('/api/chat', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                message: 'confirmed',
                history: getHistory().slice(-MAX_HISTORY),
                userId: userId,
                confirmed: true,
                pendingIntent: pending.intent,
                pendingParams: pending.params
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

/** Cancels a pending write action. */
function cancelAction() {
    localStorage.removeItem(STORAGE_KEY_PENDING);
    resetIdleTimer();
    appendMessage('❌ Cancelled', 'user');
    appendMessage('No problem — the action was cancelled. Anything else?', 'assistant');
}

// ── localStorage persistence ──────────────────────────────────────────────────

/**
 * Returns the current conversation history from localStorage.
 * Only returns history if a session is active.
 * @returns {Array}
 */
function getHistory() {
    if (!isSessionActive()) return [];
    const raw = localStorage.getItem(STORAGE_KEY_HISTORY);
    return raw ? JSON.parse(raw) : [];
}

/**
 * Saves a user/assistant exchange to localStorage.
 * Messages persist across page navigation as long as the session is active.
 * @param {string} userMessage
 * @param {string} assistantMessage
 */
function saveToHistory(userMessage, assistantMessage) {
    const history = getHistory();
    history.push({ role: 'user',      content: userMessage });
    history.push({ role: 'assistant', content: assistantMessage });
    const trimmed = history.slice(-(MAX_HISTORY * 2));
    localStorage.setItem(STORAGE_KEY_HISTORY, JSON.stringify(trimmed));
}

/**
 * Loads conversation history from localStorage and renders it.
 * Called on page load when a session is active — preserves chat across pages.
 */
function loadHistoryFromStorage() {
    const history = getHistory();
    history.forEach(msg => {
        if (msg.role === 'user' || msg.role === 'assistant') {
            appendMessage(msg.content, msg.role);
        }
    });
}
