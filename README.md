# Trading Platform — Backend

Covers two slices of the BRD so far: the Angel One SmartAPI integration, and the
full PostgreSQL/JPA persistence layer (all 10 tables from Section 16).

## What's here

```
src/main/java/com/tradingplatform/
  TradingPlatformApplication.java
  config/
    AngelOneProperties.java
    AngelOneConfig.java
  angelone/                              # Angel One SmartAPI client (see previous README section below)
  domain/
    User.java, BrokerAccount.java, StrategySettings.java, Signal.java,
    Trade.java, Position.java, DailyPnl.java, FundManagement.java,
    RiskSettings.java, SystemLog.java
    enums/                               # status/type enums shared across entities
  repository/                            # one Spring Data JpaRepository per entity
  security/
    EncryptedStringConverter.java        # AES-256-GCM field-level encryption
  controller/
src/main/resources/
  application.yml
  db/migration/
    V1__create_core_tables.sql           # Flyway migration - this IS the schema source of truth
docker-compose.yml                       # local Postgres for development
```

## Database setup

1. **Start Postgres locally:**
   ```bash
   docker compose up -d
   ```

2. **Generate an encryption key** (used to encrypt broker passwords/API keys/TOTP secrets at rest):
   ```bash
   openssl rand -base64 32
   ```

3. **Set environment variables** (add to your `.env` alongside the Angel One ones):
   ```
   DB_URL=jdbc:postgresql://localhost:5432/trading_platform
   DB_USERNAME=postgres
   DB_PASSWORD=postgres
   ENCRYPTION_KEY=<output from openssl rand -base64 32>
   ```

4. **Run the app** — Flyway runs `V1__create_core_tables.sql` automatically on startup:
   ```bash
   mvn spring-boot:run
   ```

   You should see Flyway logs confirming migration `V1` applied. Verify directly if you want:
   ```bash
   docker exec -it trading-platform-db psql -U postgres -d trading_platform -c "\dt"
   ```
   Should list all 10 tables.

## Schema notes

- **`broker_accounts.password_encrypted` / `api_key_encrypted` / `totp_secret_encrypted`**
  are genuinely encrypted (AES-256-GCM via `EncryptedStringConverter`), not just
  named that way — the JPA entity transparently encrypts on write and decrypts
  on read. Losing `ENCRYPTION_KEY` means losing access to all stored broker
  credentials, so back it up somewhere safe (password manager / secrets vault),
  separately from the database itself.
- **Flyway owns the schema**, Hibernate is set to `ddl-auto: validate` — it
  checks entities match the DB but never auto-alters tables. Any future schema
  change should be a new file: `V2__<description>.sql`, never editing `V1`.
- **`risk_settings` and `daily_pnl` are scoped per broker_account**, not per
  index — per BRD Section 9, the 2-trades/day and ₹4,500 loss cap apply
  *combined* across NIFTY + SENSEX, not separately for each.
- **`positions` is separate from `trades`** on purpose — live LTP/trailing-SL
  updates happen far more often (every tick) than trade state changes, so
  splitting them keeps the `trades` audit trail clean.

## Angel One SmartAPI integration

First working slice of the platform. Covers: login (with TOTP), token refresh,
profile fetch, live quotes, and order placement against Angel One's SmartAPI.

### Setup

1. **Get your Angel One SmartAPI credentials:**
   - API Key: from https://smartapi.angelone.in (create an app there)
   - Client Code: your Angel One trading account ID
   - Password: your account PIN/password (4-digit MPIN, not your login password)
   - TOTP Secret: the base32 secret shown when you enable TOTP-based 2FA on Angel One

2. **Set environment variables** — never hardcode these or commit them to git:
   ```bash
   export ANGELONE_API_KEY="your-api-key"
   export ANGELONE_CLIENT_CODE="your-client-code"
   export ANGELONE_PASSWORD="your-mpin"
   export ANGELONE_TOTP_SECRET="your-base32-totp-secret"
   ```

3. **Build and run:**
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```

### Verifying it works

```bash
curl -X POST http://localhost:8080/api/test/angelone/login
curl http://localhost:8080/api/test/angelone/profile
curl "http://localhost:8080/api/test/angelone/quote?exchange=NSE&token=99926000&mode=LTP"
```

## Design notes / what's deliberately deferred

- **No retry/backoff logic yet** on the Angel One clients.
- **No WebSocket feed yet** — REST polling only so far; live dashboard ticks
  need the SmartAPI WebSocket stream (`wss://smartapisocket.angelone.in/smart-stream`).
- **No service layer yet** — repositories exist, but nothing populates them
  yet (no signal generator, no order orchestration). That's the next layer.
- **`AngelOneOrderClient` has zero risk logic by design.** The risk module
  (Section 9) will sit in front of it, reading `risk_settings`/`daily_pnl`
  before deciding whether `placeOrder()` gets called at all.
- **Redis** is in the BRD's stack but not wired up yet — will matter most for
  the live dashboard/feed layer.

## Gann strike calculation engine

Pure-math implementation of BRD Sections 4 (NIFTY) and 5 (SENSEX):

```
Root        = SQRT(Open Price)
Buy Above   = (Root + offset)^2
Sell Below  = (Root - offset)^2
Spot SL     = Open Price - 60
CE Strike   = ROUND(Buy Above - 100, -2) + strikeAdjustment
PE Strike   = ROUND(Sell Below + 100, -2) - strikeAdjustment
```

Lives in `strategy/gann/`:
- `GannConstants.java` — the fixed per-index coefficients (offset 0.01562 for NIFTY
  vs 0.3124 for SENSEX, strike adjustment 200 vs 500). These come straight from the
  BRD's formulas and are NOT the same as `StrategySettings`' admin-configurable
  thresholds (premium, RSI, stop loss, targets) — these are structural to the formula.
- `GannCalculationService.java` — takes an index + open price, returns all levels.
  Pure function, no I/O. Uses BigDecimal throughout at 20-significant-digit precision,
  only rounding at the exact points the BRD's formulas specify (rounding earlier can
  shift a result across a 100-point boundary and produce the wrong strike).
- `GannLevels.java` — the result record.
- All math independently cross-checked against a Python/Decimal reference
  implementation before being ported to Java (see test file for exact verified values).

### Testing it

```bash
# Manual open price
curl "http://localhost:8080/api/test/gann/levels?index=NIFTY&openPrice=24350.50"

# Live open price via Angel One (Section 3's "Automatic Mode")
curl "http://localhost:8080/api/test/gann/levels/live?index=NIFTY&exchange=NSE&token=99926000"
```

The live endpoint only works once the market's open print is available for the day
(pre-market or before 9:15 AM IST, `open` will be null). SENSEX's exact token isn't
hardcoded anywhere in this codebase on purpose — look it up from Angel One's scrip
master before testing SENSEX live, rather than trusting a guessed value.

## Suggested next steps

1. Risk management module (Section 9) — reads/writes `risk_settings` + `daily_pnl`
2. Service layer wiring signal generation (RSI/volume/delta checks, Section 4 & 5
   entry conditions) → strike levels from `GannCalculationService` → `AngelOneOrderClient`
   → persisted `trades`/`positions`
3. Position sizing (Section 10) — the three quantity modes already modeled in
   `StrategySettings`, need the actual calculation logic
4. WebSocket live feed for the dashboard

