-- =============================================================================
-- Evaluation + Alerting schema extension for outdoor hydrology and power status.
--
-- Adds rainfall / water-level measurements (alertable) and battery / solar
-- charging status (stored and exposed, never thresholded) to telemetry_evaluations.
-- Widens alerts.metric CHECK so RAINFALL and WATER_LEVEL can be persisted.
-- Does not edit V1–V11.
-- =============================================================================

ALTER TABLE telemetry_evaluations
    ADD COLUMN hydro_rainfall_mm double precision NOT NULL DEFAULT 0,
    ADD COLUMN hydro_water_level_cm double precision NOT NULL DEFAULT 0,
    ADD COLUMN pwr_battery_percent double precision,
    ADD COLUMN pwr_solar_charging boolean NOT NULL DEFAULT false;

ALTER TABLE alerts DROP CONSTRAINT IF EXISTS alerts_metric_check;
ALTER TABLE alerts
    ADD CONSTRAINT alerts_metric_check
    CHECK (metric IN ('PM25', 'CO2', 'TEMPERATURE', 'HUMIDITY', 'RAINFALL', 'WATER_LEVEL'));
