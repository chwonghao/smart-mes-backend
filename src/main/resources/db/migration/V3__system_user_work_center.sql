-- Add optional work center assignment to system users.

ALTER TABLE system_users
    ADD COLUMN IF NOT EXISTS work_center_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_system_users_work_center'
    ) THEN
        ALTER TABLE system_users
            ADD CONSTRAINT fk_system_users_work_center
            FOREIGN KEY (work_center_id) REFERENCES work_center(id);
    END IF;
END $$;