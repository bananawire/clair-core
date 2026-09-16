-- =============================================================================
-- Feature seed: billing.plan_catalog
--
-- One seed SQL file per feature. Edit THIS file when the default plans
-- change. Versioned migration (V__): runs once per fresh deployment,
-- recorded in flyway_schema_history, checksum-locked afterwards.
--
-- Plans are tied to a user (user_plan.user_id). The fixed user ids match the
-- ones inserted by V8__seed__iam__default_users.sql so a single migration run
-- on an empty database populates both.
-- =============================================================================

INSERT INTO user_plan (id, user_id, plan_type, start_date, end_date, created_at, updated_at)
VALUES
    ('00000000-0000-0000-0000-000000000b01', '00000000-0000-0000-0000-0000000000a1', 'PREMIUM',  CURRENT_DATE, NULL, now(), now()),
    ('00000000-0000-0000-0000-000000000b02', '00000000-0000-0000-0000-0000000000a2', 'FREEMIUM', CURRENT_DATE, NULL, now(), now());