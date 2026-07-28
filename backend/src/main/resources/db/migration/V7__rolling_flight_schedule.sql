-- ============================================================
-- V7: Rolling flight schedule
-- ------------------------------------------------------------
-- The seed data sat in one fixed window (2026-08-01 → 08-06), so
-- every search would return nothing once that window passed.
--
-- Two changes make the schedule self-sustaining:
--   1. A flight number is only unique per departure time, so the
--      same service can run daily (EI156 flies every day) —
--      matching how airlines actually number flights.
--   2. Existing unbooked flights are re-anchored to start from
--      tomorrow, giving FlightScheduleService a live template set
--      to roll forward from.
--
-- Flights that already have bookings are left untouched: they are
-- historical records and their times must not move.
-- ============================================================

ALTER TABLE flights
    DROP CONSTRAINT IF EXISTS flights_flight_number_key;

ALTER TABLE flights
    ADD CONSTRAINT uq_flights_number_departure UNIQUE (flight_number, departure_time);

-- Shift the unbooked seed block so its first day is tomorrow.
WITH anchor AS (
    SELECT (CURRENT_DATE + 1) - MIN(departure_time)::date AS day_offset
    FROM flights
    WHERE NOT EXISTS (SELECT 1 FROM bookings b WHERE b.flight_id = flights.id)
)
UPDATE flights f
SET departure_time = f.departure_time + ((SELECT day_offset FROM anchor) * INTERVAL '1 day'),
    arrival_time   = f.arrival_time   + ((SELECT day_offset FROM anchor) * INTERVAL '1 day')
WHERE NOT EXISTS (SELECT 1 FROM bookings b WHERE b.flight_id = f.id)
  AND (SELECT day_offset FROM anchor) IS NOT NULL
  AND (SELECT day_offset FROM anchor) <> 0;

-- Supports the scheduler's per-number template lookups.
CREATE INDEX IF NOT EXISTS idx_flights_number_departure
    ON flights (flight_number, departure_time);
