-- =============================================================================
-- Billing bounded context — THE migration file for this BC.
--
-- One migration SQL file per bounded context. Edit THIS file when the Billing
-- schema changes. Versioned migration (V__): runs once per fresh deployment,
-- recorded in flyway_schema_history, checksum-locked afterwards.
--
-- Tables owned by billing:
--   * user_plan       — current plan (FREEMIUM / PREMIUM) per user
--   * payment_record  — Stripe payment intent ledger
--
-- user_id is an ACL boundary into the IAM context; payments and plan rows
-- stay readable for accounting even after the account is suspended, so no
-- foreign key is declared here.
-- =============================================================================

CREATE TABLE user_plan (
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

CREATE TABLE payment_record (
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