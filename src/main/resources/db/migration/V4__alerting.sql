-- =============================================================================
-- Alerting bounded context — THE migration file for this BC.
--
-- One migration SQL file per bounded context. Edit THIS file when the
-- Alerting schema changes. Versioned migration (V__): runs once per fresh
-- deployment, recorded in flyway_schema_history, checksum-locked afterwards.
--
-- Tables owned by alerting:
--   * alerts                       — open/acknowledged/resolved threshold breaches
--   * alert_transition_counter     — singleton row that hands out a strictly
--                                    increasing sequence per alert lifecycle change;
--                                    the edge pages transitions by
--                                    (transition_sequence), so a strictly
--                                    monotonic generator that locks the row on
--                                    every increment is required.
--
-- device_id, space_id and alert_id references are ACL boundaries into device
-- / spaces; they are not enforced as FKs because the edge and downstream
-- consumers must keep working even when those rows are deleted.
-- =============================================================================

CREATE TABLE alerts (
    id                       uuid                        NOT NULL,
    device_id                uuid                        NOT NULL,
    space_id                 uuid,
    metric                   varchar(255)                NOT NULL CHECK (metric IN ('PM25','CO2','TEMPERATURE','HUMIDITY')),
    severity                 varchar(255)                NOT NULL CHECK (severity IN ('CRITICAL','WARNING','LOW')),
    status                   varchar(255)                NOT NULL CHECK (status IN ('ACTIVE','ACKNOWLEDGED','RESOLVED')),
    actual_value             numeric(10,2)               NOT NULL,
    threshold_value          numeric(10,2)               NOT NULL,
    message                  varchar(500)                NOT NULL,
    device_name              varchar(255),
    space_name               varchar(255),
    occurred_at              timestamp(6) with time zone NOT NULL,
    resolved_at              timestamp(6) with time zone,
    transition_sequence      bigint                      NOT NULL DEFAULT 0,
    edge_receipt_sequence    bigint,
    created_at               timestamp(6) with time zone NOT NULL,
    updated_at               timestamp(6) with time zone NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_alert_device_metric_status    ON alerts (device_id, metric, status);
CREATE INDEX idx_alert_space_status           ON alerts (space_id, status);
CREATE INDEX idx_alert_occurred_at            ON alerts (occurred_at);
CREATE INDEX idx_alert_transition_sequence    ON alerts (transition_sequence);

CREATE TABLE alert_transition_counter (
    id          integer NOT NULL,
    last_value  bigint   NOT NULL,
    PRIMARY KEY (id)
);

INSERT INTO alert_transition_counter (id, last_value) VALUES (1, 0);