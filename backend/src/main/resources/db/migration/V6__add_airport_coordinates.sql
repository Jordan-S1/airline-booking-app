-- ============================================================
-- V6: Airport coordinates
-- ------------------------------------------------------------
-- Needed to draw the origin → destination route map. Values are
-- the published reference coordinates for each airport, in
-- decimal degrees (WGS 84).
-- ============================================================

ALTER TABLE airports
    ADD COLUMN latitude  NUMERIC(9, 6),
    ADD COLUMN longitude NUMERIC(9, 6);

UPDATE airports SET latitude = 53.421333, longitude = -6.270075  WHERE code = 'DUB';
UPDATE airports SET latitude = 51.470020, longitude = -0.454295  WHERE code = 'LHR';
UPDATE airports SET latitude = 49.009724, longitude = 2.547778   WHERE code = 'CDG';
UPDATE airports SET latitude = 52.308600, longitude = 4.763890   WHERE code = 'AMS';
UPDATE airports SET latitude = 50.037933, longitude = 8.562152   WHERE code = 'FRA';
UPDATE airports SET latitude = 40.471926, longitude = -3.562640  WHERE code = 'MAD';
UPDATE airports SET latitude = 41.800277, longitude = 12.238889  WHERE code = 'FCO';
UPDATE airports SET latitude = 41.297078, longitude = 2.078464   WHERE code = 'BCN';
UPDATE airports SET latitude = 38.774167, longitude = -9.134167  WHERE code = 'LIS';
UPDATE airports SET latitude = 51.289453, longitude = 6.766775   WHERE code = 'DUS';
UPDATE airports SET latitude = 53.365000, longitude = -2.272500  WHERE code = 'MAN';
UPDATE airports SET latitude = 48.723333, longitude = 2.379444   WHERE code = 'ORY';
