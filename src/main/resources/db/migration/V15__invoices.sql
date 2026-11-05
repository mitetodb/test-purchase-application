-- V15: Client invoices and line items (#12). Bookkeeping records only; no payment provider integration.

CREATE TABLE invoices (
    id BINARY(16) NOT NULL,
    number VARCHAR(20) NOT NULL,
    customer_id BINARY(16) NOT NULL,
    status VARCHAR(32) NOT NULL,
    period_year INT NOT NULL,
    period_month INT NOT NULL,
    currency VARCHAR(8) NOT NULL,
    total_amount DOUBLE NOT NULL,
    notes VARCHAR(2000),
    issued_at DATETIME(6),
    paid_at DATETIME(6),
    cancelled_at DATETIME(6),
    created_on DATETIME(6),
    updated_on DATETIME(6),
    updated_by_user VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT uk_invoices_number UNIQUE (number),
    CONSTRAINT fk_invoices_customer FOREIGN KEY (customer_id) REFERENCES customers (id)
) ENGINE=InnoDB;

CREATE INDEX idx_invoices_customer_period ON invoices (customer_id, period_year, period_month);
CREATE INDEX idx_invoices_status ON invoices (status);

CREATE TABLE invoice_line_items (
    id BINARY(16) NOT NULL,
    invoice_id BINARY(16) NOT NULL,
    test_purchase_id BINARY(16),
    line_type VARCHAR(32) NOT NULL,
    description VARCHAR(500) NOT NULL,
    quantity INT NOT NULL,
    unit_amount DOUBLE NOT NULL,
    line_total DOUBLE NOT NULL,
    sort_order INT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_invoice_line_invoice FOREIGN KEY (invoice_id) REFERENCES invoices (id),
    CONSTRAINT fk_invoice_line_purchase FOREIGN KEY (test_purchase_id) REFERENCES test_purchases (id)
) ENGINE=InnoDB;

CREATE UNIQUE INDEX uk_invoice_line_test_purchase ON invoice_line_items (test_purchase_id);

INSERT INTO entity_number_sequence (sequence_name, last_used_value)
VALUES ('INVOICE', 0);
