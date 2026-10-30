-- Add invite tokens for mystery shopper registration
CREATE TABLE invite_tokens (
    id BINARY(16) PRIMARY KEY,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    created_by_user_id BINARY(16) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP NULL,
    used_by_user_id BINARY(16) NULL,
    FOREIGN KEY (created_by_user_id) REFERENCES users(id),
    FOREIGN KEY (used_by_user_id) REFERENCES users(id)
);

CREATE INDEX idx_invite_tokens_token_hash ON invite_tokens(token_hash);
CREATE INDEX idx_invite_tokens_expires_at ON invite_tokens(expires_at);
