# =============================================================================
# deploy.ps1 — Full build and deployment script for the BidTech Auction System
# =============================================================================
#
# PURPOSE:
#   Builds the Spring Boot JAR, builds all Docker images, and starts the full
#   multi-container stack (backend, ui, loadbalancer) using Docker Compose.
#
# PREREQUISITES:
#   - Docker Desktop must be running
#   - Java 21 and Maven wrapper (mvnw) must be available in the project root
#   - Run from the project root directory: ./scripts/deploy.ps1
#
# WHAT IT DOES:
#   1. Verifies Docker is running
#   2. Builds the Spring Boot JAR via Maven
#   3. Tears down any existing stack (idempotent — safe to run multiple times)
#   4. Builds Docker images and starts all containers in detached mode
#   5. Polls the load balancer health endpoint until the app is ready (max 90s)
#
# OUTPUT:
#   Prints status messages at each step.
#   Exits with code 0 on success, 1 on any failure.
# =============================================================================

# ── Step 1: Verify Docker is running ─────────────────────────────────────────
# 'docker info' returns exit code 1 if the Docker daemon is not reachable.
# We suppress output and check only the exit code.
Write-Host ""
Write-Host "=== BidTech Deploy ===" -ForegroundColor Cyan
Write-Host ""
Write-Host "[1/5] Checking Docker is running..." -ForegroundColor Yellow

docker ps > $null 2>&1
if ($LASTEXITCODE -ne 0) {
    # Docker daemon is not running — nothing else will work without it
    # Using 'docker ps' instead of 'docker info' because on Windows with the
    # desktop-linux context, 'docker info' can return exit code 1 even when
    # Docker is fully operational. 'docker ps' is a more reliable liveness check.
    Write-Host "ERROR: Docker is not running. Please start Docker Desktop and try again." -ForegroundColor Red
    exit 1   # Exit code 1 = Docker not available
}
Write-Host "      Docker is running." -ForegroundColor Green

# ── Step 1b: Ensure .env file exists ──────────────────────────────────────────
# Docker Compose reads environment variables from .env at the project root.
# If a developer just cloned the repo, .env won't exist (it's gitignored).
# We copy .env.example → .env as a safe default so the stack can start.
# Developers can then edit .env to override credentials if needed.
if (-not (Test-Path ".env")) {
    Write-Host "      .env not found — copying from .env.example..." -ForegroundColor Yellow
    Copy-Item ".env.example" ".env"
    Write-Host "      .env created from .env.example. Edit it to change credentials." -ForegroundColor Green
} else {
    Write-Host "      .env file exists." -ForegroundColor Green
}

# ── Step 2: Build the Spring Boot JAR ────────────────────────────────────────
# Maven builds the JAR on the host machine, not inside Docker.
# This keeps the Docker image small (JRE-only, no Maven/JDK needed in container).
# -DskipTests speeds up the build — integration tests should be run separately.
Write-Host ""
Write-Host "[2/5] Building Spring Boot JAR (Maven)..." -ForegroundColor Yellow

./mvnw clean package -DskipTests
if ($LASTEXITCODE -ne 0) {
    # Maven failed — likely a compile error or missing dependency
    Write-Host "ERROR: Maven build failed. Check the output above for compile errors." -ForegroundColor Red
    exit 1   # Exit code 1 = Maven build failure
}
Write-Host "      JAR built successfully." -ForegroundColor Green

# ── Step 3: Tear down existing stack ─────────────────────────────────────────
# Remove any containers from a previous deploy before rebuilding.
# --remove-orphans cleans up containers from old service definitions that no
# longer exist in docker-compose.yml (e.g. after a service was renamed/removed).
# This makes the script idempotent — safe to run multiple times without errors.
Write-Host ""
Write-Host "[3/5] Removing existing containers..." -ForegroundColor Yellow

docker compose down --remove-orphans
# Not checking exit code here — 'down' on a non-existent stack exits 0 anyway

# Clean up dangling images, stopped containers, and build cache to prevent
# Docker from accumulating disk space over repeated deploys.
# --force skips the confirmation prompt. This only removes unused resources —
# active containers, volumes, and in-use images are never touched.
Write-Host "      Cleaning up unused Docker resources..." -ForegroundColor DarkGray
docker system prune -f > $null 2>&1

Write-Host "      Existing containers removed." -ForegroundColor Green

# ── Step 4: Build images and start all containers ────────────────────────────
# --build forces Docker to rebuild both images (backend and ui) even if they
# already exist — ensures the latest JAR and static files are included.
# -d runs containers in detached mode (background), freeing the terminal.
Write-Host ""
Write-Host "[4/5] Building images and starting containers..." -ForegroundColor Yellow

docker compose up --build -d
if ($LASTEXITCODE -ne 0) {
    # docker compose up failed — could be a port conflict, build error, or config issue
    Write-Host "ERROR: docker compose up failed. Run 'docker compose logs' to investigate." -ForegroundColor Red
    exit 1   # Exit code 1 = compose startup failure
}
Write-Host "      Containers started." -ForegroundColor Green

# ── Step 5: Wait for application to be healthy ───────────────────────────────
# Poll the load balancer health endpoint (port 80, not 8080) to verify the full
# routing chain is working: loadbalancer → backend → /api/catalogue/health.
# We wait up to 90 seconds — Spring Boot typically starts in 20-40s, but the
# load balancer also waits for backend and ui health checks before starting.
Write-Host ""
Write-Host "[5/5] Waiting for application to be ready (max 90s)..." -ForegroundColor Yellow

$maxWaitSeconds = 90   # Maximum time to wait before declaring a timeout failure
$elapsedSeconds = 0
$pollIntervalSeconds = 5   # Check every 5 seconds — frequent enough without hammering

$healthy = $false
do {
    Start-Sleep -Seconds $pollIntervalSeconds
    $elapsedSeconds += $pollIntervalSeconds

    try {
        # Invoke-WebRequest throws on non-2xx status, so we catch and continue
        $response = Invoke-WebRequest `
            -Uri "http://localhost/api/catalogue/health" `
            -UseBasicParsing `
            -TimeoutSec 3   # Short timeout per attempt — don't wait long if backend is down
        if ($response.StatusCode -eq 200) {
            $healthy = $true
            break
        }
    } catch {
        # Request failed (connection refused, timeout, etc.) — keep waiting
    }

    Write-Host "      Still waiting... ($elapsedSeconds / $maxWaitSeconds s)" -ForegroundColor DarkGray

} while ($elapsedSeconds -lt $maxWaitSeconds)

# ── Result ────────────────────────────────────────────────────────────────────
if (-not $healthy) {
    # Application did not become healthy within the timeout window
    Write-Host ""
    Write-Host "ERROR: Application did not become healthy within ${maxWaitSeconds}s." -ForegroundColor Red
    Write-Host "       Run the following to investigate:" -ForegroundColor Yellow
    Write-Host "         docker compose logs backend" -ForegroundColor White
    Write-Host "         docker compose logs ui" -ForegroundColor White
    Write-Host "         docker compose logs loadbalancer" -ForegroundColor White
    exit 1   # Exit code 1 = health check timeout
}

Write-Host ""
Write-Host "======================================" -ForegroundColor Green
Write-Host "  Application is ready!" -ForegroundColor Green
Write-Host "  Open: http://localhost" -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Green
Write-Host ""
