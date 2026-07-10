-- ============================================================
-- V1 — Initial schema
-- Creates all tables for the Airline Booking System
-- ============================================================

-- Users table
CREATE TABLE users
(
    id                      BIGSERIAL PRIMARY KEY,
    email                   VARCHAR(255) NOT NULL UNIQUE,
    password                VARCHAR(255) NOT NULL,
    first_name              VARCHAR(255) NOT NULL,
    last_name               VARCHAR(255) NOT NULL,
    phone_number            VARCHAR(50),
    address                 VARCHAR(255),
    city                    VARCHAR(100),
    country                 VARCHAR(100),
    postal_code             VARCHAR(20),
    role                    VARCHAR(20)  NOT NULL DEFAULT 'CUSTOMER',
    enabled                 BOOLEAN      NOT NULL DEFAULT TRUE,
    account_non_expired     BOOLEAN      NOT NULL DEFAULT TRUE,
    account_non_locked      BOOLEAN      NOT NULL DEFAULT TRUE,
    credentials_non_expired BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMP,
    updated_at              TIMESTAMP,

    CONSTRAINT chk_users_role CHECK (role IN ('CUSTOMER', 'ADMIN', 'AIRLINE_STAFF'))
);

-- Airlines table
CREATE TABLE airlines
(
    id         BIGSERIAL PRIMARY KEY,
    code       VARCHAR(2)   NOT NULL UNIQUE,
    name       VARCHAR(255) NOT NULL,
    logo_url   VARCHAR(500),
    website    VARCHAR(500),
    country    VARCHAR(100) NOT NULL,
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- Airports table
CREATE TABLE airports
(
    id         BIGSERIAL PRIMARY KEY,
    code       VARCHAR(3)   NOT NULL UNIQUE,
    name       VARCHAR(255) NOT NULL,
    city       VARCHAR(100) NOT NULL,
    country    VARCHAR(100) NOT NULL,
    timezone   VARCHAR(100),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- Flights table
CREATE TABLE flights
(
    id                   BIGSERIAL PRIMARY KEY,
    flight_number        VARCHAR(20)    NOT NULL UNIQUE,
    airline_id           BIGINT         NOT NULL,
    departure_airport_id BIGINT         NOT NULL,
    arrival_airport_id   BIGINT         NOT NULL,
    departure_time       TIMESTAMP      NOT NULL,
    arrival_time         TIMESTAMP      NOT NULL,
    duration             INTEGER        NOT NULL,
    base_price           NUMERIC(10, 2) NOT NULL,
    total_seats          INTEGER        NOT NULL,
    available_seats      INTEGER        NOT NULL,
    economy_seats        INTEGER        NOT NULL DEFAULT 0,
    business_seats       INTEGER        NOT NULL DEFAULT 0,
    first_class_seats    INTEGER        NOT NULL DEFAULT 0,
    economy_price        NUMERIC(10, 2),
    business_price       NUMERIC(10, 2),
    first_class_price    NUMERIC(10, 2),
    status               VARCHAR(20)    NOT NULL DEFAULT 'SCHEDULED',
    active               BOOLEAN        NOT NULL DEFAULT TRUE,
    aircraft             VARCHAR(100),
    created_at           TIMESTAMP,
    updated_at           TIMESTAMP,

    CONSTRAINT fk_flights_airline
        FOREIGN KEY (airline_id) REFERENCES airlines (id),
    CONSTRAINT fk_flights_departure_airport
        FOREIGN KEY (departure_airport_id) REFERENCES airports (id),
    CONSTRAINT fk_flights_arrival_airport
        FOREIGN KEY (arrival_airport_id) REFERENCES airports (id),
    CONSTRAINT chk_flights_status
        CHECK (status IN ('SCHEDULED', 'DELAYED', 'CANCELLED', 'BOARDING', 'DEPARTED', 'ARRIVED')),
    CONSTRAINT chk_flights_seats
        CHECK (available_seats >= 0 AND total_seats >= 0)
);

-- Bookings table
CREATE TABLE bookings
(
    id                   BIGSERIAL PRIMARY KEY,
    booking_reference    VARCHAR(50)    NOT NULL UNIQUE,
    user_id              BIGINT         NOT NULL,
    flight_id            BIGINT         NOT NULL,
    number_of_passengers INTEGER        NOT NULL,
    total_amount         NUMERIC(10, 2) NOT NULL,
    status               VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    seat_class           VARCHAR(20)    NOT NULL,
    created_at           TIMESTAMP,
    updated_at           TIMESTAMP,

    CONSTRAINT fk_bookings_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_bookings_flight
        FOREIGN KEY (flight_id) REFERENCES flights (id),
    CONSTRAINT chk_bookings_status
        CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED', 'COMPLETED')),
    CONSTRAINT chk_bookings_seat_class
        CHECK (seat_class IN ('ECONOMY', 'BUSINESS', 'FIRST'))
);

-- Passengers table
CREATE TABLE passengers
(
    id              BIGSERIAL PRIMARY KEY,
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    date_of_birth   DATE         NOT NULL,
    gender          VARCHAR(10)  NOT NULL,
    passport_number VARCHAR(50)  NOT NULL,
    nationality     VARCHAR(100) NOT NULL,
    seat_number     VARCHAR(10),
    passenger_type  VARCHAR(10)  NOT NULL DEFAULT 'ADULT',
    booking_id      BIGINT       NOT NULL,
    created_at      TIMESTAMP,
    updated_at      TIMESTAMP,

    CONSTRAINT fk_passengers_booking
        FOREIGN KEY (booking_id) REFERENCES bookings (id),
    CONSTRAINT chk_passengers_gender
        CHECK (gender IN ('MALE', 'FEMALE', 'OTHER')),
    CONSTRAINT chk_passengers_type
        CHECK (passenger_type IN ('ADULT', 'CHILD', 'INFANT'))
);

-- Payments table
CREATE TABLE payments
(
    id                       BIGSERIAL PRIMARY KEY,
    transaction_id           VARCHAR(100)   NOT NULL UNIQUE,
    booking_id               BIGINT         NOT NULL UNIQUE,
    amount                   NUMERIC(10, 2) NOT NULL,
    payment_method           VARCHAR(20)    NOT NULL,
    status                   VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    payment_gateway_response TEXT,
    created_at               TIMESTAMP,
    updated_at               TIMESTAMP,

    CONSTRAINT fk_payments_booking
        FOREIGN KEY (booking_id) REFERENCES bookings (id),
    CONSTRAINT chk_payments_method
        CHECK (payment_method IN ('CREDIT_CARD', 'DEBIT_CARD', 'PAYPAL', 'BANK_TRANSFER')),
    CONSTRAINT chk_payments_status
        CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED', 'REFUNDED'))
);