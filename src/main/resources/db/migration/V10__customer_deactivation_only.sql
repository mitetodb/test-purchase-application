-- V10: Customers are deactivated via active flag only (never hard-deleted).
-- Index supports admin customer list filtering by active status.

CREATE INDEX idx_customers_active ON customers (active);
