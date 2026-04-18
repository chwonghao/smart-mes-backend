-- Ensure worker table is compatible with BaseEntity inheritance.
-- Safe for both existing and fresh databases.

CREATE TABLE IF NOT EXISTS master_workers (
    id BIGSERIAL PRIMARY KEY,
    worker_code VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    role VARCHAR(255) NOT NULL,
    shift VARCHAR(255),
    status VARCHAR(255),
    tenant_id VARCHAR(50),
    created_at TIMESTAMP,
    created_by VARCHAR(50),
    updated_at TIMESTAMP,
    updated_by VARCHAR(50),
    is_deleted BOOLEAN DEFAULT FALSE
);

ALTER TABLE master_workers ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(50);
ALTER TABLE master_workers ADD COLUMN IF NOT EXISTS created_at TIMESTAMP;
ALTER TABLE master_workers ADD COLUMN IF NOT EXISTS created_by VARCHAR(50);
ALTER TABLE master_workers ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;
ALTER TABLE master_workers ADD COLUMN IF NOT EXISTS updated_by VARCHAR(50);
ALTER TABLE master_workers ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN DEFAULT FALSE;

UPDATE master_workers
SET tenant_id = 'TENANT_DEFAULT'
WHERE tenant_id IS NULL OR tenant_id = '';

UPDATE master_workers
SET created_at = CURRENT_TIMESTAMP
WHERE created_at IS NULL;

UPDATE master_workers
SET is_deleted = FALSE
WHERE is_deleted IS NULL;

ALTER TABLE master_workers ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE master_workers ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE master_workers ALTER COLUMN is_deleted SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_master_workers_tenant_deleted
    ON master_workers (tenant_id, is_deleted);
