ALTER TABLE users
    ADD COLUMN failed_login_attempts INT NOT NULL DEFAULT 0,
    ADD COLUMN locked_until DATETIME(6) NULL;

CREATE TABLE login_rate_limit (
    id BINARY(16) NOT NULL,
    rate_key VARCHAR(512) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    window_start DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_login_rate_limit_key UNIQUE (rate_key)
) ENGINE=InnoDB;
