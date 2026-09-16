-- =============================================================================
-- Feature seed: billing.plan_catalog
--
-- One seed SQL file per feature. Edit THIS file when the default plans
-- change. Versioned migration (V__): runs once per fresh deployment,
-- recorded in flyway_schema_history, checksum-locked afterwards.
--
-- Plans are tied to a user (user_plan.user_id). The user_id values below are
-- the stable UUIDs inserted by V8__seed__iam__default_users.sql; keep them in
-- sync if that file is ever changed.
-- =============================================================================

INSERT INTO user_plan (id, user_id, plan_type, start_date, end_date, created_at, updated_at)
VALUES
    ('29150302-2b5c-4198-9ca0-08d773ce7dd2', '3cc7e082-b755-4446-9e59-033d2022c37b', 'PREMIUM',  CURRENT_DATE, NULL, now(), now()),
    ('515098f3-28fc-47d5-946f-74fe5516e73f', '8453fdb2-34fd-4292-a369-8140e408985b', 'FREEMIUM', CURRENT_DATE, NULL, now(), now());
