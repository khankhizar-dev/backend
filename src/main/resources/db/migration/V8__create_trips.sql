CREATE TABLE trips
(
    id          UUID         NOT NULL,
    owner_id    UUID         NOT NULL,
    name        VARCHAR(200) NOT NULL,
    destination VARCHAR(255) NOT NULL,
    start_date  DATE         NOT NULL,
    end_date    DATE         NOT NULL,
    status      VARCHAR(30)  NOT NULL,
    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP    NOT NULL,

    CONSTRAINT pk_trips
        PRIMARY KEY (id),

    CONSTRAINT fk_trips_owner
        FOREIGN KEY (owner_id)
            REFERENCES users (id),

    CONSTRAINT chk_trips_date_range
        CHECK (end_date >= start_date)
);

CREATE INDEX idx_trips_owner_id
    ON trips(owner_id);

CREATE INDEX idx_trips_owner_status
    ON trips(owner_id, status);

CREATE INDEX idx_trips_start_date
    ON trips(start_date);