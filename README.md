<p align="center">
  <img src="https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=openjdk" />
  <img src="https://img.shields.io/badge/Spring%20Boot-4.0-brightgreen?style=flat-square&logo=springboot" />
  <img src="https://img.shields.io/badge/Docker-Compose-blue?style=flat-square&logo=docker" />
  <img src="https://img.shields.io/badge/Tailwind%20CSS-3.x-38bdf8?style=flat-square&logo=tailwindcss" />
  <img src="https://img.shields.io/badge/Ollama-gemma3%3A1b-purple?style=flat-square" />
  <img src="https://img.shields.io/badge/RabbitMQ-3--management-ff6600?style=flat-square&logo=rabbitmq" />
</p>

<h1 align="center">🏛️ BidTech Auction System</h1>

<p align="center">
  A full-stack auction platform with an AI chatbot, real-time notifications,<br/>
  and a multi-container Docker deployment — all running locally.
</p>

---

## Architecture

```
                        http://localhost
                              │
                    ┌─────────▼──────────┐
                    │   Load Balancer    │  ← only exposed port (80)
                    │   nginx:alpine     │
                    └────┬──────────┬────┘
                         │          │
              ┌──────────▼──┐  ┌───▼───────────┐
              │     UI      │  │    Backend     │
              │ nginx:3000  │  │ Spring Boot    │
              │ static HTML │  │    :8080       │
              └─────────────┘  └──┬─────┬───┬──┘
                                  │     │   │
                    ┌─────────────▼┐  ┌─▼───▼──────┐
                    │   RabbitMQ   │  │   Ollama    │
                    │  :5672/15672 │  │   :11434    │
                    └──────────────┘  └─────────────┘
                                  │
                         ┌────────▼────────┐
                         │  SQLite (×4)    │
                         │  Docker Volume  │
                         └─────────────────┘
```

| Container | Role | Exposed Port |
|:--|:--|:--:|
| `bidtech-loadbalancer` | Nginx reverse proxy — single entry point | `80` |
| `bidtech-backend` | Spring Boot REST API | — |
| `bidtech-ui` | Nginx static file server | — |
| `bidtech-rabbitmq` | Message broker (notifications) | `5672` `15672` |
| `bidtech-ollama` | Local LLM runtime (gemma3:1b) | `11434` |

---

## Quick Start

> **Prerequisites** — Docker Desktop running, Java 21, Maven

```powershell
# 1. Deploy everything (build JAR → build images → start containers)
./scripts/deploy.ps1

# 2. Open the app
http://localhost

# 3. Stop containers (data preserved)
./scripts/stop.ps1

# 4. Start again without rebuilding
./scripts/start.ps1
```


The deploy script handles everything automatically:
- Copies `.env.example` → `.env` if missing
- Builds the Spring Boot JAR
- Builds Docker images (backend, UI, Ollama with auto-model-pull)
- Starts all 5 containers with health checks
- Cleans up unused Docker resources

> ⚠️ **Full reset** (deletes all data): `docker compose down -v`

---

## Pages

