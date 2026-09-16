-- =============================================================================
-- Notifications bounded context — canonical schema (repeatable migration).
--
-- This is THE file to edit when the Notifications schema changes. V1..V6
-- already built the tables; this R__ migration is idempotent and re-runs
-- whenever its checksum changes, so future column/constraint edits land here.
--
-- Tables owned by notifications:
--   * email_logs             — every transactional email we attempted to send
--   * push_notification_logs — every push notification we attempted to send
-- =============================================================================

CREATE TABLE IF NOT EXISTS email_logs (
    id              uuid                        NOT NULL,
    recipient_email varchar(255)                NOT NULL,
    subject         varchar(255)                NOT NULL,
    content         TEXT,
    sent            boolean                     NOT NULL,
    error_message   varchar(255),
    created_at      timestamp(6) with time zone NOT NULL,
    updated_at      timestamp(6) with time zone NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS push_notification_logs (
    id              uuid                        NOT NULL,
    user_id         uuid                        NOT NULL,
    alert_id        uuid,
    title           varchar(255)                NOT NULL,
    message         TEXT                        NOT NULL,
    sent            boolean                     NOT NULL,
    error_message   varchar(255),
    created_at      timestamp(6) with time zone NOT NULL,
    updated_at      timestamp(6) with time zone NOT NULL,
    PRIMARY KEY (id)
);

-- user_id and alert_id are ACL boundaries into IAM / Alerting; notifications
-- must keep the delivery log even when the user is suspended or the alert is
-- resolved, so no foreign keys are declared here.