-- ============================================================
-- V9: Global airports and airlines
-- ------------------------------------------------------------
-- The network was 12 Western European airports across 8 carriers,
-- which made every search a short-haul European hop. This widens
-- it to the major world hubs so long-haul routes, and the route
-- map's world view, have somewhere to go.
--
-- Reference data only — the timetable that connects these is in
-- V10, kept separate so the two can be reviewed independently.
--
-- Coordinates are the published airport reference points, needed
-- by RouteMap; timezones are IANA identifiers; country_code is
-- ISO 3166-1 alpha-2 to satisfy chk_airports_country_code and
-- drive the flag icons.
--
-- logo_url is deliberately left NULL: nothing in the frontend
-- renders it, and inventing 17 image URLs that may rot is worse
-- than an empty column that the API already treats as optional.
-- ============================================================

INSERT INTO airports (code, name, city, country, timezone, latitude, longitude, country_code, created_at, updated_at)
VALUES
-- ── North America ────────────────────────────────────────
('JFK', 'John F. Kennedy International Airport', 'New York', 'United States', 'America/New_York',
 40.639751, -73.778925, 'US', NOW(), NOW()),
('LAX', 'Los Angeles International Airport', 'Los Angeles', 'United States', 'America/Los_Angeles',
 33.942536, -118.408075, 'US', NOW(), NOW()),
('ORD', 'O''Hare International Airport', 'Chicago', 'United States', 'America/Chicago',
 41.978603, -87.904842, 'US', NOW(), NOW()),
('SFO', 'San Francisco International Airport', 'San Francisco', 'United States', 'America/Los_Angeles',
 37.618972, -122.374889, 'US', NOW(), NOW()),
('MIA', 'Miami International Airport', 'Miami', 'United States', 'America/New_York',
 25.795865, -80.287046, 'US', NOW(), NOW()),
('YYZ', 'Toronto Pearson International Airport', 'Toronto', 'Canada', 'America/Toronto',
 43.677717, -79.624819, 'CA', NOW(), NOW()),

-- ── Latin America ────────────────────────────────────────
('GRU', 'São Paulo/Guarulhos International Airport', 'São Paulo', 'Brazil', 'America/Sao_Paulo',
 -23.435556, -46.473056, 'BR', NOW(), NOW()),
('MEX', 'Mexico City International Airport', 'Mexico City', 'Mexico', 'America/Mexico_City',
 19.436303, -99.072096, 'MX', NOW(), NOW()),
('EZE', 'Ministro Pistarini International Airport', 'Buenos Aires', 'Argentina', 'America/Argentina/Buenos_Aires',
 -34.822222, -58.535833, 'AR', NOW(), NOW()),

-- ── Middle East ──────────────────────────────────────────
('DXB', 'Dubai International Airport', 'Dubai', 'United Arab Emirates', 'Asia/Dubai',
 25.253174, 55.365673, 'AE', NOW(), NOW()),
('DOH', 'Hamad International Airport', 'Doha', 'Qatar', 'Asia/Qatar',
 25.273056, 51.608056, 'QA', NOW(), NOW()),
('IST', 'Istanbul Airport', 'Istanbul', 'Türkiye', 'Europe/Istanbul',
 41.275278, 28.751944, 'TR', NOW(), NOW()),

-- ── Asia ─────────────────────────────────────────────────
('HND', 'Tokyo Haneda Airport', 'Tokyo', 'Japan', 'Asia/Tokyo',
 35.553333, 139.781111, 'JP', NOW(), NOW()),
('ICN', 'Incheon International Airport', 'Seoul', 'South Korea', 'Asia/Seoul',
 37.469075, 126.450517, 'KR', NOW(), NOW()),
('PVG', 'Shanghai Pudong International Airport', 'Shanghai', 'China', 'Asia/Shanghai',
 31.143333, 121.805278, 'CN', NOW(), NOW()),
('HKG', 'Hong Kong International Airport', 'Hong Kong', 'Hong Kong SAR China', 'Asia/Hong_Kong',
 22.308889, 113.914444, 'HK', NOW(), NOW()),
('SIN', 'Singapore Changi Airport', 'Singapore', 'Singapore', 'Asia/Singapore',
 1.350189, 103.994433, 'SG', NOW(), NOW()),
('BKK', 'Suvarnabhumi Airport', 'Bangkok', 'Thailand', 'Asia/Bangkok',
 13.689999, 100.750114, 'TH', NOW(), NOW()),
('DEL', 'Indira Gandhi International Airport', 'Delhi', 'India', 'Asia/Kolkata',
 28.556162, 77.100281, 'IN', NOW(), NOW()),

