CREATE TABLE dead_letter (
    id BIGSERIAL PRIMARY KEY,
    outbox_id BIGINT NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    error_message TEXT NOT NULL,
    failed_at TIMESTAMP DEFAULT now()
);

CREATE INDEX idx_dead_letter_failed_at ON dead_letter(failed_at);