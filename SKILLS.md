# StockIQ — Skills & Competency Matrix

**Version:** 1.0  
**Project:** StockIQ Full-Stack Platform

---

## 1. Frontend Skills

### 1.1 React & TypeScript

| Skill | Applied In |
|-------|-----------|
| React 18 functional components + hooks | All components |
| TypeScript 5 strict mode | Entire codebase — `tsconfig.json` |
| Custom hooks (`useApi`, `useMarketWebSocket`) | `src/hooks/` |
| Context API (`AuthContext`) | Auth state management |
| `useCallback`, `useMemo`, `useEffect` | Performance optimization throughout |
| Error boundaries (component-level) | Loading/error states in all tabs |

### 1.2 State Management

| Skill | Applied In |
|-------|-----------|
| Redux Toolkit (`createSlice`) | UI state: sidebar, active tab, theme |
| React Query v5 (`useQuery`, `useMutation`) | All server state — market data, portfolio |
| Query invalidation on mutations | Portfolio CRUD (add/delete/edit holdings) |
| Auto-refetch intervals | Market data: 30s, portfolio: 60s |
| localStorage for ephemeral state | Watchlists |

### 1.3 Build & Tooling

| Skill | Applied In |
|-------|-----------|
| Vite 6 (Rolldown) | Build tool, dev server, HMR |
| Vite `define` for Node.js polyfills | `global: 'globalThis'`, `process.env: {}` |
| `manualChunks` for code splitting | Vendor, MUI, charts, grid, Redux |
| vitest + jsdom | Unit tests — format utils, Redux store, Sidebar |
| Playwright | E2E tests (25 tests) |
| `tsconfig.node.json` | Separate TS config for Vite config file |

### 1.4 UI Framework (MUI v9)

| Skill | Applied In |
|-------|-----------|
| MUI v9 Grid2 / Box CSS Grid | All layouts |
| MUI theming (`createTheme`) | Dark/light themes in `src/theme/index.ts` |
| Slots API (`slotProps`) | TextField, Dialog components |
| `sx` prop for inline styles | Throughout |
| `ToggleButtonGroup` | DCA frequency selector |
| `LinearProgress` | Dividend yield bars |

### 1.5 Data Visualization

| Skill | Applied In |
|-------|-----------|
| Recharts (`AreaChart`, `BarChart`, `PieChart`, `LineChart`) | All charts |
| AG Grid Community v35 | Stock tables with sort, filter, pagination |
| AG Grid `ModuleRegistry` | v35 module registration |
| AG Grid legacy theme mode | CSS compatibility with v35 Theming API |
| Sparkline (custom Recharts `LineChart`) | Mag7 card price paths |

### 1.6 API Integration

| Skill | Applied In |
|-------|-----------|
| Axios interceptors (request + response) | JWT attachment, auto token refresh |
| STOMP/SockJS WebSocket | Live quote push from market-data-service |
| Batch API calls (single request, multiple symbols) | MarketBar, all research tabs |
| `import.meta.env` for Vite env vars | `VITE_API_URL`, `VITE_WS_URL` |
| Graceful null/undefined handling | All API response fields coerced with `Number() \|\| 0` |

---

## 2. Backend Skills

### 2.1 Java & Spring Boot

| Skill | Applied In |
|-------|-----------|
| Java 21 (records, pattern matching, virtual threads) | DTOs as records, modern syntax |
| Spring Boot 3.2.5 auto-configuration | All services |
| Spring Security (stateless JWT) | Auth service, gateway filter |
| `@EnableMethodSecurity` + `@PreAuthorize` | Portfolio controller access control |
| Lombok (`@Data`, `@Builder`, `@RequiredArgsConstructor`, `@Slf4j`) | All entity + service classes |
| Spring Data JPA + Hibernate 6 | Portfolio + auth repositories |
| Flyway migrations | DB schema versioning (per-service tracking tables) |
| Spring Validation (`@Valid`, `@NotBlank`) | Request DTO validation |
| `@Scheduled` + `@EnableScheduling` | Quote broadcast every 5s |
| Spring Cache + Redis | 30s quote caching |
| Resilience4j Circuit Breaker + Retry | Alpha Vantage client |
| MapStruct | DTO ↔ entity mapping |

### 2.2 Microservices Architecture

