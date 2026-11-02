-- Store SHA-256 hex digests (64 chars) instead of raw rate keys (long usernames exceeded VARCHAR(512)).
DELETE FROM login_rate_limit;

ALTER TABLE login_rate_limit
    MODIFY rate_key VARCHAR(64) NOT NULL;
