# 🏛️ BidTech Auction System

A full-stack auction platform built with **Spring Boot**, **SQLite**, and a multi-container **Docker** deployment. Users can browse catalogue items, create auctions, place bids, and process payments — all through a responsive Tailwind CSS interface.

---

## 🧱 Architecture

```
User Browser
     │
     ▼  http://localhost (port 80)
┌─────────────────────┐
│   Nginx Load Balancer│  ← only host-exposed port
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
            ┌─────────────┐
            │ SQLite DBs  │
            │ Docker Vol  │
            └─────────────┘
```

| Container | Role | Host Port |
|---|---|---|
| `bidtech-loadbalancer` | Nginx reverse proxy — single entry point | **80** |
| `bidtech-backend` | Spring Boot REST API | internal only |
| `bidtech-ui` | Nginx static file server | internal only |

---

## 🚀 Quick Start — Docker Desktop

> **Prerequisites:** Docker Desktop running, Java 21, Maven

### First deploy (or after any code change)

```powershell
./scripts/deploy.ps1
```

This will:
1. Build the Spring Boot JAR via Maven
2. Build both Docker images
3. Start all three containers
4. Wait until healthy, then print the URL

### Open the app

```
http://localhost
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
| Payment | `http://localhost/pay.html` | Process payments and receipts |

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 4, Spring Data JPA, Hibernate |
| Database | SQLite (4 separate databases — IAM, Catalogue, Auction, Payment) |
| Frontend | HTML, Tailwind CSS, Vanilla JavaScript |
| Proxy | Nginx (load balancer + static file server) |
| Containers | Docker, Docker Compose |
| Build | Maven (via `mvnw` wrapper) |

---

## 📁 Project Structure

```
├── src/main/java/              # Spring Boot application
│   └── com/BidTech/auctionSystem/
│       ├── AuctionService/     # Auction & Bid domain
│       ├── CatalogueService/   # Product catalogue
│       ├── IAMService/         # User management
│       ├── payment/            # Payment processing
│       └── config/             # Multi-database configuration
├── src/main/resources/static/  # Frontend HTML/JS (served by UI container)
├── Dockerfile.backend          # Spring Boot image
├── Dockerfile.ui               # Nginx static file image
├── docker-compose.yml          # Full stack definition
├── nginx/
│   ├── loadbalancer.conf       # Reverse proxy routing rules
│   └── ui.conf                 # UI Nginx server config
└── scripts/
    ├── deploy.ps1              # Build + deploy
    ├── start.ps1               # Start stopped containers
    └── stop.ps1                # Stop containers (data preserved)
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
| Payment | GET | `/api/payments/receipt/{id}` | Get receipt |