| Skill | Applied In |
|-------|-----------|
| Spring Cloud Gateway (reactive) | API Gateway — routing, JWT validation |
| Gateway header injection (`X-User-Id`, `X-User-Role`) | Downstream service auth |
| Service-to-service communication via HTTP | Gateway → services |
| Static routing (no Eureka for local dev) | `AUTH_SERVICE_URI` env vars |
| STOMP WebSocket broker | Real-time quote push |
| `SimpMessagingTemplate` | Server → client quote broadcasting |
| `MappingJackson2MessageConverter` + `JavaTimeModule` | `Instant` serialization over WebSocket |

### 2.3 Database

| Skill | Applied In |
|-------|-----------|
| PostgreSQL 16 | Auth + portfolio data |
| Flyway 9.x multi-schema management | Per-service `flyway_schema_history_*` tables |
| `validate-on-migrate: false` | Resilient to schema drift in dev |
| `baseline-on-migrate: true` | Handles pre-existing schemas |
| HikariCP connection pooling | Configured in all services |
| Redis 7 caching | Market data 30s TTL |
| `@Cacheable` | Quote caching annotation |

### 2.4 Security

| Skill | Applied In |
|-------|-----------|
| JWT (JJWT 0.12.5) — access + refresh tokens | Auth service |
| BCrypt password hashing (10 rounds) | User passwords |
| Stateless Spring Security | All services |
| `GatewayHeaderAuthFilter` | Portfolio + market-data services |
| CORS configuration | API Gateway — allows localhost:3000/5173 |
| `UserDetailsServiceAutoConfiguration` exclusion | Prevents default password generation |

### 2.5 Testing

| Skill | Applied In |
|-------|-----------|
| JUnit 5 + Mockito (`@ExtendWith`) | Auth service unit tests (6 tests) |
| RestAssured integration tests | Market-data service (5 tests) |
| `@WebMvcTest` + `@MockBean` | Portfolio controller tests (7 tests) |
| `@AutoConfigureMockMvc(addFilters=false)` | Security bypass in controller slice tests |
| `@WithMockUser` | Mock auth principal in tests |
| Testcontainers (PostgreSQL) | Integration test DB (removed in favor of @WebMvcTest) |
| `@DynamicPropertySource` | Dynamic test DB URL injection |
| Surefire XML reports | Uploaded as CI artifacts |

---

## 3. DevOps & Infrastructure

### 3.1 Docker

| Skill | Applied In |
|-------|-----------|
| Multi-stage Dockerfiles (builder + runtime) | All 5 backend services |
| `eclipse-temurin:21-jdk-alpine` builder | Maven compile stage |
| `eclipse-temurin:21-jre-alpine` runtime | Minimal production image |
| Repo root as build context | Parent POM accessible in Docker build |
| `apk add --no-cache curl` | Health check dependency |
| `docker-compose.yml` with healthchecks | All services with `condition: service_healthy` |
| Named volumes (`pgdata`) | PostgreSQL data persistence |
| Service dependency ordering | Gateway waits for all 3 services healthy |
| `restart: on-failure` | Resilience to startup race conditions |
| `start_period: 90s` + `retries: 15` | Spring Boot startup tolerance |

### 3.2 GitHub Actions CI/CD

| Skill | Applied In |
|-------|-----------|
| Matrix strategy (`fail-fast: false`) | Parallel backend service builds |
| Service containers (postgres, redis) | Backend integration tests in CI |
| `setup-java@v4` with Maven cache | Build time optimization |
| `mvn install -N` (parent-first pattern) | Standalone service builds |
| `setup-node@v4` with npm cache | Frontend build optimization |
| Artifact upload (test reports, build output) | `upload-artifact@v4` |
| Branch protection rules via API | `PUT /repos/{owner}/{repo}/branches/{branch}/protection` |
| Environment secrets | AWS credentials, API keys |
| GitHub Environments (staging, production) | Deployment gates |

### 3.3 Maven

| Skill | Applied In |
|-------|-----------|
| Multi-module POM | Root `pom.xml` with 5 child modules |
| Spring Boot BOM inheritance | Dependency version management |
| Lombok annotation processor with explicit version | `annotationProcessorPaths` |
| `working-directory` per service | CI matrix build isolation |
| `mvn verify` (compile + test + package) | CI test command |
| `-DskipTests` in Dockerfile | Fast Docker builds |

---

## 4. Market Data Integration

### 4.1 Alpaca Markets

