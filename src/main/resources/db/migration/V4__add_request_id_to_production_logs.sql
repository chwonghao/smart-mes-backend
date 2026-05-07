-- Add request_id column for Idempotency (Offline Sync support)
ALTER TABLE production_logs ADD COLUMN request_id VARCHAR(36) UNIQUE NULL;

-- Create index for faster lookups
CREATE INDEX idx_production_logs_request_id ON production_logs(request_id);
