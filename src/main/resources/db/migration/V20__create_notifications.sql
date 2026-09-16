CREATE TABLE notifications
(
    id UUID PRIMARY KEY,

    recipient_user_id UUID NOT NULL,
    actor_user_id UUID,

    trip_id UUID,

    category VARCHAR(30) NOT NULL,
    type VARCHAR(50) NOT NULL,

    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,

    target_type VARCHAR(30),
    target_id UUID,
    target_name VARCHAR(500),

    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    is_archived BOOLEAN NOT NULL DEFAULT FALSE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,

    snoozed_until TIMESTAMP,

    created_at TIMESTAMP NOT NULL,
    read_at TIMESTAMP,
    archived_at TIMESTAMP,
    deleted_at TIMESTAMP,

    CONSTRAINT fk_notifications_recipient
        FOREIGN KEY (recipient_user_id)
            REFERENCES users(id),

    CONSTRAINT fk_notifications_actor
        FOREIGN KEY (actor_user_id)
            REFERENCES users(id),

    CONSTRAINT fk_notifications_trip
        FOREIGN KEY (trip_id)
            REFERENCES trips(id)
);

CREATE INDEX idx_notifications_recipient_created
    ON notifications(recipient_user_id, created_at DESC);

CREATE INDEX idx_notifications_recipient_unread
    ON notifications(recipient_user_id, is_read, created_at DESC);

CREATE INDEX idx_notifications_recipient_archived
    ON notifications(recipient_user_id, is_archived, created_at DESC);

CREATE INDEX idx_notifications_trip
    ON notifications(trip_id, created_at DESC);

CREATE INDEX idx_notifications_target
    ON notifications(target_type, target_id);


CREATE TABLE notification_preferences
(
    id UUID PRIMARY KEY,

    user_id UUID NOT NULL UNIQUE,

    push_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    email_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    in_app_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sms_enabled BOOLEAN NOT NULL DEFAULT FALSE,

    digest_enabled BOOLEAN NOT NULL DEFAULT FALSE,

    quiet_hours_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    quiet_hours_start TIME,
    quiet_hours_end TIME,

    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_notification_preferences_user
        FOREIGN KEY (user_id)
            REFERENCES users(id)
);


CREATE TABLE reminders
(
    id UUID PRIMARY KEY,

    user_id UUID NOT NULL,
    trip_id UUID,

    title VARCHAR(255) NOT NULL,
    description TEXT,

    due_at TIMESTAMP NOT NULL,

    status VARCHAR(20) NOT NULL DEFAULT 'UPCOMING',

    snoozed_until TIMESTAMP,

    recurring BOOLEAN NOT NULL DEFAULT FALSE,
    recurrence_rule VARCHAR(255),

    completed_at TIMESTAMP,

    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_reminders_user
        FOREIGN KEY (user_id)
            REFERENCES users(id),

    CONSTRAINT fk_reminders_trip
        FOREIGN KEY (trip_id)
            REFERENCES trips(id)
);

CREATE INDEX idx_reminders_user_due
    ON reminders(user_id, due_at);

CREATE INDEX idx_reminders_user_status
    ON reminders(user_id, status, due_at);

CREATE INDEX idx_reminders_trip
    ON reminders(trip_id, due_at);