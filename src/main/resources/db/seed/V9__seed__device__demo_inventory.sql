-- =============================================================================
-- Feature seed: device.demo_inventory
--
-- One seed SQL file per feature. Edit THIS file when the default device
-- inventory changes. Versioned migration (V__): runs once per fresh
-- deployment, recorded in flyway_schema_history, checksum-locked afterwards.
--
-- Mirrors DemoInventorySeedOnStartup (Java) so a fresh local database has
-- something to claim before the demo profile runs. The Java seeder still
-- owns the API keys (it writes them to DEVICE_PROVISIONING_EXPORT_PATH); the
-- records here use placeholder keys, which the Java seeder replaces on first
-- boot by way of the UNIQUE constraint on hardware_id / serial_number.
-- =============================================================================

INSERT INTO devices (id, serial_number, hardware_id, api_key, name, factory_name, device_type, deleted, created_at, updated_at)
VALUES
    ('00000000-0000-0000-0000-000000000d01', 'SN-0001', 'CLAIR-0001', 'placeholder-key-0001', 'Sensor 0001', 'Sensor 0001', 'air-quality-v1', false, now(), now()),
    ('00000000-0000-0000-0000-000000000d02', 'SN-0002', 'CLAIR-0002', 'placeholder-key-0002', 'Sensor 0002', 'Sensor 0002', 'air-quality-v1', false, now(), now()),
    ('00000000-0000-0000-0000-000000000d03', 'SN-0003', 'CLAIR-0003', 'placeholder-key-0003', 'Sensor 0003', 'Sensor 0003', 'air-quality-v1', false, now(), now()),
    ('00000000-0000-0000-0000-000000000d04', 'SN-0004', 'CLAIR-0004', 'placeholder-key-0004', 'Sensor 0004', 'Sensor 0004', 'air-quality-v1', false, now(), now()),
    ('00000000-0000-0000-0000-000000000d05', 'SN-0005', 'CLAIR-0005', 'placeholder-key-0005', 'Sensor 0005', 'Sensor 0005', 'air-quality-v1', false, now(), now());