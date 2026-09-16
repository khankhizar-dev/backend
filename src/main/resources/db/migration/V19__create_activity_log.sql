CREATE TABLE IF NOT EXISTS activity_logs
(
    id          UUID PRIMARY KEY,
    trip_id     UUID         NOT NULL,
    user_id     UUID         NOT NULL,
    action      VARCHAR(255) NOT NULL,
    target_type VARCHAR(30)  NOT NULL,
    target_name VARCHAR(500) NOT NULL,
    created_at  TIMESTAMP    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_activity_logs_trip_created
    ON activity_logs(trip_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_activity_logs_trip_target
    ON activity_logs(trip_id, target_type);
