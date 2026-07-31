CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE users (
                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                       email VARCHAR(255) NOT NULL UNIQUE,
                       password_hash TEXT NOT NULL,

                       first_name VARCHAR(100),
                       last_name VARCHAR(100),

                       profile_image_url TEXT,

                       is_email_verified BOOLEAN NOT NULL DEFAULT FALSE,
                       is_active BOOLEAN NOT NULL DEFAULT TRUE,

                       created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                       updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                       deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_users_email
    ON users(email);

CREATE TABLE refresh_tokens (

                                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                user_id UUID NOT NULL
                                    REFERENCES users(id)
                                        ON DELETE CASCADE,

                                token_hash TEXT NOT NULL,

                                expires_at TIMESTAMPTZ NOT NULL,

                                created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                                revoked_at TIMESTAMPTZ
);

CREATE INDEX idx_refresh_user
    ON refresh_tokens(user_id);

CREATE TABLE user_devices (

                              id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                              user_id UUID NOT NULL
                                  REFERENCES users(id)
                                      ON DELETE CASCADE,

                              device_name VARCHAR(255),

                              platform VARCHAR(30),

                              app_version VARCHAR(30),

                              last_login_at TIMESTAMPTZ,

                              created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_devices_user
    ON user_devices(user_id);