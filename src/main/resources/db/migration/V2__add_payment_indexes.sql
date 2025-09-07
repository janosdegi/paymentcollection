CREATE TABLE payments (
    id           BIGSERIAL PRIMARY KEY,
    amount       NUMERIC(19,2) NOT NULL CHECK (amount > 0),
    currency     VARCHAR(3)    NOT NULL,                 -- ISO 4217 (e.g., "EUR")
    status       VARCHAR(32)   NOT NULL,
    method       VARCHAR(32)   NOT NULL,
    provider_ref VARCHAR(64),
    customer_id  VARCHAR(64),
    created_at   TIMESTAMPTZ   NOT NULL,                 -- app-managed in @PrePersist
    updated_at   TIMESTAMPTZ   NOT NULL,                 -- app-managed in @PreUpdate
    metadata     JSONB
);

-- Indexes for typical lookups/sorting
CREATE INDEX idx_payments_customer_id ON payments (customer_id);
CREATE INDEX idx_payments_status      ON payments (status);
CREATE INDEX idx_payments_created_at  ON payments (created_at);