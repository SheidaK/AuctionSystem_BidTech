# How to Build and Run using Docker Desktop

First deploy (or after any code change):

./scripts/deploy.ps1
Stop:

./scripts/stop.ps1
Start again (no rebuild):

./scripts/start.ps1
View logs:

docker compose logs -f
Full reset (deletes all data):

docker compose down -v
Then open http://localhost in your browser.
