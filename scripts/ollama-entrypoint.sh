#!/bin/bash
# ── ollama-entrypoint.sh ─────────────────────────────────────────────────────
# Custom entrypoint for the Ollama container.
# Starts the Ollama server, waits for it to be ready, then pulls the model
# specified by the OLLAMA_MODEL environment variable (from .env).
#
# The model is cached in the ollama-data Docker volume, so the pull only
# downloads on first run — subsequent starts skip the download.
# ─────────────────────────────────────────────────────────────────────────────

# Default model if OLLAMA_MODEL is not set in .env
MODEL="${OLLAMA_MODEL:-llama3.2}"

echo "🤖 Starting Ollama server..."

# Start the Ollama server in the background so we can pull the model
ollama serve &
SERVER_PID=$!

# Wait for the server to be ready before pulling
# Poll the /api/tags endpoint — returns 200 when the server is accepting requests
echo "⏳ Waiting for Ollama server to be ready..."
MAX_WAIT=60
ELAPSED=0
while [ $ELAPSED -lt $MAX_WAIT ]; do
    # Use ollama list as a health check — exit 0 means server is ready
    ollama list > /dev/null 2>&1 && break
    sleep 2
    ELAPSED=$((ELAPSED + 2))
    echo "   Still waiting... ($ELAPSED/$MAX_WAIT s)"
done

if [ $ELAPSED -ge $MAX_WAIT ]; then
    echo "❌ Ollama server did not start within ${MAX_WAIT}s"
    exit 1
fi

echo "✅ Ollama server is ready."

# Pull the model — skips download if already cached in the volume
echo "📦 Ensuring model '$MODEL' is available..."
ollama pull "$MODEL"

if [ $? -eq 0 ]; then
    echo "✅ Model '$MODEL' is ready."
else
    echo "⚠️ Failed to pull model '$MODEL'. Chatbot may be unavailable."
    # Non-fatal — the server still runs, ChatService returns a friendly error
fi

# Keep the server running in the foreground (wait for the background process)
echo "🚀 Ollama is serving on port 11434 with model '$MODEL'"
wait $SERVER_PID
