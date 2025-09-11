CREATE TABLE outbox (
                        id BIGSERIAL PRIMARY KEY,
                        aggregate_type VARCHAR(100) NOT NULL,
                        aggregate_id VARCHAR(100) NOT NULL,
                        event_type VARCHAR(100) NOT NULL,
                        payload JSONB NOT NULL,
                        created_at TIMESTAMP DEFAULT now(),
                        processed BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_outbox_processed ON outbox(processed);
CREATE INDEX idx_outbox_created_at ON outbox(created_at);