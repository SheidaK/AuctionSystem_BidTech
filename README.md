# 🏛️ BidTech Auction System

A full-stack auction platform built with **Spring Boot**, **SQLite**, **RabbitMQ**, and a multi-container **Docker** deployment. Features an AI chatbot powered by **Ollama (gemma3:1b)**, real-time notifications via pub/sub messaging, and a responsive **Tailwind CSS** interface.

---

## 🧱 Architecture

```
User Browser
     │
     ▼  http://localhost (port 80)
┌─────────────────────┐
│  Nginx Load Balancer│  ← only host-exposed port
└────────┬────────────┘
         │
    ┌────┴────┐
    ▼         ▼
┌────────┐  ┌──────────────┐
│  UI    │  │   Backend    │
│ Nginx  │  │ Spring Boot  │
│ :3000  │  │   :8080      │
└────────┘  └──────┬───────┘
                   ▼
     ┌──────────┬──────────┐
     │ SQLite   │ RabbitMQ │ Ollama │
     │ Docker   │  :5672   │ :11434 │
     │ Volume   │  :15672  │        │
     └──────────┴──────────┘
```

| Container | Role | Host Port |
|---|---|---|
| `bidtech-loadbalancer` | Nginx reverse proxy — single entry point | **80** |
| `bidtech-backend` | Spring Boot REST API | internal only |
| `bidtech-ui` | Nginx static file server | internal only |
| `bidtech-rabbitmq` | Message broker for notifications | **5672**, **15672** |
| `bidtech-ollama` | Local LLM for AI chatbot (gemma3:1b) | **11434** |

---

## 🚀 Quick Start — Docker Desktop

> **Prerequisites:** Docker Desktop running, Java 21, Maven

### First deploy (or after any code change)

```powershell
./scripts/deploy.ps1
```

This will:
1. Copy `.env.example` → `.env` if missing
2. Build the Spring Boot JAR via Maven
3. Build all Docker images (backend, UI, Ollama with auto-model-pull)
4. Start all 5 containers with health checks
5. Clean up unused Docker resources automatically

### Open the app

```
http://localhost
```

### RabbitMQ Management UI

```
http://localhost:15672
Username: guest | Password: guest
```

### Stop

```powershell
./scripts/stop.ps1
```

### Start again (no rebuild)

```powershell
./scripts/start.ps1
```

### View logs

```powershell
docker compose logs -f
```

### Full reset (⚠️ deletes all data)

```powershell
docker compose down -v
```

---

## 🗂️ Services

| Service | URL | Description |
|---|---|---|
| Catalogue | `http://localhost` | Browse and manage auction items |
| Auction | `http://localhost/auction.html` | Create auctions, place bids |
| Users (IAM) | `http://localhost/users.html` | Register and manage users |
| Payment | `http://localhost/pay.html` | Process payments, check status, receipts, history |
| Notifications | `http://localhost/notifications.html` | Real-time auction/payment event notifications |
| AI Chatbot | 💬 button (every page) | Search products and auctions via natural language |
| Login | `http://localhost/login.html` | User authentication |
| Register | `http://localhost/register.html` | New user registration |

---

## 🤖 AI Chatbot

A floating chat widget (💬) appears on every page. Powered by Ollama running the **gemma3:1b** model locally.

**What it can do:**
- Search products by keyword ("any laptops?", "show me electronics")
- List all active products or auctions
- Check auction status, highest bid, remaining time, bid history
- Get bid recommendations

**What it won't do:**
- Place bids, process payments, or modify data — those get a "let me connect you with a human agent" response

**How it works:**
- IntentResolver classifies the message using keyword matching
- For product searches, it queries the catalogue database directly (no LLM in the response path)
- Ollama is only used for visitor Q&A — search results come straight from the DB
- Session persists across pages with 5-minute idle timeout

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 4, Spring Data JPA, Hibernate |
| Database | SQLite (4 separate databases — IAM, Catalogue, Auction, Payment) |
| Frontend | HTML, Tailwind CSS, Vanilla JavaScript |
| Proxy | Nginx (load balancer + static file server) |
| Messaging | RabbitMQ (pub/sub notifications) |
| AI | Ollama (gemma3:1b) — local, no data leaves the machine |
| Containers | Docker, Docker Compose |
| Build | Maven (via `mvnw` wrapper) |
| Config | `.env` file for all credentials |

---

## ⚙️ Configuration

All credentials are in `.env` (gitignored). Copy `.env.example` on first setup:

```
RABBITMQ_DEFAULT_USER=guest
RABBITMQ_DEFAULT_PASS=guest
RABBITMQ_HOST=rabbitmq
RABBITMQ_PORT=5672
OLLAMA_HOST=ollama
OLLAMA_PORT=11434
OLLAMA_MODEL=gemma3:1b
```

---

## 📁 Project Structure

```
├── src/main/java/              # Spring Boot application
│   └── com/BidTech/auctionSystem/
│       ├── AuctionService/     # Auction & Bid domain
│       ├── CatalogueService/   # Product catalogue
│       ├── IAMService/         # User management
│       ├── payment/            # Payment processing
│       ├── chatbot/            # AI chatbot (Intent, ChatService, ActionExecutor)
│       └── config/             # Multi-database configuration
├── src/main/resources/static/  # Frontend HTML/JS/CSS
├── Dockerfile.backend          # Spring Boot image
├── Dockerfile.ui               # Nginx static file image
├── Dockerfile.ollama           # Ollama with auto-model-pull entrypoint
├── docker-compose.yml          # Full 5-container stack
├── nginx/
│   ├── loadbalancer.conf       # Reverse proxy routing rules
│   └── ui.conf                 # UI Nginx server config
├── scripts/
│   ├── deploy.ps1              # Build + deploy + cleanup
│   ├── start.ps1               # Start stopped containers
│   ├── stop.ps1                # Stop containers (data preserved)
│   └── ollama-entrypoint.sh    # Auto-pull model on container start
├── .env.example                # Credential template (committed)
└── .env                        # Actual credentials (gitignored)
```

---

## 🔌 API Endpoints

| Service | Method | Endpoint | Description |
|---|---|---|---|
| Catalogue | GET | `/api/catalogue/products` | List all products |
| Catalogue | POST | `/api/catalogue/products` | Create product |
| Catalogue | GET | `/api/catalogue/health` | Health check |
| Auction | POST | `/auction/create` | Create auction |
| Auction | POST | `/auction/{id}/bid` | Place bid |
| Auction | GET | `/auction/{id}/highest` | Get highest bid |
| Auction | POST | `/auction/{id}/end` | End auction |
| Users | GET | `/users` | List all users |
| Users | POST | `/users` | Register user |
| Users | PUT | `/users/reset-password/{name}` | Reset password |
| Payment | POST | `/api/payments/process` | Process payment |
| Payment | GET | `/api/payments/status/{txId}` | Check payment status |
| Payment | GET | `/api/payments/receipt/{id}` | Get receipt |
| Payment | GET | `/api/payments` | Payment history |
| Chatbot | POST | `/api/chat` | AI chatbot endpoint |
| Notifications | GET | `/api/notifications` | Get all notifications |
| Notifications | GET | `/api/notifications/count` | Get notification count |
