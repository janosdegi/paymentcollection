-- V3__idempotency_store.sql (corrected)
CREATE TABLE IF NOT EXISTS idempotency_record (
    id           UUID PRIMARY KEY,
    idem_key     VARCHAR(128)  NOT NULL,
    request_hash VARCHAR(64)   NOT NULL,
    payment_id   BIGINT        NULL,
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    status       VARCHAR(16)   NOT NULL DEFAULT 'CREATED',
    CONSTRAINT uk_idempotency_key UNIQUE (idem_key),
    CONSTRAINT chk_idem_status CHECK (status IN ('CREATED','COMPLETED'))
    );

ALTER TABLE idempotency_record
    ADD CONSTRAINT fk_idem_payment
        FOREIGN KEY (payment_id) REFERENCES payments(id)
            ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_idem_payment_id ON idempotency_record(payment_id);
CREATE INDEX IF NOT EXISTS idx_idem_created_at ON idempotency_record(created_at);