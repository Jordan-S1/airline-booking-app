-- ============================================================
-- V3 — Seed data
-- Realistic Irish/European airline, airport and flight data
-- All data uses real IATA codes, real aircraft types,
-- and realistic routes/schedules/pricing
--
-- Airlines: Aer Lingus, Ryanair, British Airways, Lufthansa,
--           Air France, KLM, Iberia, EasyJet
-- Airports:  DUB, LHR, CDG, AMS, FRA, MAD, FCO, BCN,
--            LIS, DUS, MAN, ORY
-- Flights:   30 routes covering major Irish/European connections
-- ============================================================

-- ── Airlines ──────────────────────────────────────────────

INSERT INTO airlines (code, name, logo_url, website, country, active, created_at, updated_at)
VALUES ('EI', 'Aer Lingus',
        'https://upload.wikimedia.org/wikipedia/commons/thumb/7/7f/Aer_Lingus_logo.svg/320px-Aer_Lingus_logo.svg.png',
        'https://www.aerlingus.com', 'Ireland', true, NOW(), NOW()),
       ('FR', 'Ryanair', 'https://upload.wikimedia.org/wikipedia/commons/thumb/0/04/Ryanair.svg/320px-Ryanair.svg.png',
        'https://www.ryanair.com', 'Ireland', true, NOW(), NOW()),
       ('BA', 'British Airways',
        'https://upload.wikimedia.org/wikipedia/commons/thumb/4/42/British_Airways_Logo.svg/320px-British_Airways_Logo.svg.png',
        'https://www.britishairways.com', 'United Kingdom', true, NOW(), NOW()),
       ('LH', 'Lufthansa',
        'https://upload.wikimedia.org/wikipedia/commons/thumb/8/84/Lufthansa_Logo_2018.svg/320px-Lufthansa_Logo_2018.svg.png',
        'https://www.lufthansa.com', 'Germany', true, NOW(), NOW()),
       ('AF', 'Air France',
        'https://upload.wikimedia.org/wikipedia/commons/thumb/4/44/Air_France_Logo.svg/320px-Air_France_Logo.svg.png',
        'https://www.airfrance.com', 'France', true, NOW(), NOW()),
       ('KL', 'KLM', 'https://upload.wikimedia.org/wikipedia/commons/thumb/c/c7/KLM_logo.svg/320px-KLM_logo.svg.png',
        'https://www.klm.com', 'Netherlands', true, NOW(), NOW()),
       ('IB', 'Iberia',
        'https://upload.wikimedia.org/wikipedia/commons/thumb/a/a4/Iberia_logo.svg/320px-Iberia_logo.svg.png',
        'https://www.iberia.com', 'Spain', true, NOW(), NOW()),
       ('U2', 'EasyJet',
        'https://upload.wikimedia.org/wikipedia/commons/thumb/9/9a/EasyJet_logo.svg/320px-EasyJet_logo.svg.png',
        'https://www.easyjet.com', 'United Kingdom', true, NOW(), NOW());


-- ── Airports ──────────────────────────────────────────────

INSERT INTO airports (code, name, city, country, timezone, created_at, updated_at)
VALUES ('DUB', 'Dublin Airport', 'Dublin', 'Ireland', 'Europe/Dublin', NOW(), NOW()),
       ('LHR', 'Heathrow Airport', 'London', 'United Kingdom', 'Europe/London', NOW(), NOW()),
       ('CDG', 'Charles de Gaulle Airport', 'Paris', 'France', 'Europe/Paris', NOW(), NOW()),
       ('AMS', 'Amsterdam Airport Schiphol', 'Amsterdam', 'Netherlands', 'Europe/Amsterdam', NOW(), NOW()),
       ('FRA', 'Frankfurt Airport', 'Frankfurt', 'Germany', 'Europe/Berlin', NOW(), NOW()),
       ('MAD', 'Adolfo Suárez Madrid-Barajas', 'Madrid', 'Spain', 'Europe/Madrid', NOW(), NOW()),
       ('FCO', 'Leonardo da Vinci International', 'Rome', 'Italy', 'Europe/Rome', NOW(), NOW()),
       ('BCN', 'Josep Tarradellas Barcelona-El Prat', 'Barcelona', 'Spain', 'Europe/Madrid', NOW(), NOW()),
       ('LIS', 'Humberto Delgado Airport', 'Lisbon', 'Portugal', 'Europe/Lisbon', NOW(), NOW()),
       ('DUS', 'Düsseldorf Airport', 'Düsseldorf', 'Germany', 'Europe/Berlin', NOW(), NOW()),
       ('MAN', 'Manchester Airport', 'Manchester', 'United Kingdom', 'Europe/London', NOW(), NOW()),
       ('ORY', 'Paris Orly Airport', 'Paris', 'France', 'Europe/Paris', NOW(), NOW());


