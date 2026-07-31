-- ============================================================
-- V12: Put every flight time on one timeline (UTC)
-- ------------------------------------------------------------
-- The table mixed two incompatible conventions:
--
--   * V3's seed stored wall-clock local times at each end, so
--     arrival_time - departure_time included the offset between
--     the two zones. Eleven rows disagreed with their own
--     `duration` column by exactly that offset.
--   * FlightScheduleService and the V10/V11 seeds derive arrival
--     as departure + duration, which is only meaningful if both
--     ends sit in the same zone.
--
-- The second convention is the one the code depends on:
-- deriveStatus and deriveProgress in FlightService compare "now"
-- against these columns and measure elapsed time between them,
-- which needs a single timeline. Local wall-clock times cannot
-- support that without a zone lookup on every comparison.
--
-- So the columns become UTC instants. Existing values are read as
-- local at the *departure* airport — which is what both seeds
-- meant — and converted. Arrival is then rederived from duration,
-- healing the eleven inconsistent rows.
--
-- Nothing is rescheduled: a flight's real departure moment is
-- unchanged, only how it is written down. The displayed local
-- time stays the same once the UI converts it back, which is what
-- the timezone fields added to the flight DTOs are for.
-- ============================================================

UPDATE flights f
SET departure_time = (f.departure_time AT TIME ZONE da.timezone) AT TIME ZONE 'UTC',
    arrival_time   = ((f.departure_time AT TIME ZONE da.timezone) AT TIME ZONE 'UTC')
                         + (f.duration * INTERVAL '1 minute')
FROM airports da
WHERE da.id = f.departure_airport_id
  AND da.timezone IS NOT NULL;

-- Any airport without a timezone would have been skipped above and left on the
-- old convention. There are none today, but failing loudly beats a silent mix.
DO $$
    DECLARE untimed INT;
    BEGIN
        SELECT count(*) INTO untimed FROM airports WHERE timezone IS NULL;
        IF untimed > 0 THEN
            RAISE EXCEPTION 'Cannot normalise flight times: % airport(s) have no timezone', untimed;
        END IF;
    END
$$;

COMMENT ON COLUMN flights.departure_time IS
    'UTC instant of departure. Render in the departure airport''s timezone.';
COMMENT ON COLUMN flights.arrival_time IS
    'UTC instant of arrival, always departure_time + duration. Render in the arrival airport''s timezone.';
COMMENT ON COLUMN flights.duration IS
    'Scheduled block time in minutes. Authoritative — arrival_time is derived from it.';
