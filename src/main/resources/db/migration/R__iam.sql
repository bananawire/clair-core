-- =============================================================================
-- IAM bounded context — canonical schema (repeatable migration).
--
-- This is THE file to edit when the IAM schema changes. V1..V6 already built
-- the table; this R__ migration is idempotent and re-runs whenever its
-- checksum changes, so future column/constraint edits land here.
--
-- Tables owned by iam:
--   * users                       — accounts that signed up via mail or Google
-- =============================================================================

CREATE TABLE IF NOT EXISTS users (
    id              uuid                        NOT NULL,
    address         varchar(255)                NOT NULL UNIQUE,
    password_hash   varchar(255),
    oauth_provider   varchar(255)               CHECK (oauth_provider IN ('MAIL','GOOGLE')),
    oauth_subject   varchar(255),
    status          varchar(255)                NOT NULL CHECK (status IN ('PENDING_VERIFICATION','ACTIVE','SUSPENDED')),
    created_at      timestamp(6) with time zone NOT NULL,
    updated_at      timestamp(6) with time zone NOT NULL,
    PRIMARY KEY (id)
);

-- IAM has no foreign keys of its own: every cross-context reference (user_id
-- columns in billing / device / analytics) is an ACL boundary, not an FK, and
-- must stay that way so the IAM aggregate owns the user lifecycle.