-- ── Flights ───────────────────────────────────────────────
-- Covers the next 2 months of routes from Dublin and major European hubs
-- Duration in minutes, prices in EUR

INSERT INTO flights (flight_number, airline_id, departure_airport_id, arrival_airport_id,
                     departure_time, arrival_time, duration,
                     base_price, total_seats, available_seats,
                     economy_seats, business_seats, first_class_seats,
                     economy_price, business_price, first_class_price,
                     status, active, aircraft, created_at, updated_at)
VALUES

-- ── Aer Lingus (EI) ──────────────────────────────────────

('EI156', (SELECT id FROM airlines WHERE code = 'EI'), (SELECT id FROM airports WHERE code = 'DUB'),
 (SELECT id FROM airports WHERE code = 'LHR'),
 '2026-08-01 06:30:00', '2026-08-01 07:55:00', 85, 89.99, 190, 190, 150, 30, 10, 89.99, 299.99, 599.99, 'SCHEDULED',
 true, 'Airbus A320', NOW(), NOW()),

('EI521', (SELECT id FROM airlines WHERE code = 'EI'), (SELECT id FROM airports WHERE code = 'DUB'),
 (SELECT id FROM airports WHERE code = 'CDG'),
 '2026-08-01 09:15:00', '2026-08-01 12:30:00', 135, 119.99, 185, 185, 145, 30, 10, 119.99, 349.99, 699.99, 'SCHEDULED',
 true, 'Airbus A320neo', NOW(), NOW()),

('EI633', (SELECT id FROM airlines WHERE code = 'EI'), (SELECT id FROM airports WHERE code = 'DUB'),
 (SELECT id FROM airports WHERE code = 'AMS'),
 '2026-08-02 07:00:00', '2026-08-02 09:55:00', 115, 109.99, 185, 185, 145, 30, 10, 109.99, 329.99, 649.99, 'SCHEDULED',
 true, 'Airbus A320neo', NOW(), NOW()),

('EI745', (SELECT id FROM airlines WHERE code = 'EI'), (SELECT id FROM airports WHERE code = 'DUB'),
 (SELECT id FROM airports WHERE code = 'FRA'),
 '2026-08-02 10:30:00', '2026-08-02 13:55:00', 145, 129.99, 185, 185, 145, 30, 10, 129.99, 379.99, 749.99, 'SCHEDULED',
 true, 'Airbus A321', NOW(), NOW()),

('EI832', (SELECT id FROM airlines WHERE code = 'EI'), (SELECT id FROM airports WHERE code = 'DUB'),
 (SELECT id FROM airports WHERE code = 'MAD'),
 '2026-08-03 08:00:00', '2026-08-03 11:30:00', 150, 139.99, 185, 185, 145, 30, 10, 139.99, 399.99, 799.99, 'SCHEDULED',
 true, 'Airbus A320neo', NOW(), NOW()),

('EI914', (SELECT id FROM airlines WHERE code = 'EI'), (SELECT id FROM airports WHERE code = 'DUB'),
 (SELECT id FROM airports WHERE code = 'BCN'),
 '2026-08-04 07:30:00', '2026-08-04 11:15:00', 165, 149.99, 185, 185, 145, 30, 10, 149.99, 429.99, 849.99, 'SCHEDULED',
 true, 'Airbus A320', NOW(), NOW()),

