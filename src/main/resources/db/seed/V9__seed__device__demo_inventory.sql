-- =============================================================================
-- Feature seed: device.demo_inventory
--
-- One seed SQL file per feature. Edit THIS file when the default device
-- inventory changes. Versioned migration (V__): runs once per fresh
-- deployment, recorded in flyway_schema_history, checksum-locked afterwards.
--
-- Mirrors DemoInventorySeedOnStartup (Java) so a fresh local database has
-- something to claim before the demo profile runs. The Java seeder still
-- owns the API keys under the `demo` profile: on first boot it overwrites
-- the values below with freshly generated ones (32 random bytes encoded as
-- URL-safe Base64, no padding) and writes them to
-- DEVICE_PROVISIONING_EXPORT_PATH. Outside the `demo` profile, the keys
-- here are what the device uses to authenticate; rotate them in a new
-- migration rather than editing this file in place once shipped.
-- =============================================================================

INSERT INTO devices (id, serial_number, hardware_id, api_key, name, factory_name, device_type, deleted, created_at, updated_at)
VALUES
    ('01d3c4fc-fad1-48fc-bac7-6e2793a2f5ae', 'SN-0001', 'CLAIR-0001', 'uG8Ak90S-boWDeDIKXbKL_TbVKLQX_PBB-6f95licqI', 'Sensor 0001', 'Sensor 0001', 'air-quality-v1', false, now(), now()),
    ('2c120f53-1960-47a0-8482-d65b5eece7fa', 'SN-0002', 'CLAIR-0002', 'Q0CSsGoo1FolpThlNGBT4FCjZ5R_l0ySPYrChuGLTy0', 'Sensor 0002', 'Sensor 0002', 'air-quality-v1', false, now(), now()),
    ('9c56ccb8-e792-471e-9f80-a395888dccc4', 'SN-0003', 'CLAIR-0003', 'iFdBU0O7dRqEY6QonjGmZTh0j1_l1wusd3_71S0Yn88', 'Sensor 0003', 'Sensor 0003', 'air-quality-v1', false, now(), now()),
    ('ca01f2ee-5cec-4094-8028-bf0d5aaaf3aa', 'SN-0004', 'CLAIR-0004', 'b3Q3m5LOrbxR__uGbbKU6UWB1GRX_WX3V_qwsmA8xzo', 'Sensor 0004', 'Sensor 0004', 'air-quality-v1', false, now(), now()),
    ('bab718b1-9f6c-4fdd-b2dd-01be94899289', 'SN-0005', 'CLAIR-0005', 'tQtU90Y0v_vgGxdL6zAbgGUxBWy0P_EJIw922ZG1_Og', 'Sensor 0005', 'Sensor 0005', 'air-quality-v1', false, now(), now());
