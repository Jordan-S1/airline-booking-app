-- ============================================================
-- V4: Multi-currency support
-- ------------------------------------------------------------
-- Base currency is EUR. All stored prices and booking amounts
-- remain in EUR; the exchange_rates table holds display
-- conversion rates relative to 1 EUR. Each user has a preferred
-- display currency (defaults to EUR).
-- ============================================================

-- Per-user preferred display currency
ALTER TABLE users
    ADD COLUMN preferred_currency VARCHAR(3) NOT NULL DEFAULT 'EUR';

-- Supported display currencies and their rate relative to 1 EUR
CREATE TABLE exchange_rates
(
    id            BIGSERIAL PRIMARY KEY,
    currency_code VARCHAR(3)     NOT NULL UNIQUE,
    currency_name VARCHAR(50)    NOT NULL,
    symbol        VARCHAR(5)     NOT NULL,
    rate_from_eur NUMERIC(14, 6) NOT NULL,
    created_at    TIMESTAMP,
    updated_at    TIMESTAMP,

    CONSTRAINT chk_exchange_rate_positive CHECK (rate_from_eur > 0)
);

INSERT INTO exchange_rates (currency_code, currency_name, symbol, rate_from_eur, created_at, updated_at)
VALUES ('EUR', 'Euro', '€', 1.000000, NOW(), NOW()),
       ('GBP', 'British Pound', '£', 0.850000, NOW(), NOW()),
       ('USD', 'US Dollar', '$', 1.080000, NOW(), NOW()),
       ('CHF', 'Swiss Franc', 'CHF', 0.960000, NOW(), NOW()),
       ('CAD', 'Canadian Dollar', 'CA$', 1.470000, NOW(), NOW()),
       ('AUD', 'Australian Dollar', 'A$', 1.630000, NOW(), NOW()),
       ('JPY', 'Japanese Yen', '¥', 168.000000, NOW(), NOW());