('EI157', (SELECT id FROM airlines WHERE code = 'EI'), (SELECT id FROM airports WHERE code = 'LHR'),
 (SELECT id FROM airports WHERE code = 'DUB'),
 '2026-08-01 14:00:00', '2026-08-01 15:20:00', 80, 89.99, 190, 190, 150, 30, 10, 89.99, 299.99, 599.99, 'SCHEDULED',
 true, 'Airbus A320', NOW(), NOW()),

-- ── Ryanair (FR) ─────────────────────────────────────────

('FR114', (SELECT id FROM airlines WHERE code = 'FR'), (SELECT id FROM airports WHERE code = 'DUB'),
 (SELECT id FROM airports WHERE code = 'LHR'),
 '2026-08-01 05:45:00', '2026-08-01 07:10:00', 85, 29.99, 189, 189, 189, 0, 0, 29.99, 0, 0, 'SCHEDULED', true,
 'Boeing 737-800', NOW(), NOW()),

('FR2482', (SELECT id FROM airlines WHERE code = 'FR'), (SELECT id FROM airports WHERE code = 'DUB'),
 (SELECT id FROM airports WHERE code = 'BCN'),
 '2026-08-02 06:30:00', '2026-08-02 10:10:00', 160, 39.99, 189, 189, 189, 0, 0, 39.99, 0, 0, 'SCHEDULED', true,
 'Boeing 737-800', NOW(), NOW()),

('FR5104', (SELECT id FROM airlines WHERE code = 'FR'), (SELECT id FROM airports WHERE code = 'DUB'),
 (SELECT id FROM airports WHERE code = 'MAD'),
 '2026-08-03 07:15:00', '2026-08-03 10:40:00', 145, 44.99, 189, 189, 189, 0, 0, 44.99, 0, 0, 'SCHEDULED', true,
 'Boeing 737 MAX 8', NOW(), NOW()),

('FR8812', (SELECT id FROM airlines WHERE code = 'FR'), (SELECT id FROM airports WHERE code = 'DUB'),
 (SELECT id FROM airports WHERE code = 'FCO'),
 '2026-08-04 06:00:00', '2026-08-04 09:55:00', 175, 49.99, 189, 189, 189, 0, 0, 49.99, 0, 0, 'SCHEDULED', true,
 'Boeing 737-800', NOW(), NOW()),

('FR6621', (SELECT id FROM airlines WHERE code = 'FR'), (SELECT id FROM airports WHERE code = 'DUB'),
 (SELECT id FROM airports WHERE code = 'LIS'),
 '2026-08-05 07:30:00', '2026-08-05 10:15:00', 165, 34.99, 189, 189, 189, 0, 0, 34.99, 0, 0, 'SCHEDULED', true,
 'Boeing 737-800', NOW(), NOW()),

('FR7723', (SELECT id FROM airlines WHERE code = 'FR'), (SELECT id FROM airports WHERE code = 'DUB'),
 (SELECT id FROM airports WHERE code = 'MAN'),
 '2026-08-06 08:00:00', '2026-08-06 09:10:00', 70, 24.99, 189, 189, 189, 0, 0, 24.99, 0, 0, 'SCHEDULED', true,
 'Boeing 737-800', NOW(), NOW()),

-- ── British Airways (BA) ──────────────────────────────────

('BA832', (SELECT id FROM airlines WHERE code = 'BA'), (SELECT id FROM airports WHERE code = 'LHR'),
 (SELECT id FROM airports WHERE code = 'DUB'),
 '2026-08-01 08:00:00', '2026-08-01 09:25:00', 85, 149.99, 168, 168, 128, 32, 8, 149.99, 449.99, 899.99, 'SCHEDULED',
 true, 'Airbus A319', NOW(), NOW()),

('BA308', (SELECT id FROM airlines WHERE code = 'BA'), (SELECT id FROM airports WHERE code = 'LHR'),
 (SELECT id FROM airports WHERE code = 'CDG'),
 '2026-08-01 07:00:00', '2026-08-01 09:20:00', 80, 129.99, 168, 168, 128, 32, 8, 129.99, 399.99, 799.99, 'SCHEDULED',
 true, 'Airbus A319', NOW(), NOW()),

