-- =============================================================================
-- Assistant bounded context — THE migration file for this BC.
--
-- One migration SQL file per bounded context. Edit THIS file when the
-- Assistant schema changes. Versioned migration (V__): runs once per
-- fresh deployment, recorded in flyway_schema_history, checksum-locked
-- afterwards. V8..V10 are the seed files in db/seed, hence V11.
--
-- Tables owned by assistant:
--   * assistant_conversations — one thread per question history of a user
--   * assistant_messages      — every user question and assistant answer;
--                               USER rows per day are the daily quota
--
-- user_id is an ACL boundary into IAM, so no foreign key is declared to
-- users, same as notifications.
-- =============================================================================

CREATE TABLE assistant_conversations (
    id               uuid                        NOT NULL,
    user_id          uuid                        NOT NULL,
    title            varchar(80)                 NOT NULL,
    last_message_at  timestamp(6) with time zone NOT NULL,
    created_at       timestamp(6) with time zone NOT NULL,
    updated_at       timestamp(6) with time zone NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_assistant_conversations_user_activity
    ON assistant_conversations (user_id, last_message_at DESC);

CREATE TABLE assistant_messages (
    id                 uuid                        NOT NULL,
    conversation_id    uuid                        NOT NULL,
    user_id            uuid                        NOT NULL,
    role               varchar(16)                 NOT NULL,
    content            TEXT                        NOT NULL,
    model              varchar(64),
    prompt_tokens      integer,
    completion_tokens  integer,
    latency_ms         integer,
    feedback           varchar(8),
    created_at         timestamp(6) with time zone NOT NULL,
    updated_at         timestamp(6) with time zone NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_assistant_messages_conversation
        FOREIGN KEY (conversation_id) REFERENCES assistant_conversations (id) ON DELETE CASCADE,
    CONSTRAINT ck_assistant_messages_role CHECK (role IN ('USER', 'ASSISTANT')),
    CONSTRAINT ck_assistant_messages_feedback CHECK (feedback IS NULL OR feedback IN ('UP', 'DOWN'))
);

CREATE INDEX idx_assistant_messages_conversation
    ON assistant_messages (conversation_id, created_at);

-- Daily quota: COUNT(*) WHERE user_id = ? AND role = 'USER' AND created_at >= ?
CREATE INDEX idx_assistant_messages_user_role_created
    ON assistant_messages (user_id, role, created_at);
