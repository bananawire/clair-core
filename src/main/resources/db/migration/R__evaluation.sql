-- =============================================================================
-- Evaluation bounded context — canonical schema (repeatable migration).
--
-- This is THE file to edit when the Evaluation schema changes. V1..V6 already
-- built the table; this R__ migration is idempotent and re-runs whenever its
-- checksum changes, so future column/constraint edits land here.
--
-- Table owned by evaluation:
--   * telemetry_evaluations — a committed sensor reading plus the air-quality,
--                            particulate-matter, connectivity and location
--                            measures; reading_id is the durable identity for
--                            downstream evaluation (alerting, analytics).
-- =============================================================================

CREATE TABLE IF NOT EXISTS telemetry_evaluations (
    id                          uuid                        NOT NULL,
    device_id                   uuid                        NOT NULL,
    reading_id                  uuid                        NOT NULL,
    aq_co2                      double precision            NOT NULL,
    aq_temperature               double precision            NOT NULL,
    aq_humidity                  double precision            NOT NULL,
    pm_pm1_0                    double precision            NOT NULL,
    pm_pm2_5                    double precision            NOT NULL,
    pm_pm10                     double precision            NOT NULL,
    conn_status                 varchar(255)                NOT NULL,
    conn_network                varchar(255),
    conn_signal_strength        integer,
    location_country            varchar(255),
    uptime_seconds              bigint                      NOT NULL,
    status                      varchar(255)                NOT NULL,
    health_status               integer                     NOT NULL,
    device_time                 time(6),
    recorded_at                 timestamp(6) with time zone NOT NULL,
    alerts_evaluated_at         timestamp(6) with time zone,
    created_at                  timestamp(6) with time zone NOT NULL,
    updated_at                  timestamp(6) with time zone NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_telemetry_device_reading UNIQUE (device_id, reading_id)
);

-- Reading lookup by device, oldest first.
CREATE INDEX IF NOT EXISTS idx_telemetry_eval_device_recorded
    ON telemetry_evaluations (device_id, recorded_at);

-- Catch-up replay scans for readings that crashed between commit and the
-- after-commit alert evaluation handler.
CREATE INDEX IF NOT EXISTS idx_telemetry_eval_alerts_pending
    ON telemetry_evaluations (created_at)
    WHERE alerts_evaluated_at IS NULL;

-- device_id is an ACL boundary into the device context; we do NOT add a
-- foreign key so the context stays loosely coupled (the device may be
-- decommissioned while its readings remain for analytics).