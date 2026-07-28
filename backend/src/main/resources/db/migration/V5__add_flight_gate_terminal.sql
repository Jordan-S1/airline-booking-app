-- ============================================================
-- V5: Gate and terminal assignments for flights
-- ------------------------------------------------------------
-- Needed by the live flight status endpoint, which reports the
-- departure gate/terminal alongside the derived progress.
-- Existing seeded flights get deterministic assignments so the
-- data is stable across restarts.
-- ============================================================

ALTER TABLE flights
    ADD COLUMN gate     VARCHAR(10),
    ADD COLUMN terminal VARCHAR(10);

UPDATE flights
SET gate     = CHR(65 + (id % 4)::INT) || (10 + (id * 7) % 30)::TEXT,
    terminal = ((id % 3) + 1)::TEXT;
