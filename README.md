# StockIQ Backend

Spring Boot 3.x microservices · Java 21 · PostgreSQL 16 · Redis 7

## Local demo — one command

```bash
docker compose up --build
```

Services start in dependency order. Wait for all health checks to pass (~60s):

| Service | Port | Health check |
|---------|------|-------------|
| PostgreSQL | 5432 | `pg_isready` |
| Redis | 6379 | `redis-cli ping` |
| Auth Service | 8081 | `GET /api/v1/auth/health` |
| Market Data | 8082 | `GET /actuator/health` |
| Portfolio | 8083 | `GET /actuator/health` |
| API Gateway | 8080 | Routes all traffic |

## API endpoints

```
# Auth (no JWT required)
POST http://localhost:8080/api/v1/auth/register
POST http://localhost:8080/api/v1/auth/login
POST http://localhost:8080/api/v1/auth/forgot-password

# Market data (JWT required)
GET  http://localhost:8080/api/v1/market/mag7
GET  http://localhost:8080/api/v1/market/quote/{symbol}
GET  http://localhost:8080/api/v1/market/crypto

# Portfolio (JWT required)
GET  http://localhost:8080/api/v1/portfolio
POST http://localhost:8080/api/v1/portfolio
POST http://localhost:8080/api/v1/portfolio/{id}/holdings
GET  http://localhost:8080/api/v1/portfolio/{id}/allocation
GET  http://localhost:8080/api/v1/portfolio/{id}/export/csv
```

## Default credentials (seeded by Flyway)

| Username | Password | Role |
|----------|----------|------|
| admin | Admin@123 | ADMIN |

## Live market data

Set `ALPHA_VANTAGE_API_KEY` in `.env` or docker-compose for real quotes:
```bash
ALPHA_VANTAGE_API_KEY=your_key docker compose up
```
Without a key, the service uses realistic mock data with the same response shape.

## Ports

All services use static routing (no Eureka required for local dev). The API Gateway
reads `AUTH_SERVICE_URI`, `MARKET_DATA_SERVICE_URI`, `PORTFOLIO_SERVICE_URI` env vars
— these are set automatically in docker-compose.yml.
