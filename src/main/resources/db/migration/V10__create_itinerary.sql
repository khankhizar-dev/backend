CREATE TABLE itinerary_days
(
    id         UUID      NOT NULL,
    trip_id    UUID      NOT NULL,
    day_number INTEGER   NOT NULL,
    date       DATE      NOT NULL,
    title      VARCHAR(200),
    notes      TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT pk_itinerary_days
        PRIMARY KEY (id),

    CONSTRAINT fk_itinerary_days_trip
        FOREIGN KEY (trip_id)
            REFERENCES trips (id)
            ON DELETE CASCADE,

    CONSTRAINT uk_itinerary_days_trip_day
        UNIQUE (trip_id, day_number),

    CONSTRAINT uk_itinerary_days_trip_date
        UNIQUE (trip_id, date)
);

CREATE INDEX idx_itinerary_days_trip_id
    ON itinerary_days(trip_id);


CREATE TABLE itinerary_activities
(
    id               UUID         NOT NULL,
    itinerary_day_id UUID         NOT NULL,
    title            VARCHAR(200) NOT NULL,
    description      TEXT,
    type             VARCHAR(30)  NOT NULL,
    start_time       TIME,
    end_time         TIME,
    location         VARCHAR(500),
    latitude         DOUBLE PRECISION,
    longitude        DOUBLE PRECISION,
    sort_order       INTEGER      NOT NULL DEFAULT 0,
    completed        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMP    NOT NULL,
    updated_at       TIMESTAMP    NOT NULL,

    CONSTRAINT pk_itinerary_activities
        PRIMARY KEY (id),

    CONSTRAINT fk_itinerary_activities_day
        FOREIGN KEY (itinerary_day_id)
            REFERENCES itinerary_days (id)
            ON DELETE CASCADE
);

CREATE INDEX idx_itinerary_activities_day_id
    ON itinerary_activities(itinerary_day_id);

CREATE INDEX idx_itinerary_activities_day_order
    ON itinerary_activities(itinerary_day_id, sort_order);