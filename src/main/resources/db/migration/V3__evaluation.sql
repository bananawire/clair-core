-- =============================================================================
-- Evaluation bounded context — THE migration file for this BC.
--
-- One migration SQL file per bounded context. Edit THIS file when the
-- Evaluation schema changes. Versioned migration (V__): runs once per fresh
-- deployment, recorded in flyway_schema_history, checksum-locked afterwards.
--
-- Table owned by evaluation:
--   * telemetry_evaluations — a committed sensor reading plus the air-quality,
--                            particulate-matter, connectivity and location
--                            measures; reading_id is the durable identity for
--                            downstream evaluation (alerting, analytics).
--                            alerts_evaluated_at records that the after-commit
--                            handler finished with the reading; NULL means a
--                            catch-up replay still has work to do.
--
-- device_id is an ACL boundary into the device context; we do NOT add a
-- foreign key so the context stays loosely coupled (the device may be
-- decommissioned while its readings remain for analytics).
-- =============================================================================

CREATE TABLE telemetry_evaluations (
    id                          uuid                        NOT NULL,
    device_id                   uuid                        NOT NULL,
    reading_id                  uuid                        NOT NULL,
    aq_co2                      double precision            NOT NULL,
    aq_temperature              double precision            NOT NULL,
    aq_humidity                 double precision            NOT NULL,
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

CREATE INDEX idx_telemetry_eval_device_recorded
    ON telemetry_evaluations (device_id, recorded_at);

CREATE INDEX idx_telemetry_eval_alerts_pending
    ON telemetry_evaluations (created_at)
    WHERE alerts_evaluated_at IS NULL;