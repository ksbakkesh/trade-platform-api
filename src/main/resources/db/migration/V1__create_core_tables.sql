-- ============================================================================
-- V1: Core schema for NIFTY & SENSEX Options Auto Trading Platform
-- Tables per BRD Section 16: users, broker_accounts, strategy_settings,
-- signals, trades, positions, daily_pnl, fund_management, risk_settings,
-- system_logs
-- ============================================================================

-- ---------------------------------------------------------------------------
-- users
-- ---------------------------------------------------------------------------
CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(100) NOT NULL UNIQUE,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(20)  NOT NULL DEFAULT 'ADMIN'
                    CHECK (role IN ('ADMIN', 'VIEWER')),
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------------------
-- broker_accounts
-- NOTE: api_key / password / totp_secret columns hold ENCRYPTED values.
-- Application layer is responsible for encrypting before insert and
-- decrypting after select (e.g. via a JPA AttributeConverter backed by
-- AES-GCM with a key from a secrets manager). Never store these in plaintext.
-- ---------------------------------------------------------------------------
CREATE TABLE broker_accounts (
    id                      BIGSERIAL PRIMARY KEY,
    user_id                 BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    broker_name             VARCHAR(50) NOT NULL DEFAULT 'ANGEL_ONE',
    client_code             VARCHAR(50) NOT NULL,
    api_key_encrypted       VARCHAR(500) NOT NULL,
    password_encrypted      VARCHAR(500) NOT NULL,
    totp_secret_encrypted   VARCHAR(500) NOT NULL,
    is_active               BOOLEAN NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_broker_account_client UNIQUE (user_id, client_code)
);

CREATE INDEX idx_broker_accounts_user_id ON broker_accounts(user_id);

-- ---------------------------------------------------------------------------
-- strategy_settings
-- One row per (broker_account, index). Holds everything an admin can
-- configure per BRD Section 12.
-- ---------------------------------------------------------------------------
CREATE TABLE strategy_settings (
    id                          BIGSERIAL PRIMARY KEY,
    broker_account_id           BIGINT NOT NULL REFERENCES broker_accounts(id) ON DELETE CASCADE,
    index_name                  VARCHAR(10) NOT NULL CHECK (index_name IN ('NIFTY', 'SENSEX')),

    open_price_mode             VARCHAR(10) NOT NULL DEFAULT 'AUTO'
                                CHECK (open_price_mode IN ('AUTO', 'MANUAL')),
    manual_open_price           NUMERIC(10,2),

    premium_threshold           NUMERIC(10,2) NOT NULL,
    candle_timeframe_minutes    INT NOT NULL DEFAULT 15,
    rsi_threshold                NUMERIC(5,2) NOT NULL DEFAULT 60,
    volume_multiplier            NUMERIC(5,2) NOT NULL DEFAULT 2,
    delta_min                    NUMERIC(4,3) NOT NULL DEFAULT 0.450,
    delta_max                    NUMERIC(4,3) NOT NULL DEFAULT 0.650,

    stop_loss_points             NUMERIC(10,2) NOT NULL,
    target1_points                NUMERIC(10,2) NOT NULL,
    target2_points                NUMERIC(10,2) NOT NULL,

    exit_strategy_mode           VARCHAR(10) NOT NULL DEFAULT 'OPTION1'
                                CHECK (exit_strategy_mode IN ('OPTION1', 'OPTION2')),
    re_entry_enabled              BOOLEAN NOT NULL DEFAULT TRUE,

    quantity_mode                 VARCHAR(20) NOT NULL DEFAULT 'CAPITAL_BASED'
                                CHECK (quantity_mode IN ('FIXED_LOTS', 'CAPITAL_BASED', 'FIXED_QUANTITY')),
    fixed_lots                    INT,
    fixed_quantity                 INT,
    capital_allocation_percent    NUMERIC(5,2),
    max_lots                      INT,

    auto_trading_enabled           BOOLEAN NOT NULL DEFAULT FALSE,

    created_at                    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_strategy_settings_account_index UNIQUE (broker_account_id, index_name)
);

