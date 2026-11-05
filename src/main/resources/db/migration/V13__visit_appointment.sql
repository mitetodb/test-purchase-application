-- V13: Shop visit appointments linked to test purchases (issue #24).

ALTER TABLE test_purchases
    ADD COLUMN visit_appointment_at DATETIME NULL,
    ADD COLUMN visit_appointment_location VARCHAR(500) NULL,
    ADD COLUMN visit_appointment_notes VARCHAR(2000) NULL,
    ADD COLUMN visit_appointment_confirmed BIT NOT NULL DEFAULT 0;

CREATE INDEX idx_test_purchases_visit_appointment_at ON test_purchases (visit_appointment_at);
