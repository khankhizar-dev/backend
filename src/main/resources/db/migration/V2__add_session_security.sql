CREATE TABLE token_blacklist (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token_id VARCHAR(500) NOT NULL UNIQUE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    blacklisted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_token_blacklist_expires_at ON token_blacklist(expires_at);

ALTER TABLE users ADD COLUMN tokens_valid_after TIMESTAMPTZ;
ALTER TABLE refresh_tokens ADD COLUMN device_id UUID REFERENCES user_devices(id) ON DELETE SET NULL;