| Page | URL | Description |
|:--|:--|:--|
| 📦 Catalogue | [`/`](http://localhost) | Browse products, add new items, create auctions |
| 🔨 Auction | [`/auction.html`](http://localhost/auction.html) | View active auctions, place bids, end auctions |
| 👤 Users | [`/users.html`](http://localhost/users.html) | Register, update, delete users, reset passwords |
| 💳 Payment | [`/pay.html`](http://localhost/pay.html) | Pay for won auctions, expedited shipping |
| 🧾 Receipt | [`/receipt.html`](http://localhost/receipt.html) | View payment receipts |
| 🔔 Notifications | [`/notifications.html`](http://localhost/notifications.html) | Real-time event feed (admin only) |
| 🔐 Login | [`/login.html`](http://localhost/login.html) | User authentication |
| 📝 Register | [`/register.html`](http://localhost/register.html) | New user registration |
| 💬 AI Chatbot | Floating button on every page | Natural language product & auction search |

> All pages require authentication — unauthenticated users are redirected to the login page.

---

## AI Chatbot

A floating chat widget (💬) appears on every page, powered by **Ollama** running the `gemma3:1b` model locally.

**Can do:**
- Search products by keyword — *"any laptops?"*, *"show me electronics"*
- List active products or auctions
- Check auction status, highest bid, remaining time, bid history
- Get bid recommendations with statistical analysis

**Won't do:**
- Place bids, process payments, or modify any data
- Those requests get a friendly *"let me connect you with a human agent"* response

**How it works:**
- `IntentResolver` classifies messages using keyword/regex matching (no ML)
- Product searches query the catalogue database directly — Ollama is not in the search response path
- Ollama is only used for visitor general Q&A
- Chat session persists across pages via localStorage with a 5-minute idle timeout

---

## Tech Stack

| Layer | Technology |
|:--|:--|
| Backend | Java 21 · Spring Boot 4 · Spring Data JPA · Hibernate |
| Database | SQLite × 4 (IAM, Catalogue, Auction, Payment) |
| Frontend | HTML · Tailwind CSS · Vanilla JavaScript |
| Proxy | Nginx (reverse proxy + static file server) |
| Messaging | RabbitMQ 3 (pub/sub notifications) |
| AI | Ollama · gemma3:1b — fully local, no data leaves the machine |
| Containers | Docker · Docker Compose |
| Build | Maven via `mvnw` wrapper |

---

## Configuration

All credentials live in `.env` (gitignored). The deploy script auto-creates it from `.env.example`:

```env
RABBITMQ_DEFAULT_USER=guest
RABBITMQ_DEFAULT_PASS=guest
RABBITMQ_HOST=rabbitmq
RABBITMQ_PORT=5672
OLLAMA_HOST=ollama
OLLAMA_PORT=11434
OLLAMA_MODEL=gemma3:1b
```

---

## Project Structure

```
src/main/java/com/BidTech/auctionSystem/
├── AuctionService/          # Auction & Bid domain
├── CatalogueService/        # Product catalogue
├── IAMService/              # User management (login, register, CRUD)
├── payment/                 # Payment processing & receipts
├── chatbot/                 # AI chatbot (IntentResolver, ChatService, ActionExecutor)
├── config/                  # Multi-database configuration (4 SQLite files)
├── NotificationListener     # RabbitMQ event consumer
├── NotificationController   # Notification REST API
└── RabbitMQConfig           # Queue & exchange declarations

src/main/resources/static/   # Frontend (HTML, JS, CSS)

nginx/
├── loadbalancer.conf        # Reverse proxy routing rules
└── ui.conf                  # UI server config (port 3000)

scripts/
├── deploy.ps1               # Build + deploy + cleanup
├── start.ps1                # Start stopped containers
├── stop.ps1                 # Stop containers (data preserved)
└── ollama-entrypoint.sh     # Auto-pull model on first start
```

---

## API Reference

<details>
<summary><strong>Catalogue</strong></summary>

| Method | Endpoint | Description |
|:--|:--|:--|
| `GET` | `/api/catalogue/products` | List all products |
| `GET` | `/api/catalogue/products/{id}` | Get product by ID |
| `POST` | `/api/catalogue/products` | Create product |
| `PUT` | `/api/catalogue/products/{id}/status` | Update product status |
| `DELETE` | `/api/catalogue/products/{id}` | Delete product |
| `GET` | `/api/catalogue/health` | Health check |

</details>

<details>
<summary><strong>Auction</strong></summary>

| Method | Endpoint | Description |
|:--|:--|:--|
| `POST` | `/auction/create` | Create auction |
| `POST` | `/auction/{id}/bid` | Place bid |
| `GET` | `/auction/{id}/highest` | Get highest bid |
| `POST` | `/auction/{id}/end` | End auction |
| `GET` | `/auction/all` | List all auctions |

</details>

<details>
<summary><strong>Users</strong></summary>

| Method | Endpoint | Description |
|:--|:--|:--|
| `GET` | `/users` | List all users |
| `POST` | `/users` | Register user |
| `PUT` | `/users/reset-password/{name}` | Reset password |
| `DELETE` | `/users/{id}` | Delete user |

</details>

<details>
<summary><strong>Payment</strong></summary>

| Method | Endpoint | Description |
|:--|:--|:--|
| `POST` | `/api/payments/process` | Process payment |
| `GET` | `/api/payments/status/{txId}` | Check payment status |
| `GET` | `/api/payments/receipt/{id}` | Get receipt |
| `GET` | `/api/payments` | Payment history |

</details>

<details>
<summary><strong>Chatbot & Notifications</strong></summary>

| Method | Endpoint | Description |
|:--|:--|:--|
| `POST` | `/api/chat` | AI chatbot |
| `GET` | `/api/notifications` | Get all notifications |
| `GET` | `/api/notifications/count` | Notification count |
| `POST` | `/api/notifications/clear` | Clear notifications |

</details>

---

<p align="center">
  <sub>Built with ☕ Java · 🐳 Docker · 🤖 Ollama</sub>
</p>
