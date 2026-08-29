CREATE TABLE IF NOT EXISTS itinerary_activities
(
    id
    UUID
    NOT
    NULL,
    itinerary_day_id
    UUID
    NOT
    NULL,
    title
    VARCHAR
(
    200
) NOT NULL,
    description TEXT,
    type VARCHAR
(
    30
) NOT NULL,
    start_time TIME,
    end_time TIME,
    location VARCHAR
(
    500
),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    sort_order INTEGER NOT NULL DEFAULT 0,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_itinerary_activities
    PRIMARY KEY
(
    id
),
    CONSTRAINT fk_itinerary_activities_day
    FOREIGN KEY
(
    itinerary_day_id
)
    REFERENCES itinerary_days
(
    id
)
    ON DELETE CASCADE
    );

CREATE INDEX IF NOT EXISTS idx_itinerary_activities_day_id
    ON itinerary_activities(itinerary_day_id);

CREATE INDEX IF NOT EXISTS idx_itinerary_activities_day_order
    ON itinerary_activities(itinerary_day_id, sort_order);