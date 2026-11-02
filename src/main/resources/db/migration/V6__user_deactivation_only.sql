-- V6: Users are deactivated via active flag only (never hard-deleted).
-- Index supports admin user list filtering by active status.

CREATE INDEX idx_users_active ON users (active);
