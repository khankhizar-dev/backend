CREATE TABLE bookings
(
    id                UUID         NOT NULL,
    trip_id           UUID         NOT NULL,
    itinerary_day_id  UUID,
    created_by        UUID         NOT NULL,

    type              VARCHAR(30)  NOT NULL,
    status            VARCHAR(30)  NOT NULL DEFAULT 'PENDING',

    title             VARCHAR(200) NOT NULL,
    provider          VARCHAR(200),
    booking_reference VARCHAR(100),

    start_at          TIMESTAMP,
    end_at            TIMESTAMP,

    location          VARCHAR(500),

    amount            NUMERIC(12, 2),
    currency          VARCHAR(3),

    source            VARCHAR(30)  NOT NULL DEFAULT 'MANUAL',

    notes             TEXT,

    details           JSONB,

    created_at        TIMESTAMP    NOT NULL,
    updated_at        TIMESTAMP    NOT NULL,

    CONSTRAINT pk_bookings
        PRIMARY KEY (id),

    CONSTRAINT fk_bookings_trip
        FOREIGN KEY (trip_id)
            REFERENCES trips (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_bookings_itinerary_day
        FOREIGN KEY (itinerary_day_id)
            REFERENCES itinerary_days (id)
            ON DELETE SET NULL
);

CREATE INDEX idx_bookings_trip_id
    ON bookings(trip_id);

CREATE INDEX idx_bookings_trip_status
    ON bookings(trip_id, status);

CREATE INDEX idx_bookings_trip_type
    ON bookings(trip_id, type);

CREATE INDEX idx_bookings_reference
    ON bookings(booking_reference);

CREATE INDEX idx_bookings_start_at
    ON bookings(start_at);

CREATE TABLE booking_travellers
(
    id            UUID         NOT NULL,
    booking_id    UUID         NOT NULL,

    first_name    VARCHAR(100) NOT NULL,
    last_name     VARCHAR(100),

    email         VARCHAR(255),
    phone_number  VARCHAR(30),

    date_of_birth DATE,

    ticket_number VARCHAR(100),
    seat_number   VARCHAR(30),

    created_at    TIMESTAMP    NOT NULL,
    updated_at    TIMESTAMP    NOT NULL,

    CONSTRAINT pk_booking_travellers
        PRIMARY KEY (id),

    CONSTRAINT fk_booking_travellers_booking
        FOREIGN KEY (booking_id)
            REFERENCES bookings (id)
            ON DELETE CASCADE
);

CREATE INDEX idx_booking_travellers_booking_id
    ON booking_travellers(booking_id);

CREATE TABLE booking_events
(
    id          UUID        NOT NULL,
    booking_id  UUID        NOT NULL,

    event_type  VARCHAR(50) NOT NULL,
    description VARCHAR(500),

    metadata    JSONB,

    created_by  UUID,

    created_at  TIMESTAMP   NOT NULL,

    CONSTRAINT pk_booking_events
        PRIMARY KEY (id),

    CONSTRAINT fk_booking_events_booking
        FOREIGN KEY (booking_id)
            REFERENCES bookings (id)
            ON DELETE CASCADE
);

CREATE INDEX idx_booking_events_booking_id
    ON booking_events(booking_id);

CREATE INDEX idx_booking_events_booking_created
    ON booking_events(booking_id, created_at);