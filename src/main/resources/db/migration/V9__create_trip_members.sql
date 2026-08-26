CREATE TABLE trip_members (
                              id UUID NOT NULL,
                              trip_id UUID NOT NULL,
                              user_id UUID NOT NULL,
                              role VARCHAR(20) NOT NULL,
                              status VARCHAR(20) NOT NULL,
                              invited_at TIMESTAMP NOT NULL,
                              joined_at TIMESTAMP,

                              CONSTRAINT pk_trip_members
                                  PRIMARY KEY (id),

                              CONSTRAINT fk_trip_members_trip
                                  FOREIGN KEY (trip_id)
                                      REFERENCES trips(id)
                                      ON DELETE CASCADE,

                              CONSTRAINT fk_trip_members_user
                                  FOREIGN KEY (user_id)
                                      REFERENCES users(id)
                                      ON DELETE CASCADE,

                              CONSTRAINT uk_trip_members_trip_user
                                  UNIQUE (trip_id, user_id)
);

CREATE INDEX idx_trip_members_trip_id
    ON trip_members(trip_id);

CREATE INDEX idx_trip_members_user_id
    ON trip_members(user_id);

CREATE INDEX idx_trip_members_trip_status
    ON trip_members(trip_id, status);