-- =============================================================================
-- Device bounded context — canonical schema (repeatable migration).
--
-- This is THE file to edit when the Device schema changes. V1..V6 already built
-- the tables; this R__ migration is idempotent and re-runs whenever its
-- checksum changes, so future column/constraint edits land here.
--
-- Tables owned by device:
--   * devices                      — factory inventory (provisioned, not claimed)
--   * device_assignments           — pairing generation: who owns the device now
--   * device_assignment_configuration — per-assignment key/value overrides
--   * device_commands              — STANDBY/WAKE/RESTART issued under an assignment
--   * organizations                — top-level container owned by a user
--   * spaces                       — room/group inside an organization
-- =============================================================================

CREATE TABLE IF NOT EXISTS devices (
    id              uuid                        NOT NULL,
    api_key         varchar(255)                NOT NULL UNIQUE,
    device_type     varchar(255)                NOT NULL,
    factory_name    varchar(255)                NOT NULL,
    hardware_id     varchar(255)                NOT NULL UNIQUE,
    name            varchar(255)                NOT NULL,
    serial_number   varchar(255)                NOT NULL UNIQUE,
    deleted         boolean                     NOT NULL DEFAULT false,
    created_at      timestamp(6) with time zone NOT NULL,
    updated_at      timestamp(6) with time zone NOT NULL,
    PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_devices_updated_at ON devices (updated_at);

CREATE TABLE IF NOT EXISTS device_assignments (
    id              uuid                        NOT NULL,
    device_id       uuid                        NOT NULL UNIQUE,
    owner_user_id   uuid,
    space_id        uuid,
    claim_token     varchar(255)                UNIQUE,
    status          varchar(255)                NOT NULL CHECK (status IN ('OFFLINE','ONLINE','STANDBY','ERROR','MAINTENANCE','DECOMMISSIONED')),
    activated_at    timestamp(6) with time zone,
    last_seen_at    timestamp(6) with time zone,
    presence_at     timestamp(6) with time zone,
    created_at      timestamp(6) with time zone NOT NULL,
    updated_at      timestamp(6) with time zone NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS device_assignment_configuration (
    assignment_id   uuid                        NOT NULL,
    config_key      varchar(255)                NOT NULL,
    config_value    varchar(255),
    PRIMARY KEY (assignment_id, config_key)
);

CREATE TABLE IF NOT EXISTS device_commands (
    id              uuid                        NOT NULL,
    device_id       uuid                        NOT NULL,
    assignment_id   uuid,
    type            varchar(255)                NOT NULL CHECK (type IN ('STANDBY','WAKE','RESTART')),
    status          varchar(255)                NOT NULL CHECK (status IN ('PENDING','SENT','EXECUTED','FAILED','EXPIRED')),
    payload         text,
    sent_at         timestamp(6) with time zone,
    executed_at     timestamp(6) with time zone,
    failure_reason  text,
    created_at      timestamp(6) with time zone NOT NULL,
    updated_at      timestamp(6) with time zone NOT NULL,
    PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_device_commands_assignment ON device_commands (assignment_id);

CREATE TABLE IF NOT EXISTS organizations (
    id              uuid                        NOT NULL,
    user_id         uuid,
    name            varchar(255)                NOT NULL,
    created_at      timestamp(6) with time zone NOT NULL,
    updated_at      timestamp(6) with time zone NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS spaces (
    id              uuid                        NOT NULL,
    organization_id uuid                        NOT NULL,
    user_id         uuid,
    name            varchar(255)                NOT NULL,
    created_at      timestamp(6) with time zone NOT NULL,
    updated_at      timestamp(6) with time zone NOT NULL,
    PRIMARY KEY (id)
);

-- Integrity guarantees kept here so they survive a wipe + re-create of the
-- device tables: orphan device_assignments and device_commands are a bug, and
-- the migration integration test exercises both inserts to prove they fail.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint c
        JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attname = 'device_id'
        WHERE c.contype = 'f' AND c.conrelid = 'device_assignments'::regclass
          AND c.confrelid = 'devices'::regclass AND c.conkey = ARRAY[a.attnum]
    ) THEN
        ALTER TABLE device_assignments
            ADD CONSTRAINT fk_device_assignments_device FOREIGN KEY (device_id) REFERENCES devices(id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint c
        JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attname = 'device_id'
        WHERE c.contype = 'f' AND c.conrelid = 'device_commands'::regclass
          AND c.confrelid = 'devices'::regclass AND c.conkey = ARRAY[a.attnum]
    ) THEN
        ALTER TABLE device_commands
            ADD CONSTRAINT fk_device_commands_device FOREIGN KEY (device_id) REFERENCES devices(id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint c
        JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attname = 'assignment_id'
        WHERE c.contype = 'f' AND c.conrelid = 'device_assignment_configuration'::regclass
          AND c.confrelid = 'device_assignments'::regclass AND c.conkey = ARRAY[a.attnum]
    ) THEN
        ALTER TABLE device_assignment_configuration
            ADD CONSTRAINT fk_device_assignment_configuration_assignment
            FOREIGN KEY (assignment_id) REFERENCES device_assignments(id);
    END IF;
END $$;

-- Cross-context references (organizations.user_id, spaces.user_id, spaces.organization_id,
-- device_assignments.owner_user_id, device_assignments.space_id, device_commands.assignment_id)
-- are ACL boundaries, not foreign keys, so they are NOT enforced here.