('BA902', (SELECT id FROM airlines WHERE code = 'BA'), (SELECT id FROM airports WHERE code = 'LHR'),
 (SELECT id FROM airports WHERE code = 'FRA'),
 '2026-08-02 09:30:00', '2026-08-02 12:10:00', 100, 159.99, 168, 168, 128, 32, 8, 159.99, 479.99, 949.99, 'SCHEDULED',
 true, 'Airbus A320', NOW(), NOW()),

('BA460', (SELECT id FROM airlines WHERE code = 'BA'), (SELECT id FROM airports WHERE code = 'LHR'),
 (SELECT id FROM airports WHERE code = 'MAD'),
 '2026-08-03 10:00:00', '2026-08-03 13:15:00', 135, 179.99, 168, 168, 128, 32, 8, 179.99, 529.99, 1049.99, 'SCHEDULED',
 true, 'Airbus A321', NOW(), NOW()),

-- ── Lufthansa (LH) ───────────────────────────────────────

('LH997', (SELECT id FROM airlines WHERE code = 'LH'), (SELECT id FROM airports WHERE code = 'FRA'),
 (SELECT id FROM airports WHERE code = 'DUB'),
 '2026-08-01 11:30:00', '2026-08-01 13:00:00', 150, 169.99, 220, 220, 174, 38, 8, 169.99, 499.99, 999.99, 'SCHEDULED',
 true, 'Airbus A321', NOW(), NOW()),

('LH900', (SELECT id FROM airlines WHERE code = 'LH'), (SELECT id FROM airports WHERE code = 'FRA'),
 (SELECT id FROM airports WHERE code = 'LHR'),
 '2026-08-02 07:00:00', '2026-08-02 08:05:00', 65, 139.99, 220, 220, 174, 38, 8, 139.99, 419.99, 829.99, 'SCHEDULED',
 true, 'Airbus A320', NOW(), NOW()),

('LH984', (SELECT id FROM airlines WHERE code = 'LH'), (SELECT id FROM airports WHERE code = 'FRA'),
 (SELECT id FROM airports WHERE code = 'AMS'),
 '2026-08-03 09:00:00', '2026-08-03 10:10:00', 70, 99.99, 220, 220, 174, 38, 8, 99.99, 299.99, 599.99, 'SCHEDULED',
 true, 'Airbus A319', NOW(), NOW()),

-- ── Air France (AF) ──────────────────────────────────────

('AF546', (SELECT id FROM airlines WHERE code = 'AF'), (SELECT id FROM airports WHERE code = 'CDG'),
 (SELECT id FROM airports WHERE code = 'DUB'),
 '2026-08-01 14:30:00', '2026-08-01 15:50:00', 80, 149.99, 200, 200, 158, 34, 8, 149.99, 449.99, 899.99, 'SCHEDULED',
 true, 'Airbus A320', NOW(), NOW()),

('AF1780', (SELECT id FROM airlines WHERE code = 'AF'), (SELECT id FROM airports WHERE code = 'CDG'),
 (SELECT id FROM airports WHERE code = 'LHR'),
 '2026-08-02 08:00:00', '2026-08-02 09:05:00', 65, 109.99, 200, 200, 158, 34, 8, 109.99, 329.99, 659.99, 'SCHEDULED',
 true, 'Airbus A318', NOW(), NOW()),

('AF1240', (SELECT id FROM airlines WHERE code = 'AF'), (SELECT id FROM airports WHERE code = 'CDG'),
 (SELECT id FROM airports WHERE code = 'AMS'),
 '2026-08-03 10:15:00', '2026-08-03 11:35:00', 80, 89.99, 200, 200, 158, 34, 8, 89.99, 269.99, 539.99, 'SCHEDULED',
 true, 'Airbus A320', NOW(), NOW()),

-- ── KLM ──────────────────────────────────────────────────

('KL945', (SELECT id FROM airlines WHERE code = 'KL'), (SELECT id FROM airports WHERE code = 'AMS'),
 (SELECT id FROM airports WHERE code = 'DUB'),
 '2026-08-01 10:00:00', '2026-08-01 11:05:00', 65, 129.99, 186, 186, 144, 34, 8, 129.99, 389.99, 779.99, 'SCHEDULED',
 true, 'Boeing 737-900', NOW(), NOW()),

