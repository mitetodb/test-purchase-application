-- V8: Concurrency-safe sequential numbers for customers, shops, and test purchases (#23).
-- Open PR #48 adds V7 (TOTP); this migration is V8 so both can land independently.
-- Unique constraints on number columns already exist in V1 (safety net for duplicates).

CREATE TABLE entity_number_sequence (
    sequence_name VARCHAR(64) NOT NULL,
    last_used_value BIGINT NOT NULL,
    PRIMARY KEY (sequence_name)
) ENGINE=InnoDB;

INSERT INTO entity_number_sequence (sequence_name, last_used_value)
VALUES (
    'CUSTOMER',
    COALESCE((SELECT MAX(CAST(number AS UNSIGNED)) FROM customers), 0)
);

INSERT INTO entity_number_sequence (sequence_name, last_used_value)
VALUES (
    'SHOP',
    COALESCE((SELECT MAX(CAST(number AS UNSIGNED)) FROM shops), 0)
);

INSERT INTO entity_number_sequence (sequence_name, last_used_value)
VALUES (
    'TEST_PURCHASE',
    COALESCE((SELECT MAX(CAST(SUBSTRING(number, 4) AS UNSIGNED)) FROM test_purchases), 1000)
);
