-- Append-only audit log (issue #13). V7/V8 are reserved by open PRs (TOTP, entity sequences).
CREATE TABLE audit_log (
    id BINARY(16) NOT NULL,
    occurred_at DATETIME(6) NOT NULL,
    actor_user_id BINARY(16) NULL,
    action VARCHAR(64) NOT NULL,
    entity_type VARCHAR(32) NOT NULL,
    entity_id VARCHAR(64) NULL,
    client_ip VARCHAR(45) NULL,
    change_summary VARCHAR(2000) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_audit_log_occurred_at (occurred_at),
    INDEX idx_audit_log_action (action),
    INDEX idx_audit_log_entity_type (entity_type),
    INDEX idx_audit_log_actor_user_id (actor_user_id)
) ENGINE=InnoDB;
