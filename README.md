# Investment RiTech — Backend

REST API for Investment RiTech, a decision-support system for the NASDAQ 100 index built for RiTech SAS. It runs investment simulations, computes RSI(14) every five minutes from Yahoo Finance data, and emits BUY / SELL / HOLD recommendations with a plain-language explanation.

- Base URL: `https://ritechbackend.onrender.com/api`
- Swagger UI: `https://ritechbackend.onrender.com/swagger-ui.html`
- OpenAPI JSON: `https://ritechbackend.onrender.com/v3/api-docs`

The free Render instance sleeps after 15 minutes without traffic; the first request afterwards can take up to a minute.

## Stack

Java 21, Spring Boot 4.1 (Web MVC, Data JPA, Security, OAuth2 Resource Server for HS256 JWT, Validation), PostgreSQL, springdoc-openapi, Maven Wrapper.

## Run locally

1. Install Java 21.
2. Create a `.env` file in the project root (it is git-ignored) with the variables below.
3. Start the application:

   ```sh
   ./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
   ```

   The API listens on `http://localhost:8080`.

Run the tests (no database or internet access needed):

```sh
./mvnw test
```

## Environment variables

| Variable | Description |
|---|---|
| `DB_URL` | JDBC URL, e.g. `jdbc:postgresql://<host>:5432/<database>?sslmode=require` |
| `DB_USERNAME` | Database user |
| `DB_PASSWORD` | Database password |
| `JWT_SECRET` | HMAC secret for tokens, at least 32 bytes |
| `CORS_ALLOWED_ORIGINS` | Comma-separated origins. Default: `http://localhost:5173,http://localhost:3000` |
| `SEED_DEMO_DATA` | `true` inserts missing roles and demo users at startup |
| `DEMO_USER_PASSWORD` | Password for the demo user `usuario@ritech.co` |
| `DEMO_MANAGER_PASSWORD` | Password for the demo manager `gerente@ritech.co` |
| `PORT` | HTTP port (set by Render; default `8080`) |

The database schema is managed outside this project. Hibernate only validates it at startup (`ddl-auto: validate`).

## Endpoints

All responses are JSON with camelCase keys. Dates are ISO-8601 with the Bogotá offset. Money and percentages have 2 decimals. Send `Authorization: Bearer <token>` on every endpoint except login, password recovery, health and Swagger.

| Method and path | Role | Purpose |
|---|---|---|
| `POST /api/auth/login` | public | `{email, password}` → `{token, expiresAt, user}` |
| `POST /api/auth/password-recovery` | public | `{email}` → 202 with a generic message |
| `POST /api/auth/logout` | any | 204 |
| `GET /api/auth/me` | any | Current user |
| `GET /api/market/summary` | any | Index value, change, market status, RSI, current recommendation, data status |
| `GET /api/market/history?range=1d\|5d` | any | Chart points: time, close, RSI, recommendation, volatility, cycle status |
| `GET /api/alerts?types=BUY,SELL&from=&to=&page=&size=` | any | Paged alerts, newest first (BUY and SELL by default) |
| `GET /api/alerts/{id}` | any | Alert detail with criteria, traceability and that day's chart |
| `POST /api/simulations` | USER | `{capital, variation}` → simulation result |
| `GET /api/simulations?q=&from=&to=` | USER | Own simulations, newest first |
| `GET /api/simulations/{id}` | USER | Simulation detail |
| `GET /api/reports/statistics?range=1d\|5d` | MANAGER | Signal counts, volatility share, failed cycles, availability, theoretical performance |
| `GET /api/reports/history?types=&from=&to=` | MANAGER | Every cycle, including those without a recommendation |
| `GET /api/health` | public | `{status: "UP"}` |

Errors use one format:

```json
{ "code": "VALIDATION_ERROR", "message": "...", "fieldErrors": [{ "field": "capital", "code": "CAPITAL_MUST_BE_POSITIVE", "message": "..." }] }
```

Status codes: 400 validation, 401 missing or invalid token, 403 wrong role, 404 not found, 503 market source unavailable.

## Business rules in brief

- **Simulation:** result = 2 × the expected NASDAQ 100 change, capped at −100%; the final value is never negative. Amounts are in Colombian pesos (COP).
- **RSI(14):** Wilder's smoothing on completed 5-minute closes; at least 15 closes are required.
- **Recommendation:** RSI ≤ 30 → BUY, RSI ≥ 70 → SELL, otherwise HOLD (using the RSI rounded to 2 decimals). Signals in the first or last 15 minutes of the session carry a high-volatility warning.
- **Cycle:** runs every 5 minutes from 9:35 to 16:00 New York time, Monday to Friday. The summary endpoint also runs it on demand when the stored data is older than 5 minutes, because the hosting plan sleeps. Failures are recorded in `log_integracion` and produce no alert.

Market data comes from Yahoo Finance's public chart endpoint, for personal and academic use under Yahoo's terms.

## Deploy

The repository deploys to Render as a Docker web service. Render builds the root `Dockerfile` on every push to `main`. The health check path is `/api/health`, and the environment variables above are set in the Render dashboard.
