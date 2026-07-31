-- ============================================================
-- V11: Return legs for one-way airports
-- ------------------------------------------------------------
-- Four airports were unreachable in one direction or entirely:
--
--   DUS  no flights at all, in either direction
--   BCN  three inbound services, nothing outbound
--   LIS  one inbound, nothing outbound
--   ORY  one inbound, nothing outbound
--
-- A traveller could fly to Barcelona and never leave, and Düsseldorf
-- appeared in the airport list while matching no search. Each existing
-- inbound service now has a mirrored return, and Düsseldorf gets a pair
-- of short-haul rotations so it is served in both directions.
--
-- Times are stated as local at the departure airport, matching the
-- convention V12 normalises the whole table to. Return departures sit a
-- few hours after the inbound arrives, leaving a plausible turnaround.
-- ============================================================

WITH route (flight_number, airline_code, dep_code, arr_code,
            dep_time, duration_minutes, base_price, aircraft,
            total_seats, business_seats, first_seats) AS (
    VALUES
    -- Mirrors of the existing inbound-only services
    ('EI915',  'EI', 'BCN', 'DUB', '11:00', 170, 149.99, 'Airbus A320', 185, 30, 10),
    ('FR2483', 'FR', 'BCN', 'DUB', '10:15', 165, 39.99,  'Boeing 737-800', 189, 0, 0),
    ('IB415',  'IB', 'BCN', 'MAD', '09:00', 75,  59.99,  'Airbus A320', 200, 34, 8),
    ('FR6622', 'FR', 'LIS', 'DUB', '11:00', 170, 34.99,  'Boeing 737-800', 189, 0, 0),
    ('U2689',  'U2', 'ORY', 'DUB', '12:30', 140, 44.99,  'Airbus A320', 186, 0, 0),

    -- Düsseldorf rotations, giving it both arrivals and departures
    ('LH2434', 'LH', 'DUS', 'MUC', '07:20', 70,  119.99, 'Airbus A320neo', 180, 0, 0),
    ('LH2435', 'LH', 'MUC', 'DUS', '10:15', 70,  119.99, 'Airbus A320neo', 180, 0, 0),
    ('BA940',  'BA', 'LHR', 'DUS', '08:10', 85,  129.99, 'Airbus A320neo', 180, 0, 0),
    ('BA941',  'BA', 'DUS', 'LHR', '11:00', 90,  129.99, 'Airbus A320neo', 180, 0, 0)
)
INSERT INTO flights (flight_number, airline_id, departure_airport_id, arrival_airport_id,
                     departure_time, arrival_time, duration,
                     base_price, total_seats, available_seats,
                     economy_seats, business_seats, first_class_seats,
                     economy_price, business_price, first_class_price,
                     status, active, aircraft, created_at, updated_at)
SELECT r.flight_number,
       (SELECT id FROM airlines WHERE code = r.airline_code),
       (SELECT id FROM airports WHERE code = r.dep_code),
       (SELECT id FROM airports WHERE code = r.arr_code),
       (CURRENT_DATE + 1) + r.dep_time::TIME,
       (CURRENT_DATE + 1) + r.dep_time::TIME + (r.duration_minutes * INTERVAL '1 minute'),
       r.duration_minutes,
       r.base_price,
       r.total_seats,
       r.total_seats,
       r.total_seats - r.business_seats - r.first_seats,
       r.business_seats,
       r.first_seats,
       r.base_price,
       ROUND(r.base_price * 3.3, 2),
       CASE WHEN r.first_seats > 0 THEN ROUND(r.base_price * 6.6, 2) END,
       'SCHEDULED',
       true,
       r.aircraft,
       NOW(),
       NOW()
FROM route r;

UPDATE flights
SET gate     = CHR(65 + (id % 4)::INT) || (10 + (id * 7) % 30)::TEXT,
    terminal = ((id % 3) + 1)::TEXT
WHERE gate IS NULL
   OR terminal IS NULL;