| Skill | Applied In |
|-------|-----------|
| REST API (`/v2/stocks/{symbol}/snapshot`) | Single quote |
| Batch snapshot endpoint (`/v2/stocks/snapshots?symbols=...`) | Up to 100 symbols in 1 call |
| IEX feed (free, real-time during market hours) | `?feed=iex` query param |
| Header auth (`APCA-API-KEY-ID`, `APCA-API-SECRET-KEY`) | `AlpacaClient.java` |
| Response deserialization (`@JsonIgnoreProperties`) | `AlpacaTrade`, `AlpacaBar`, `AlpacaSnapshot` |

### 4.2 Finnhub (fallback)

| Skill | Applied In |
|-------|-----------|
| `/api/v1/quote` endpoint | Real-time quote |
| `/api/v1/stock/profile2` endpoint | Company name, market cap |
| Token auth (`?token=`) | `FinnhubClient.java` |

### 4.3 Priority Fallback Chain

```
Alpaca (real-time)  →  Finnhub (15-min delayed)  →  Alpha Vantage (25/day)
```

---

## 5. Frontend Architecture Patterns

### 5.1 Config-Driven UI
- `src/config/tabs.tsx` — all tab metadata (label, icon, description, group, hidden)
- `src/config/research.ts` — symbol universes, tab criteria, screener presets
- Adding a new tab requires editing 2 files + creating 1 component

### 5.2 Data Flow
```
API call (React Query)  →  live quote map  →  mergeWithLive()  →  component render
                                                      ↓
                                          null if symbol has no live data
                                          (no mock fallback)
```

### 5.3 Error States
Every data-dependent component has 3 states:
1. **Loading** — `CircularProgress` + "Loading live data..."
2. **Error / no data** — `Alert` warning with backend URL hint
3. **Live** — data + `● LIVE` badge

### 5.4 Null Safety Pattern
All API numeric values wrapped: `Number(value) || 0`  
All format functions accept `undefined | null` and return safe default.

---

## 6. Key Files Reference

### Frontend
| File | Purpose |
|------|---------|
| `src/config/tabs.tsx` | Tab names, icons, order — edit to rename/reorder tabs |
| `src/config/research.ts` | Symbol lists, criteria, screener config — edit to change tab content |
| `src/services/api.ts` | Axios instance with JWT + auto-refresh interceptors |
| `src/hooks/useApi.ts` | All React Query hooks for market + portfolio data |
| `src/context/AuthContext.tsx` | Auth state + login/logout/register |
| `src/utils/format.ts` | `fmtPrice`, `fmtPct`, `fmtChange`, `fmtLargeNum`, `fmtVolume` |
| `src/components/common/AgStockTable.tsx` | AG Grid table used across all research tabs |
| `src/App.tsx` | Root component — `TAB_MAP` maps tab IDs to components |

### Backend
| File | Purpose |
|------|---------|
| `docker-compose.yml` | Local dev — all 5 services + Postgres + Redis |
| `market-data-service/AlpacaClient.java` | Primary market data — batch quotes |
| `market-data-service/MarketDataClient.java` | Priority fallback facade |
| `auth-service/V1__create_users_table.sql` | Users table + admin seed |
| `portfolio-service/V1__create_portfolio_tables.sql` | Portfolio + holdings schema |
| `portfolio-service/V2__add_current_price_to_holdings.sql` | Added current_price column |

---

## 7. Known Issues & Workarounds

| Issue | Workaround |
|-------|-----------|
| Flyway checksum conflict when V1 edited | `validate-on-migrate: false` + `baseline-on-migrate: true` in application.yml |
| Each service shares same PostgreSQL DB | Per-service Flyway tracking tables: `flyway_schema_history_auth`, `flyway_schema_history_portfolio` |
| AG Grid v35 CSS conflict error #239 | `theme="legacy"` prop on `AgGridReact` |
| AG Grid v35 module error #272 | `ModuleRegistry.registerModules([AllCommunityModule])` |
| `sockjs-client` uses Node's `global` | `define: { global: 'globalThis' }` in `vite.config.ts` |
| `process.env` in legacy deps | `define: { 'process.env': {} }` in `vite.config.ts` |
| CRA → Vite migration `ajv` conflict | Removed CRA entirely, migrated to Vite 6 |
| TypeScript 4 doesn't support `bundler` moduleResolution | Upgraded to TypeScript 5 |
| `eclipse-temurin:21-jre-alpine` has no `curl` | `apk add --no-cache curl` in runtime Dockerfile stage |
| `flyway-database-postgresql` not in Flyway 9.x | Removed — PostgreSQL support built into `flyway-core` |
| Lombok annotation processor missing version | Explicit `<version>${lombok.version}</version>` in parent pom |
