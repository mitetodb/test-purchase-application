-- V2: Add RBAC schema - customer-user link, sales-manager ownership, active flags for soft-delete

-- Add customer_id to users table (for CUSTOMER role users)
ALTER TABLE users ADD COLUMN customer_id BINARY(16) NULL;

-- Add foreign key constraint
ALTER TABLE users 
    ADD CONSTRAINT FK_users_customer_id 
    FOREIGN KEY (customer_id) 
    REFERENCES customers (id);

-- Add index for performance
CREATE INDEX idx_users_customer_id ON users(customer_id);

-- Add sales_manager_id to customers table (for ownership)
ALTER TABLE customers ADD COLUMN sales_manager_id BINARY(16) NULL;

-- Add foreign key constraint
ALTER TABLE customers 
    ADD CONSTRAINT FK_customers_sales_manager_id 
    FOREIGN KEY (sales_manager_id) 
    REFERENCES users (id);

-- Add index for performance
CREATE INDEX idx_customers_sales_manager_id ON customers(sales_manager_id);

-- Add active flag to customers (default TRUE for existing records)
ALTER TABLE customers ADD COLUMN active BIT NOT NULL DEFAULT 1;

-- Add active flag to test_purchases (default TRUE for existing records)
ALTER TABLE test_purchases ADD COLUMN active BIT NOT NULL DEFAULT 1;

-- Note: users table already has an 'active' column from V1
