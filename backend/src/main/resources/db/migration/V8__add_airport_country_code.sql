-- ============================================================
-- V8: Airport country codes
-- ------------------------------------------------------------
-- The frontend renders a country flag per destination, and flag
-- icon sets key on ISO 3166-1 alpha-2 codes rather than country
-- names. Storing the code alongside the name means a new airport
-- carries its own flag identity, instead of the frontend keeping
-- a name -> code lookup that has to be edited in lockstep.
--
-- Nullable by design: the UI falls back to a neutral marker when
-- the code is absent, so an airport added without one still works.
-- ============================================================

-- VARCHAR(2) rather than CHAR(2): Postgres reports CHAR as `bpchar`, which
-- fails Hibernate's schema validation against a String field (it expects
-- varchar). CHAR would also space-pad, which we don't want.
ALTER TABLE airports
    ADD COLUMN country_code VARCHAR(2);

-- Uppercase alpha-2 only, so a bad value fails at write time
-- rather than silently rendering no flag.
ALTER TABLE airports
    ADD CONSTRAINT chk_airports_country_code
        CHECK (country_code IS NULL OR country_code ~ '^[A-Z]{2}$');

UPDATE airports SET country_code = 'IE' WHERE code IN ('DUB');
UPDATE airports SET country_code = 'GB' WHERE code IN ('LHR', 'MAN');
UPDATE airports SET country_code = 'FR' WHERE code IN ('CDG', 'ORY');
UPDATE airports SET country_code = 'NL' WHERE code IN ('AMS');
UPDATE airports SET country_code = 'DE' WHERE code IN ('FRA', 'DUS');
UPDATE airports SET country_code = 'ES' WHERE code IN ('MAD', 'BCN');
UPDATE airports SET country_code = 'IT' WHERE code IN ('FCO');
UPDATE airports SET country_code = 'PT' WHERE code IN ('LIS');
