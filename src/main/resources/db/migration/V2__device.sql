-- =============================================================================
-- Device bounded context — THE migration file for this BC.
--
-- One migration SQL file per bounded context. Edit THIS file when the Device
-- schema changes. Versioned migration (V__): runs once per fresh deployment,
-- recorded in flyway_schema_history, checksum-locked afterwards.
--
-- Tables owned by device:
--   * devices                       — factory inventory (provisioned, not claimed)
--   * device_assignments            — pairing generation: who owns the device now
--   * device_assignment_configuration — per-assignment key/value overrides
--   * device_commands               — STANDBY/WAKE/RESTART issued under an assignment
--   * organizations                 — top-level container owned by a user
--   * spaces                        — room/group inside an organization
--
-- Cross-context references (organizations.user_id, spaces.user_id,
-- spaces.organization_id, device_assignments.owner_user_id,
-- device_assignments.space_id, device_commands.assignment_id) are ACL
-- boundaries, not foreign keys, so they are NOT enforced here.
-- =============================================================================

CREATE TABLE devices (
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
CREATE INDEX idx_devices_updated_at ON devices (updated_at);

CREATE TABLE device_assignments (
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
    PRIMARY KEY (id),
    CONSTRAINT fk_device_assignments_device FOREIGN KEY (device_id) REFERENCES devices (id)
);

CREATE TABLE device_assignment_configuration (
    assignment_id   uuid                        NOT NULL,
    config_key      varchar(255)                NOT NULL,
    config_value    varchar(255),
    PRIMARY KEY (assignment_id, config_key),
    CONSTRAINT fk_device_assignment_configuration_assignment
        FOREIGN KEY (assignment_id) REFERENCES device_assignments (id)
);

CREATE TABLE device_commands (
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
    PRIMARY KEY (id),
    CONSTRAINT fk_device_commands_device FOREIGN KEY (device_id) REFERENCES devices (id)
);
CREATE INDEX idx_device_commands_assignment ON device_commands (assignment_id);

CREATE TABLE organizations (
    id              uuid                        NOT NULL,
    user_id         uuid,
    name            varchar(255)                NOT NULL,
    created_at      timestamp(6) with time zone NOT NULL,
    updated_at      timestamp(6) with time zone NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE spaces (
    id              uuid                        NOT NULL,
    organization_id uuid                        NOT NULL,
    user_id         uuid,
    name            varchar(255)                NOT NULL,
    created_at      timestamp(6) with time zone NOT NULL,
    updated_at      timestamp(6) with time zone NOT NULL,
    PRIMARY KEY (id)
);