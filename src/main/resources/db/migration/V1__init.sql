-- Initial baseline migration for local dev
-- Add your real schema changes in subsequent migrations (V2__, V3__, ...)

CREATE TABLE IF NOT EXISTS flyway_baseline_marker (
    id INT PRIMARY KEY
);