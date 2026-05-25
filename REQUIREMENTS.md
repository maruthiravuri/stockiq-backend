# StockIQ — Product Requirements

**Version:** 1.0  
**Platform:** Web (React + Spring Boot microservices)  
**Repos:** [stockiq-frontend](https://github.com/maruthiravuri/stockiq-frontend) · [stockiq-backend](https://github.com/maruthiravuri/stockiq-backend)

---

## 1. Overview

StockIQ is a real-time stock and ETF research platform for individual investors. It provides live market data, curated research tabs, a portfolio tracker, and a DCA calculator — all backed by a production-grade microservices architecture.

---

## 2. Architecture

```
Frontend (React 18 + Vite)  →  API Gateway (:8080)
                                  ├── Auth Service (:8081)       → PostgreSQL
                                  ├── Market Data Service (:8082) → Redis + Alpaca API
                                  └── Portfolio Service (:8083)  → PostgreSQL
```

### Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | React 18, TypeScript 5, Vite 6, MUI v9, Redux Toolkit, React Query |
| Backend | Java 21, Spring Boot 3.2.5, Spring Cloud 2023.0.1 |
| Database | PostgreSQL 16 (auth + portfolio), Redis 7 (market data cache) |
| Market Data | Alpaca Markets API (real-time IEX), Finnhub (fallback), Alpha Vantage (fallback) |
| Auth | JWT (access + refresh tokens), BCrypt password hashing |
| Infrastructure | Docker Compose (local), GitHub Actions CI/CD, ECS (prod) |

---

## 3. User Authentication

### 3.1 Registration
- Fields: email, username, password
- Password stored as BCrypt hash (10 rounds)
- Returns JWT access token (15 min) + refresh token (7 days)
- Default role: ANALYST

### 3.2 Login
- Accepts username OR email
- Returns same JWT pair
- Seed admin user: `admin / Admin@123`

### 3.3 Token Management
- Access token: 15-minute expiry, sent as `Authorization: Bearer <token>`
- Refresh token: 7-day expiry, used to silently renew access tokens
- Frontend auto-refreshes on 401 response via Axios interceptor

### 3.4 Security
- API Gateway validates JWT before forwarding to downstream services
- Gateway injects `X-User-Id` and `X-User-Role` headers
- Services trust gateway headers — no re-validation needed

---

## 4. Market Data

### 4.1 Data Sources (priority order)
1. **Alpaca Markets** — real-time IEX feed, free with paper trading account
2. **Finnhub** — 15-min delayed, 60 calls/min free tier
3. **Alpha Vantage** — 15-min delayed, 25 calls/day free tier

### 4.2 API Endpoints
| Endpoint | Description |
|----------|-------------|
| `GET /api/v1/market/quote/{symbol}` | Single symbol quote |
| `GET /api/v1/market/quotes?symbols=A,B,C` | Batch quotes (up to 100 — single Alpaca call) |
| `GET /api/v1/market/mag7` | Magnificent 7 quotes |
| `GET /api/v1/market/crypto` | Top 5 crypto pairs |
| `GET /api/v1/market/watchlist?symbols=A,B` | Watchlist quotes |
| `WS /ws/quotes` | STOMP WebSocket — live quote push every 5s |

### 4.3 Caching
- Redis TTL: 30 seconds per symbol
- Batch endpoint fetches all symbols in one Alpaca API call

### 4.4 Environment Variables
```bash
ALPACA_API_KEY=PKxxxxxxxx      # primary — real-time
ALPACA_API_SECRET=xxxxxxxx
FINNHUB_API_KEY=xxxxxxxx       # fallback 1
ALPHA_VANTAGE_API_KEY=xxxxxxxx # fallback 2 (default: demo = mock data)
```

---

## 5. Research Tabs

All symbol lists and criteria are editable in `src/config/research.ts`.

| Tab | Symbols | Data |
|-----|---------|------|
| Magnificent 7 | AAPL, MSFT, NVDA, GOOGL, AMZN, META, TSLA | Live |
| S&P 100 Movers | 20 large-cap symbols | Live, sorted by % move |
| S&P 500 Value | Auto-filtered by P/E < 20, Div > 1.5%, P/B < 5 | Live |
| Buffett Portfolio | BRK top holdings | Live |
| AI Stocks & ETFs | NVDA, AMD, PLTR, ARM, SMCI + AIQ, BOTZ, ARKQ, ROBO, SMH, SOXX | Live |
| Metals ETFs | GLD, IAU, SLV, SIVR, CPER, PDBC | Live |
| Crypto Top 5 | BTCUSD, ETHUSD, SOLUSD, XRPUSD, ADAUSD | Live |
| Dividend ETFs | SCHD, VYM, DVY, DGRO, HDV, JEPI, JEPQ, DIVO, PEY, SPHD | Live |
| Recurring | VOO, QQQ, QQQM, SCHD, VXUS, GLD, SMH, SOXX, COST, GOOGL, MSFT, WMT | Live + DCA calculator |
| Screener | 65+ symbols, dynamic filters | Live |
| Market Bar | SPY, QQQ, DIA, IWM, GLD, TLT, VIX | Live |

### 5.1 Tab Criteria (S&P 500 Value)
Defined in `TAB_CRITERIA` in `src/config/research.ts`. Change thresholds to change which stocks appear:
```ts
sp500value.criteria = [
  { field: 'peRatio',       operator: '<',  value: 20  },
  { field: 'dividendYield', operator: '>',  value: 1.5 },
  { field: 'pbRatio',       operator: '<',  value: 5   },
]
```

### 5.2 Screener
- 6 preset screens (High Dividend, Value, Momentum, Mega Cap, Low Beta, Tech, Deep Value)
- Filter fields: price, % change, market cap, volume, P/E, P/B, dividend yield, beta, sector
- All filters use live prices from Alpaca

---

## 6. Portfolio Management

### 6.1 Portfolios
- Create / rename / delete portfolios per user
- Multiple portfolios per user

### 6.2 Holdings
- Add holding: symbol, name, quantity, avg cost basis, current price, sector, asset type, purchase date
- Edit / delete holdings
- P&L calculated server-side: `(currentPrice - avgCostBasis) × quantity`
- Unrealized P&L % = `(currentPrice - avgCostBasis) / avgCostBasis × 100`

### 6.3 Allocation
- Sector allocation: pie chart + weight bars
- Asset type allocation: ETF vs Stock vs Bond etc.

### 6.4 CSV Export
- `GET /api/v1/portfolio/{id}/export/csv`
- Columns: Symbol, Name, Quantity, Avg Cost, Current Price, Market Value, Unrealized P&L, P&L %, Sector, Asset Type, Purchase Date

### 6.5 API Endpoints
| Endpoint | Description |
|----------|-------------|
| `GET /api/v1/portfolio` | List all user portfolios |
| `POST /api/v1/portfolio` | Create portfolio |
| `GET /api/v1/portfolio/{id}` | Get portfolio with holdings + P&L |
| `PUT /api/v1/portfolio/{id}` | Update portfolio name |
| `DELETE /api/v1/portfolio/{id}` | Delete portfolio |
| `POST /api/v1/portfolio/{id}/holdings` | Add holding |
| `PUT /api/v1/portfolio/{id}/holdings/{hId}` | Update holding |
| `DELETE /api/v1/portfolio/{id}/holdings/{hId}` | Delete holding |
| `GET /api/v1/portfolio/{id}/allocation` | Sector + asset type breakdown |
| `GET /api/v1/portfolio/{id}/export/csv` | Download CSV |

---

## 7. Watchlist

- Multiple watchlists per user (stored in localStorage)
- Add any symbol by ticker
- Set price alert threshold per symbol
- Live prices fetched every 30s via React Query
- Alert triggered when price ≥ alert price

---

## 8. Recurring Investments Tab

- 12 curated positions (8 ETFs + 4 stocks)
- DCA Calculator: enter weekly budget → see per-position amount + fractional shares
- Frequencies: Weekly, Bi-weekly, Monthly
- Filter: All / ETF / Stock
- Annual projection calculated automatically
- Sector pie chart shows portfolio mix
- Edit positions in `src/config/research.ts → RECURRING_SYMBOLS`

---

## 9. Configuration Files

| File | Purpose |
|------|---------|
| `src/config/tabs.tsx` | Tab names, icons, descriptions, order, visibility |
| `src/config/research.ts` | Symbol lists, tab criteria, screener presets, filter fields |

### Editing tab names
```ts
// src/config/tabs.tsx
{ id: 'mag7', label: 'Magnificent 7', description: '...', icon: <AutoGraphIcon />, group: 'Research' }
```

### Editing tab criteria
```ts
// src/config/research.ts → TAB_CRITERIA
sp500value: { criteria: [
  { field: 'peRatio', operator: '<', value: 15, label: 'P/E < 15' },
] }
```

### Adding a new tab
1. Add entry to `TAB_CONFIG` in `src/config/tabs.tsx`
2. Create the component in `src/components/research/`
3. Add to `TAB_MAP` in `src/App.tsx`

---

## 10. Local Development

### Prerequisites
- Node 18+, Docker Desktop

### Start backend
```bash
cd stockiq-backend
git pull origin main
docker compose down -v      # wipe postgres volume on first run
ALPACA_API_KEY=xx ALPACA_API_SECRET=xx docker compose up --build
```

### Start frontend
```bash
cd stockiq-frontend
git pull origin main
npm install --legacy-peer-deps
npm start                   # http://localhost:3000
```

### Default credentials
```
Username: admin
Password: Admin@123
```

---

## 11. CI/CD

### Frontend pipeline (GitHub Actions)
1. `Build & Test` — `tsc --noEmit`, `vitest run` (11 unit tests), `vite build`
2. `Playwright E2E` — 25 E2E tests (on develop/main only)
3. `Docker Build & Push` — pushes to ECR
4. `Deploy Staging` — S3 + CloudFront (develop branch)
5. `Deploy Production` — S3 + CloudFront + cache invalidation (main branch)

### Backend pipeline (GitHub Actions)
1. `Build & Test (auth-service)` — 6 Mockito unit tests
2. `Build & Test (market-data-service)` — 5 RestAssured integration tests
3. `Build & Test (portfolio-service)` — 7 MockMvc controller tests
4. `SAST — OWASP Dependency Check`
5. `Docker Build & Push` — all 5 services to ECR
6. `Deploy Staging / Production` — ECS rolling deploy

### Branch protection
- `main` and `develop` protected
- PRs required
- All build & test checks must pass before merge

---

## 12. Database Schema

### Auth Service (PostgreSQL)
```sql
users (id, email, username, password_hash, role, enabled, created_at, updated_at)
password_reset_tokens (id, user_id, token, expires_at, used, created_at)
```
Flyway tracking: `flyway_schema_history_auth`

### Portfolio Service (PostgreSQL)
```sql
portfolios (id, name, description, user_id, created_at, updated_at)
holdings (id, portfolio_id, symbol, name, quantity, avg_cost_basis, current_price,
          sector, asset_type, purchase_date, created_at, updated_at)
```
Flyway tracking: `flyway_schema_history_portfolio`

---

## 13. Pending / Next Steps

- [ ] AWS infrastructure (Terraform): RDS, ElastiCache, ECS, S3+CloudFront, Route53
- [ ] Add AWS secrets to GitHub for Docker push + ECS deploy
- [ ] Alpha Vantage / Finnhub / Alpaca API keys as GitHub secrets
- [ ] Historical price charts (intraday OHLCV)
- [ ] Real-time price alerts via Redis pub/sub → notification-service → email
- [ ] Prometheus + Grafana observability
- [ ] Custom screener backend endpoint `/api/v1/market/screen`
- [ ] Expand stock universe (currently 65+ symbols)
- [ ] Mobile app (React Native)
