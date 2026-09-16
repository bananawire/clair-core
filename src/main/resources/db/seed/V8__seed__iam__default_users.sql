-- =============================================================================
-- Feature seed: iam.default_users
--
-- One seed SQL file per feature. Edit THIS file when the default IAM accounts
-- change. Versioned migration (V__): runs once per fresh deployment,
-- recorded in flyway_schema_history, checksum-locked afterwards.
--
-- Default accounts provisioned on every fresh deployment so local development
-- and the demo profile have something to sign in with without going through
-- the verification flow.
-- =============================================================================

-- Admin account: full privileges, mail-based login.
-- Password is the bcrypt of "Admin#12345" (rounds=10). Rotate before any real
-- deployment; this seed exists for local / demo use only.
INSERT INTO users (id, address, password_hash, oauth_provider, oauth_subject, status, created_at, updated_at)
VALUES (
    '00000000-0000-0000-0000-0000000000a1',
    'admin' || '@' || 'clair.local',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    NULL,
    NULL,
    'ACTIVE',
    now(),
    now()
);

-- Demo user: regular account used by integration tests and the demo profile.
INSERT INTO users (id, address, password_hash, oauth_provider, oauth_subject, status, created_at, updated_at)
VALUES (
    '00000000-0000-0000-0000-0000000000a2',
    'demo' || '@' || 'clair.local',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    NULL,
    NULL,
    'ACTIVE',
    now(),
    now()
);