CREATE INDEX idx_strategy_settings_broker_account_id ON strategy_settings(broker_account_id);

-- ---------------------------------------------------------------------------
-- signals
-- A generated trading signal, before (and regardless of whether) it
-- results in an actual order. Lets us audit "why didn't it trade" too.
-- ---------------------------------------------------------------------------
CREATE TABLE signals (
    id                  BIGSERIAL PRIMARY KEY,
    broker_account_id   BIGINT NOT NULL REFERENCES broker_accounts(id) ON DELETE CASCADE,
    index_name           VARCHAR(10) NOT NULL CHECK (index_name IN ('NIFTY', 'SENSEX')),
    signal_type           VARCHAR(2) NOT NULL CHECK (signal_type IN ('CE', 'PE')),

    open_price             NUMERIC(10,2),
    buy_above               NUMERIC(10,2),
    sell_below               NUMERIC(10,2),
    strike_price             NUMERIC(10,2) NOT NULL,
    trading_symbol            VARCHAR(50),
    symbol_token               VARCHAR(20),

    premium_at_signal           NUMERIC(10,2),
    rsi_value                    NUMERIC(5,2),
    volume_ratio                  NUMERIC(6,3),
    delta_value                    NUMERIC(4,3),

    status                          VARCHAR(20) NOT NULL DEFAULT 'GENERATED'
                                CHECK (status IN ('GENERATED', 'EXECUTED', 'REJECTED', 'EXPIRED')),
    rejection_reason                 VARCHAR(255),

    generated_at                      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_signals_broker_account_id ON signals(broker_account_id);
CREATE INDEX idx_signals_generated_at ON signals(generated_at);

-- ---------------------------------------------------------------------------
-- trades
-- An actual order placed with the broker, derived from a signal.
-- ---------------------------------------------------------------------------
CREATE TABLE trades (
    id                   BIGSERIAL PRIMARY KEY,
    signal_id            BIGINT REFERENCES signals(id) ON DELETE SET NULL,
    broker_account_id    BIGINT NOT NULL REFERENCES broker_accounts(id) ON DELETE CASCADE,
    index_name            VARCHAR(10) NOT NULL CHECK (index_name IN ('NIFTY', 'SENSEX')),

    trading_symbol          VARCHAR(50) NOT NULL,
    symbol_token              VARCHAR(20) NOT NULL,
    transaction_type           VARCHAR(4) NOT NULL CHECK (transaction_type IN ('BUY', 'SELL')),
    quantity                     INT NOT NULL,

    entry_price                   NUMERIC(10,2),
    stop_loss_price                 NUMERIC(10,2),
    target1_price                    NUMERIC(10,2),
    target2_price                     NUMERIC(10,2),
    exit_price                          NUMERIC(10,2),

    broker_order_id                      VARCHAR(50),
    status                                  VARCHAR(20) NOT NULL DEFAULT 'OPEN'
                                CHECK (status IN ('OPEN', 'PARTIALLY_CLOSED', 'CLOSED', 'CANCELLED', 'REJECTED')),
    exit_reason                              VARCHAR(30)
                                CHECK (exit_reason IN ('TARGET1', 'TARGET2', 'STOP_LOSS', 'MANUAL', 'SQUARE_OFF')),
    realized_pnl                              NUMERIC(10,2),
    is_reentry                                  BOOLEAN NOT NULL DEFAULT FALSE,

    entry_time                                   TIMESTAMPTZ,
    exit_time                                      TIMESTAMPTZ,
    created_at                                      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                                        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_trades_broker_account_id ON trades(broker_account_id);
CREATE INDEX idx_trades_signal_id ON trades(signal_id);
CREATE INDEX idx_trades_status ON trades(status);
CREATE INDEX idx_trades_entry_time ON trades(entry_time);

-- ---------------------------------------------------------------------------
-- positions
-- Live state of an open trade: current LTP, trailing stop loss, unrealized P&L.
-- Separate from `trades` so high-frequency LTP updates don't churn the trades
-- table / its audit trail.
-- ---------------------------------------------------------------------------
CREATE TABLE positions (
    id                    BIGSERIAL PRIMARY KEY,
    trade_id              BIGINT NOT NULL UNIQUE REFERENCES trades(id) ON DELETE CASCADE,
    quantity_remaining     INT NOT NULL,
    current_ltp              NUMERIC(10,2),
    current_stop_loss          NUMERIC(10,2),
    unrealized_pnl                NUMERIC(10,2),
    sl_moved_to_cost                BOOLEAN NOT NULL DEFAULT FALSE,
    last_updated_at                   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_positions_trade_id ON positions(trade_id);

-- ---------------------------------------------------------------------------
-- daily_pnl
-- One row per broker_account per trading day. This is what the risk module
-- checks before allowing a new trade (Section 9: max 2 trades/day, 4500 loss cap).
-- ---------------------------------------------------------------------------
CREATE TABLE daily_pnl (
    id                       BIGSERIAL PRIMARY KEY,
    broker_account_id        BIGINT NOT NULL REFERENCES broker_accounts(id) ON DELETE CASCADE,
    trade_date                 DATE NOT NULL,
    total_trades                  INT NOT NULL DEFAULT 0,
    total_pnl                       NUMERIC(12,2) NOT NULL DEFAULT 0,
    daily_loss_limit_hit              BOOLEAN NOT NULL DEFAULT FALSE,
    max_trades_hit                       BOOLEAN NOT NULL DEFAULT FALSE,
    trading_disabled                       BOOLEAN NOT NULL DEFAULT FALSE,
    created_at                                TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                                   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_daily_pnl_account_date UNIQUE (broker_account_id, trade_date)
);

CREATE INDEX idx_daily_pnl_broker_account_id ON daily_pnl(broker_account_id);
CREATE INDEX idx_daily_pnl_trade_date ON daily_pnl(trade_date);

-- ---------------------------------------------------------------------------
-- fund_management
-- Periodic snapshots of available funds/margin, fetched from the broker
-- (Section 11: fund validation before order placement).
-- ---------------------------------------------------------------------------
CREATE TABLE fund_management (
    id                    BIGSERIAL PRIMARY KEY,
    broker_account_id     BIGINT NOT NULL REFERENCES broker_accounts(id) ON DELETE CASCADE,
    available_funds         NUMERIC(12,2),
    available_margin           NUMERIC(12,2),
    used_margin                   NUMERIC(12,2),
    today_pnl                       NUMERIC(12,2),
    fetched_at                         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_fund_management_broker_account_id ON fund_management(broker_account_id);
CREATE INDEX idx_fund_management_fetched_at ON fund_management(fetched_at);

-- ---------------------------------------------------------------------------
-- risk_settings
-- One row per broker_account (BRD Section 9 scope is "Combined NIFTY + SENSEX",
-- i.e. one shared risk budget per account, not per-index).
-- ---------------------------------------------------------------------------
CREATE TABLE risk_settings (
    id                    BIGSERIAL PRIMARY KEY,
    broker_account_id     BIGINT NOT NULL UNIQUE REFERENCES broker_accounts(id) ON DELETE CASCADE,
    max_trades_per_day      INT NOT NULL DEFAULT 2,
    daily_loss_limit           NUMERIC(10,2) NOT NULL DEFAULT 4500,
    scope                         VARCHAR(30) NOT NULL DEFAULT 'COMBINED',
    created_at                       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                          TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------------------
-- system_logs
-- General-purpose application/audit log, queryable from the dashboard.
-- `context` holds structured extra detail (e.g. request payload, error stack)
-- as JSON so we're not parsing free text later.
-- ---------------------------------------------------------------------------
CREATE TABLE system_logs (
    id              BIGSERIAL PRIMARY KEY,
    level           VARCHAR(10) NOT NULL CHECK (level IN ('DEBUG', 'INFO', 'WARN', 'ERROR')),
    source          VARCHAR(100),
    message         TEXT NOT NULL,
    context         JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_system_logs_created_at ON system_logs(created_at);
CREATE INDEX idx_system_logs_level ON system_logs(level);
