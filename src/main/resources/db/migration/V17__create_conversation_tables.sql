CREATE TABLE chat_messages
(
    id           UUID PRIMARY KEY,
    trip_id      UUID        NOT NULL,
    sender_id    UUID        NOT NULL,

    content      TEXT        NOT NULL,

    message_type VARCHAR(20) NOT NULL,

    reply_to_id  UUID NULL,

    deleted      BOOLEAN     NOT NULL DEFAULT FALSE,

    created_at   TIMESTAMP   NOT NULL,
    updated_at   TIMESTAMP   NOT NULL,

    CONSTRAINT fk_chat_messages_trip
        FOREIGN KEY (trip_id)
            REFERENCES trips (id),

    CONSTRAINT fk_chat_messages_reply
        FOREIGN KEY (reply_to_id)
            REFERENCES chat_messages (id)
);

CREATE INDEX idx_chat_messages_trip_created
    ON chat_messages(trip_id, created_at DESC);

CREATE INDEX idx_chat_messages_trip_id
    ON chat_messages(trip_id, id);

CREATE INDEX idx_chat_messages_reply_to
    ON chat_messages(reply_to_id);

CREATE TABLE chat_attachments
(
    id         UUID PRIMARY KEY,

    message_id UUID         NOT NULL,

    name       VARCHAR(500) NOT NULL,
    url        TEXT         NOT NULL,
    mime_type  VARCHAR(100) NOT NULL,
    file_size  BIGINT       NOT NULL,

    created_at TIMESTAMP    NOT NULL,

    CONSTRAINT fk_chat_attachments_message
        FOREIGN KEY (message_id)
            REFERENCES chat_messages (id)
            ON DELETE CASCADE
);

CREATE INDEX idx_chat_attachments_message
    ON chat_attachments(message_id);