-- ── Oceania ──────────────────────────────────────────────
('SYD', 'Sydney Kingsford Smith Airport', 'Sydney', 'Australia', 'Australia/Sydney',
 -33.946111, 151.177222, 'AU', NOW(), NOW()),
('MEL', 'Melbourne Airport', 'Melbourne', 'Australia', 'Australia/Melbourne',
 -37.673333, 144.843333, 'AU', NOW(), NOW()),
('AKL', 'Auckland Airport', 'Auckland', 'New Zealand', 'Pacific/Auckland',
 -37.008056, 174.791667, 'NZ', NOW(), NOW()),

-- ── Africa ───────────────────────────────────────────────
('JNB', 'O. R. Tambo International Airport', 'Johannesburg', 'South Africa', 'Africa/Johannesburg',
 -26.139166, 28.246000, 'ZA', NOW(), NOW()),
('CAI', 'Cairo International Airport', 'Cairo', 'Egypt', 'Africa/Cairo',
 30.111944, 31.413611, 'EG', NOW(), NOW()),
('LOS', 'Murtala Muhammed International Airport', 'Lagos', 'Nigeria', 'Africa/Lagos',
 6.577222, 3.321111, 'NG', NOW(), NOW()),
('NBO', 'Jomo Kenyatta International Airport', 'Nairobi', 'Kenya', 'Africa/Nairobi',
 -1.319167, 36.927778, 'KE', NOW(), NOW()),

-- ── Additional Europe ────────────────────────────────────
('MUC', 'Munich Airport', 'Munich', 'Germany', 'Europe/Berlin',
 48.353783, 11.786086, 'DE', NOW(), NOW()),
('ZRH', 'Zurich Airport', 'Zurich', 'Switzerland', 'Europe/Zurich',
 47.464722, 8.549167, 'CH', NOW(), NOW()),
('VIE', 'Vienna International Airport', 'Vienna', 'Austria', 'Europe/Vienna',
 48.110278, 16.569722, 'AT', NOW(), NOW()),
('CPH', 'Copenhagen Airport', 'Copenhagen', 'Denmark', 'Europe/Copenhagen',
 55.617917, 12.655972, 'DK', NOW(), NOW()),
('ARN', 'Stockholm Arlanda Airport', 'Stockholm', 'Sweden', 'Europe/Stockholm',
 59.651944, 17.918611, 'SE', NOW(), NOW());


INSERT INTO airlines (code, name, logo_url, website, country, active, created_at, updated_at)
VALUES ('AA', 'American Airlines', NULL, 'https://www.aa.com', 'United States', true, NOW(), NOW()),
       ('DL', 'Delta Air Lines', NULL, 'https://www.delta.com', 'United States', true, NOW(), NOW()),
       ('UA', 'United Airlines', NULL, 'https://www.united.com', 'United States', true, NOW(), NOW()),
       ('AC', 'Air Canada', NULL, 'https://www.aircanada.com', 'Canada', true, NOW(), NOW()),
       ('LA', 'LATAM Airlines', NULL, 'https://www.latamairlines.com', 'Chile', true, NOW(), NOW()),
       ('EK', 'Emirates', NULL, 'https://www.emirates.com', 'United Arab Emirates', true, NOW(), NOW()),
       ('QR', 'Qatar Airways', NULL, 'https://www.qatarairways.com', 'Qatar', true, NOW(), NOW()),
       ('TK', 'Turkish Airlines', NULL, 'https://www.turkishairlines.com', 'Türkiye', true, NOW(), NOW()),
       ('NH', 'All Nippon Airways', NULL, 'https://www.ana.co.jp', 'Japan', true, NOW(), NOW()),
       ('KE', 'Korean Air', NULL, 'https://www.koreanair.com', 'South Korea', true, NOW(), NOW()),
       ('CA', 'Air China', NULL, 'https://www.airchina.com', 'China', true, NOW(), NOW()),
       ('CX', 'Cathay Pacific', NULL, 'https://www.cathaypacific.com', 'Hong Kong SAR China', true, NOW(), NOW()),
       ('SQ', 'Singapore Airlines', NULL, 'https://www.singaporeair.com', 'Singapore', true, NOW(), NOW()),
       ('TG', 'Thai Airways International', NULL, 'https://www.thaiairways.com', 'Thailand', true, NOW(), NOW()),
       ('AI', 'Air India', NULL, 'https://www.airindia.com', 'India', true, NOW(), NOW()),
       ('QF', 'Qantas', NULL, 'https://www.qantas.com', 'Australia', true, NOW(), NOW()),
       ('ET', 'Ethiopian Airlines', NULL, 'https://www.ethiopianairlines.com', 'Ethiopia', true, NOW(), NOW());
