ALTER TABLE user_totp_recovery_code
    ADD COLUMN invalidated_at DATETIME(6) NULL,
    ADD COLUMN invalidation_reason VARCHAR(64) NULL;
