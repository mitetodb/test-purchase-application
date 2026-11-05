ALTER TABLE test_purchases
    ADD COLUMN deadline_at DATE NULL;

CREATE TABLE test_purchase_deadline_reminders (
    id BINARY(16) NOT NULL,
    test_purchase_id BINARY(16) NOT NULL,
    days_before_offset INT NOT NULL,
    sent_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_tp_deadline_reminder UNIQUE (test_purchase_id, days_before_offset),
    CONSTRAINT fk_tp_deadline_reminder_purchase FOREIGN KEY (test_purchase_id) REFERENCES test_purchases (id)
) ENGINE=InnoDB;
