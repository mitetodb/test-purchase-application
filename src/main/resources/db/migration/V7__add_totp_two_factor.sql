ALTER TABLE users
    ADD COLUMN totp_secret_encrypted VARCHAR(512) NULL,
    ADD COLUMN totp_enabled TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN totp_last_time_step BIGINT NULL;

CREATE TABLE user_totp_recovery_code (
    id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    code_hash VARCHAR(255) NOT NULL,
    used_at DATETIME(6) NULL,
    created_on DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_totp_recovery_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_totp_recovery_user ON user_totp_recovery_code (user_id);
