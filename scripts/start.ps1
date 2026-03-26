# =============================================================================
# start.ps1 — Start all stopped BidTech containers
# =============================================================================
#
# PURPOSE:
#   Resumes all containers in the BidTech stack that were previously stopped
#   with stop.ps1. Does NOT rebuild images or recreate containers.
#
# PREREQUISITES:
#   - Docker Desktop must be running
#   - The stack must have been deployed at least once with deploy.ps1
#   - Run from the project root directory: ./scripts/start.ps1
#
# WHAT IT DOES:
#   1. Verifies Docker is running
#   2. Starts all stopped containers (backend, ui, loadbalancer)
#
# NOTE:
#   Use deploy.ps1 if you need to rebuild images or apply code changes.
#   start.ps1 only resumes existing containers — it does not rebuild anything.
# =============================================================================

# ── Step 1: Verify Docker is running ─────────────────────────────────────────
# 'docker info' returns exit code 1 if the Docker daemon is not reachable.
Write-Host ""
Write-Host "=== BidTech Start ===" -ForegroundColor Cyan
Write-Host ""
Write-Host "[1/2] Checking Docker is running..." -ForegroundColor Yellow

docker ps > $null 2>&1
if ($LASTEXITCODE -ne 0) {
    # Docker daemon is not running — containers cannot be started without it
    # Using 'docker ps' for the same reason as deploy.ps1 — more reliable on Windows
    Write-Host "ERROR: Docker is not running. Please start Docker Desktop and try again." -ForegroundColor Red
    exit 1   # Exit code 1 = Docker not available
}
Write-Host "      Docker is running." -ForegroundColor Green

# ── Step 2: Start all containers ─────────────────────────────────────────────
# 'docker compose start' resumes stopped containers without recreating them.
# This is faster than 'docker compose up' because it skips image builds and
# container recreation — it simply unpauses the existing container processes.
# Database data is preserved because the bidtech-data volume is untouched.
Write-Host ""
Write-Host "[2/2] Starting all containers..." -ForegroundColor Yellow

docker compose start
if ($LASTEXITCODE -ne 0) {
    # Start failed — containers may not exist (run deploy.ps1 first)
    Write-Host "ERROR: Failed to start containers." -ForegroundColor Red
    Write-Host "       If this is a fresh environment, run deploy.ps1 first." -ForegroundColor Yellow
    exit 1   # Exit code 1 = containers could not be started
}

Write-Host ""
Write-Host "======================================" -ForegroundColor Green
Write-Host "  All containers started!" -ForegroundColor Green
Write-Host "  Open: http://localhost" -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Green
Write-Host ""
