-- V11: Shops deactivate via active flag; test purchases archive via ARCHIVED status (never hard-deleted).

ALTER TABLE shops ADD COLUMN active BIT NOT NULL DEFAULT 1;
CREATE INDEX idx_shops_active ON shops (active);

ALTER TABLE test_purchases
    MODIFY COLUMN status ENUM (
        'ARCHIVED',
        'CANCELLED',
        'CLOSED',
        'INITIALISED',
        'IN_PROGRESS',
        'PRODUCT_FORWARDED_TO_CLIENT',
        'PRODUCT_ORDERED',
        'PRODUCT_RECEIVED',
        'PRODUCT_SHIPPED_BACK_TO_SELLER'
    );

ALTER TABLE testpurchase_status_history
    MODIFY COLUMN old_status ENUM (
        'ARCHIVED',
        'CANCELLED',
        'CLOSED',
        'INITIALISED',
        'IN_PROGRESS',
        'PRODUCT_FORWARDED_TO_CLIENT',
        'PRODUCT_ORDERED',
        'PRODUCT_RECEIVED',
        'PRODUCT_SHIPPED_BACK_TO_SELLER'
    ) NOT NULL;

ALTER TABLE testpurchase_status_history
    MODIFY COLUMN new_status ENUM (
        'ARCHIVED',
        'CANCELLED',
        'CLOSED',
        'INITIALISED',
        'IN_PROGRESS',
        'PRODUCT_FORWARDED_TO_CLIENT',
        'PRODUCT_ORDERED',
        'PRODUCT_RECEIVED',
        'PRODUCT_SHIPPED_BACK_TO_SELLER'
    ) NOT NULL;

-- Legacy soft-deletes (active = 0) become ARCHIVED rows kept for audit.
UPDATE test_purchases
SET status = 'ARCHIVED', active = 1
WHERE active = 0;