('KL1009', (SELECT id FROM airlines WHERE code = 'KL'), (SELECT id FROM airports WHERE code = 'AMS'),
 (SELECT id FROM airports WHERE code = 'LHR'),
 '2026-08-02 07:30:00', '2026-08-02 08:25:00', 55, 99.99, 186, 186, 144, 34, 8, 99.99, 299.99, 599.99, 'SCHEDULED',
 true, 'Boeing 737-700', NOW(), NOW()),

('KL1773', (SELECT id FROM airlines WHERE code = 'KL'), (SELECT id FROM airports WHERE code = 'AMS'),
 (SELECT id FROM airports WHERE code = 'FRA'),
 '2026-08-03 08:45:00', '2026-08-03 09:55:00', 70, 89.99, 186, 186, 144, 34, 8, 89.99, 269.99, 539.99, 'SCHEDULED',
 true, 'Embraer E190', NOW(), NOW()),

-- ── Iberia (IB) ──────────────────────────────────────────

('IB3166', (SELECT id FROM airlines WHERE code = 'IB'), (SELECT id FROM airports WHERE code = 'MAD'),
 (SELECT id FROM airports WHERE code = 'DUB'),
 '2026-08-01 07:30:00', '2026-08-01 10:10:00', 160, 159.99, 200, 200, 158, 34, 8, 159.99, 479.99, 959.99, 'SCHEDULED',
 true, 'Airbus A320', NOW(), NOW()),

('IB3164', (SELECT id FROM airlines WHERE code = 'IB'), (SELECT id FROM airports WHERE code = 'MAD'),
 (SELECT id FROM airports WHERE code = 'LHR'),
 '2026-08-02 09:00:00', '2026-08-02 11:15:00', 135, 139.99, 200, 200, 158, 34, 8, 139.99, 419.99, 839.99, 'SCHEDULED',
 true, 'Airbus A321', NOW(), NOW()),

('IB414', (SELECT id FROM airlines WHERE code = 'IB'), (SELECT id FROM airports WHERE code = 'MAD'),
 (SELECT id FROM airports WHERE code = 'BCN'),
 '2026-08-03 06:45:00', '2026-08-03 07:55:00', 70, 59.99, 200, 200, 158, 34, 8, 59.99, 179.99, 359.99, 'SCHEDULED',
 true, 'Airbus A320', NOW(), NOW()),

-- ── EasyJet (U2) ─────────────────────────────────────────

('U2395', (SELECT id FROM airlines WHERE code = 'U2'), (SELECT id FROM airports WHERE code = 'LHR'),
 (SELECT id FROM airports WHERE code = 'DUB'),
 '2026-08-01 12:00:00', '2026-08-01 13:15:00', 75, 49.99, 186, 186, 186, 0, 0, 49.99, 0, 0, 'SCHEDULED', true,
 'Airbus A319', NOW(), NOW()),

('U2507', (SELECT id FROM airlines WHERE code = 'U2'), (SELECT id FROM airports WHERE code = 'LHR'),
 (SELECT id FROM airports WHERE code = 'AMS'),
 '2026-08-02 08:30:00', '2026-08-02 11:00:00', 90, 59.99, 186, 186, 186, 0, 0, 59.99, 0, 0, 'SCHEDULED', true,
 'Airbus A320', NOW(), NOW()),

('U2241', (SELECT id FROM airlines WHERE code = 'U2'), (SELECT id FROM airports WHERE code = 'MAN'),
 (SELECT id FROM airports WHERE code = 'DUB'),
 '2026-08-03 07:00:00', '2026-08-03 08:15:00', 75, 39.99, 186, 186, 186, 0, 0, 39.99, 0, 0, 'SCHEDULED', true,
 'Airbus A319', NOW(), NOW()),

('U2688', (SELECT id FROM airlines WHERE code = 'U2'), (SELECT id FROM airports WHERE code = 'DUB'),
 (SELECT id FROM airports WHERE code = 'ORY'),
 '2026-08-04 09:30:00', '2026-08-04 12:45:00', 135, 44.99, 186, 186, 186, 0, 0, 44.99, 0, 0, 'SCHEDULED', true,
 'Airbus A320', NOW(), NOW());
