# =============================================================================
# stop.ps1 — Stop all running BidTech containers
# =============================================================================
#
# PURPOSE:
#   Gracefully stops all running containers in the BidTech stack.
#   Does NOT remove containers, images, or volumes — all data is preserved.
#
# PREREQUISITES:
#   - Docker Desktop must be running
#   - Run from the project root directory: ./scripts/stop.ps1
#
# WHAT IT DOES:
#   1. Stops all running containers (backend, ui, loadbalancer)
#
# DATA SAFETY:
#   'docker compose stop' only stops container processes — it does NOT remove:
#     - Containers (they can be resumed with start.ps1)
#     - The bidtech-data volume (all SQLite database files are preserved)
#     - Built images (no rebuild needed on next start)
#
#   To permanently delete all data, use: docker compose down -v
# =============================================================================

# ── Stop all containers ───────────────────────────────────────────────────────
# 'docker compose stop' sends SIGTERM to each container and waits for graceful
# shutdown. This is different from 'docker compose down' which also removes
# containers and networks. We use 'stop' here to preserve the container state
# so that 'start.ps1' can resume them quickly without a full redeploy.
Write-Host ""
Write-Host "=== BidTech Stop ===" -ForegroundColor Cyan
Write-Host ""
Write-Host "Stopping all containers..." -ForegroundColor Yellow

docker compose stop
if ($LASTEXITCODE -ne 0) {
    # Stop failed — this is unusual; containers may already be stopped
    Write-Host "WARNING: docker compose stop returned an error (containers may already be stopped)." -ForegroundColor Yellow
    # Not exiting with error — a failed stop is not critical
}

Write-Host ""
Write-Host "======================================" -ForegroundColor Green
Write-Host "  All containers stopped." -ForegroundColor Green
Write-Host "  Database data is preserved." -ForegroundColor Green
Write-Host "  Run ./scripts/start.ps1 to restart." -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Green
Write-Host ""
