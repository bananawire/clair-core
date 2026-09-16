-- =============================================================================
-- Notifications bounded context — THE migration file for this BC.
--
-- One migration SQL file per bounded context. Edit THIS file when the
-- Notifications schema changes. Versioned migration (V__): runs once per
-- fresh deployment, recorded in flyway_schema_history, checksum-locked
-- afterwards.
--
-- Tables owned by notifications:
--   * email_logs             — every transactional email we attempted to send
--   * push_notification_logs — every push notification we attempted to send
--
-- user_id and alert_id are ACL boundaries into IAM / Alerting; notifications
-- must keep the delivery log even when the user is suspended or the alert is
-- resolved, so no foreign keys are declared here.
-- =============================================================================

CREATE TABLE email_logs (
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

CREATE TABLE push_notification_logs (
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