-- =============================================================================
-- IAM bounded context — THE migration file for this BC.
--
-- One migration SQL file per bounded context. Edit THIS file when the IAM
-- schema changes. Versioned migration (V__): runs once per fresh deployment,
-- recorded in flyway_schema_history, checksum-locked afterwards.
--
-- Table owned by iam:
--   * users — accounts that signed up via mail or Google.
--
-- No FKs out of users: every cross-context reference (user_id columns in
-- billing / device / analytics) is an ACL boundary, not an FK, so the IAM
-- aggregate owns the user lifecycle.
-- =============================================================================

CREATE TABLE users (
    id              uuid                        NOT NULL,
    address         varchar(255)                NOT NULL UNIQUE,
    password_hash   varchar(255),
    oauth_provider  varchar(255)                CHECK (oauth_provider IN ('MAIL','GOOGLE')),
    oauth_subject   varchar(255),
    status          varchar(255)                NOT NULL CHECK (status IN ('PENDING_VERIFICATION','ACTIVE','SUSPENDED')),
    created_at      timestamp(6) with time zone NOT NULL,
    updated_at      timestamp(6) with time zone NOT NULL,
    PRIMARY KEY (id)
);