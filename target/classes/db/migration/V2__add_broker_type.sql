-- V2: Add broker type and active flag to broker_accounts
-- Supports multiple broker integrations: Angel One (live), Zerodha/Upstox/Dhan (stub/future)

ALTER TABLE broker_accounts
    ADD COLUMN IF NOT EXISTS broker_type   VARCHAR(50)  NOT NULL DEFAULT 'ANGEL_ONE',
    ADD COLUMN IF NOT EXISTS display_name  VARCHAR(100),
    ADD COLUMN IF NOT EXISTS is_active     BOOLEAN      NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS is_enabled    BOOLEAN      NOT NULL DEFAULT true,
    ADD COLUMN IF NOT EXISTS notes         TEXT;

-- Set first account as active by default
UPDATE broker_accounts SET is_active = true WHERE id = (SELECT MIN(id) FROM broker_accounts);

-- Index for fast active broker lookup
CREATE INDEX IF NOT EXISTS idx_broker_accounts_active ON broker_accounts (is_active) WHERE is_active = true;
