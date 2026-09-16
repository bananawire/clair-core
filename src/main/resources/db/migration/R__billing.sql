-- =============================================================================
-- Billing bounded context — canonical schema (repeatable migration).
--
-- This is THE file to edit when the Billing schema changes. V1..V6 already
-- built the tables; this R__ migration is idempotent and re-runs whenever its
-- checksum changes, so future column/constraint edits land here.
--
-- Tables owned by billing:
--   * user_plan       — current plan (FREEMIUM / PREMIUM) per user
--   * payment_record  — Stripe payment intent ledger
-- =============================================================================

CREATE TABLE IF NOT EXISTS user_plan (
    id          uuid                        NOT NULL,
    user_id     uuid,
    plan_type   varchar(255)
        CHECK (plan_type IN ('FREEMIUM','PREMIUM')),
    start_date  date,
    end_date    date,
    created_at  timestamp(6) with time zone NOT NULL,
    updated_at  timestamp(6) with time zone NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS payment_record (
    id                       uuid                        NOT NULL,
    user_id                  uuid,
    amount                   bigint,
    currency                 varchar(255),
    status                   varchar(255)
        CHECK (status IN ('PENDING','COMPLETED','FAILED')),
    stripe_payment_intent_id varchar(255),
    created_at               timestamp(6) with time zone NOT NULL,
    updated_at               timestamp(6) with time zone NOT NULL,
    PRIMARY KEY (id)
);

-- user_id is an ACL boundary into the IAM context; payments and plan rows
-- stay readable for accounting even after the account is suspended, so no
-- foreign key is declared here.