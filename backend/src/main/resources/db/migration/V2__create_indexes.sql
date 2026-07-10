-- ============================================================
-- V2 — Performance indexes
-- Adds indexes on commonly queried columns
-- ============================================================

-- Users
CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_role ON users (role);

-- Airlines
CREATE INDEX idx_airlines_code ON airlines (code);
CREATE INDEX idx_airlines_country ON airlines (country);
CREATE INDEX idx_airlines_active ON airlines (active);

-- Airports
CREATE INDEX idx_airports_code ON airports (code);
CREATE INDEX idx_airports_country ON airports (country);
CREATE INDEX idx_airports_city ON airports (city);

-- Flights
CREATE INDEX idx_flights_flight_number ON flights (flight_number);
CREATE INDEX idx_flights_airline_id ON flights (airline_id);
CREATE INDEX idx_flights_departure_airport ON flights (departure_airport_id);
CREATE INDEX idx_flights_arrival_airport ON flights (arrival_airport_id);
CREATE INDEX idx_flights_departure_time ON flights (departure_time);
CREATE INDEX idx_flights_status ON flights (status);
CREATE INDEX idx_flights_active ON flights (active);

-- Composite index for the most common search query
CREATE INDEX idx_flights_search
    ON flights (departure_airport_id, arrival_airport_id, departure_time, status, active);

-- Bookings
CREATE INDEX idx_bookings_reference ON bookings (booking_reference);
CREATE INDEX idx_bookings_user_id ON bookings (user_id);
CREATE INDEX idx_bookings_flight_id ON bookings (flight_id);
CREATE INDEX idx_bookings_status ON bookings (status);

-- Passengers
CREATE INDEX idx_passengers_booking_id ON passengers (booking_id);
CREATE INDEX idx_passengers_passport ON passengers (passport_number);

-- Payments
CREATE INDEX idx_payments_transaction_id ON payments (transaction_id);
CREATE INDEX idx_payments_booking_id ON payments (booking_id);
CREATE INDEX idx_payments_status ON payments (status);
CREATE INDEX idx_payments_created_at ON payments (created_at);