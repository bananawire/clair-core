-- =============================================================================
-- Analytics bounded context — THE migration file for this BC.
--
-- One migration SQL file per bounded context. Edit THIS file when the
-- Analytics schema changes. Versioned migration (V__): runs once per fresh
-- deployment, recorded in flyway_schema_history, checksum-locked afterwards.
--
-- Tables owned by analytics:
--   * device_analytics_snapshots — recent windowed AQI rollup per device
--   * device_daily_summaries     — one row per device per day
--   * device_monthly_summaries   — one row per device per month
--
-- device_id is an ACL boundary into the device context; analytics must keep
-- reading historical summaries even after the device is decommissioned, so no
-- foreign key is declared here.
-- =============================================================================

CREATE TABLE device_analytics_snapshots (
    id                  uuid                        NOT NULL,
    device_id           uuid                        NOT NULL,
    time_window_start   timestamp(6) with time zone NOT NULL,
    time_window_end     timestamp(6) with time zone NOT NULL,
    average_co2         double precision            NOT NULL,
    average_pm2_5       double precision            NOT NULL,
    average_temperature double precision            NOT NULL,
    average_humidity    double precision            NOT NULL,
    aqi_value           integer                     NOT NULL,
    aqi_category        varchar(255)                NOT NULL
        CHECK (aqi_category IN ('GOOD','MODERATE','UNHEALTHY_FOR_SENSITIVE','UNHEALTHY','VERY_UNHEALTHY','HAZARDOUS')),
    created_at          timestamp(6) with time zone NOT NULL,
    updated_at          timestamp(6) with time zone NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE device_daily_summaries (
    id                       uuid                        NOT NULL,
    device_id                uuid                        NOT NULL,
    summary_date             date                        NOT NULL,
    co2_avg                  double precision            NOT NULL,
    co2_min                  double precision            NOT NULL,
    co2_max                  double precision            NOT NULL,
    pm2_5_avg                double precision            NOT NULL,
    pm2_5_min                double precision            NOT NULL,
    pm2_5_max                double precision            NOT NULL,
    temperature_avg          double precision            NOT NULL,
    temperature_min          double precision            NOT NULL,
    temperature_max          double precision            NOT NULL,
    humidity_avg             double precision            NOT NULL,
    humidity_min             double precision            NOT NULL,
    humidity_max             double precision            NOT NULL,
    peak_pm2_5               double precision            NOT NULL,
    peak_pm2_5_at            timestamp(6) with time zone NOT NULL,
    average_aqi              integer                     NOT NULL,
    dominant_aqi_category    varchar(255)                NOT NULL
        CHECK (dominant_aqi_category IN ('GOOD','MODERATE','UNHEALTHY_FOR_SENSITIVE','UNHEALTHY','VERY_UNHEALTHY','HAZARDOUS')),
    cat_good                 bigint                      NOT NULL,
    cat_moderate             bigint                      NOT NULL,
    cat_unhealthy_sensitive  bigint                      NOT NULL,
    cat_unhealthy            bigint                      NOT NULL,
    cat_very_unhealthy       bigint                      NOT NULL,
    cat_hazardous            bigint                      NOT NULL,
    reading_count            bigint                      NOT NULL,
    aqi_delta_pct            double precision,
    created_at               timestamp(6) with time zone NOT NULL,
    updated_at               timestamp(6) with time zone NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_device_daily_summary UNIQUE (device_id, summary_date)
);

CREATE TABLE device_monthly_summaries (
    id                       uuid                        NOT NULL,
    device_id                uuid                        NOT NULL,
    summary_month            date                        NOT NULL,
    co2_avg                  double precision            NOT NULL,
    co2_min                  double precision            NOT NULL,
    co2_max                  double precision            NOT NULL,
    pm2_5_avg                double precision            NOT NULL,
    pm2_5_min                double precision            NOT NULL,
    pm2_5_max                double precision            NOT NULL,
    temperature_avg          double precision            NOT NULL,
    temperature_min          double precision            NOT NULL,
    temperature_max          double precision            NOT NULL,
    humidity_avg             double precision            NOT NULL,
    humidity_min             double precision            NOT NULL,
    humidity_max             double precision            NOT NULL,
    peak_pm2_5               double precision            NOT NULL,
    peak_pm2_5_at            timestamp(6) with time zone NOT NULL,
    average_aqi              integer                     NOT NULL,
    dominant_aqi_category    varchar(255)                NOT NULL
        CHECK (dominant_aqi_category IN ('GOOD','MODERATE','UNHEALTHY_FOR_SENSITIVE','UNHEALTHY','VERY_UNHEALTHY','HAZARDOUS')),
    cat_good                 bigint                      NOT NULL,
    cat_moderate             bigint                      NOT NULL,
    cat_unhealthy_sensitive  bigint                      NOT NULL,
    cat_unhealthy            bigint                      NOT NULL,
    cat_very_unhealthy       bigint                      NOT NULL,
    cat_hazardous            bigint                      NOT NULL,
    reading_count            bigint                      NOT NULL,
    days_covered             integer                     NOT NULL,
    aqi_delta_pct            double precision,
    created_at               timestamp(6) with time zone NOT NULL,
    updated_at               timestamp(6) with time zone NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_device_monthly_summary UNIQUE (device_id, summary_month)
);