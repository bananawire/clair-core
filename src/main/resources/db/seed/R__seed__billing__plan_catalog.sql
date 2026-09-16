-- =============================================================================
-- Feature seed: billing.plan_catalog
--
-- Edit THIS file when the default plans change. Repeatable migration (R__):
-- re-runs whenever the checksum changes. ON CONFLICT DO NOTHING keeps re-runs
-- safe on databases that already have plans for these users.
--
-- Plans are tied to a user (user_plan.user_id). On a fresh database there are
-- no users yet; the IAM seed inserts them and then the next Flyway run picks
-- up this seed. To avoid that ordering hazard, plans here use the same fixed
-- user ids as R__seed__iam__default_users.sql so a single migration run on an
-- empty database populates both.
-- =============================================================================

INSERT INTO user_plan (id, user_id, plan_type, start_date, end_date, created_at, updated_at)
VALUES
    ('00000000-0000-0000-0000-000000000b01', '00000000-0000-0000-0000-0000000000a1', 'PREMIUM',  CURRENT_DATE, NULL, now(), now()),
    ('00000000-0000-0000-0000-000000000b02', '00000000-0000-0000-0000-0000000000a2', 'FREEMIUM', CURRENT_DATE, NULL, now(), now())
ON CONFLICT (id) DO NOTHING;