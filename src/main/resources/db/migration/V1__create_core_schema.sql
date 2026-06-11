-- ============================================================
-- V1 — Core Schema: Tenants, Users, Roles, Auth, Audit
-- ============================================================

-- ── Tenants ──────────────────────────────────────────────────────────────
CREATE TABLE tenants (
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name              VARCHAR(255) NOT NULL,
    subscription_plan VARCHAR(50)  NOT NULL DEFAULT 'STARTER'
                                   CHECK (subscription_plan IN ('STARTER','PROFESSIONAL','ENTERPRISE')),
    status            VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE'
                                   CHECK (status IN ('ACTIVE','SUSPENDED','CANCELLED')),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at        TIMESTAMPTZ
);

-- ── Users ─────────────────────────────────────────────────────────────────
CREATE TABLE users (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID         NOT NULL REFERENCES tenants(id),
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted_at    TIMESTAMPTZ,
    CONSTRAINT uq_users_email_tenant UNIQUE (email, tenant_id)
);

-- ── User Profiles (name & contact, separate from credentials) ─────────────
CREATE TABLE user_profiles (
    user_id    UUID         PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    first_name VARCHAR(100) NOT NULL,
    last_name  VARCHAR(100) NOT NULL,
    phone      VARCHAR(50),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ── Roles ─────────────────────────────────────────────────────────────────
CREATE TABLE roles (
    id   UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE
);

-- ── User <-> Role join ────────────────────────────────────────────────────
CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(id),
    PRIMARY KEY (user_id, role_id)
);

-- ── Refresh Tokens ────────────────────────────────────────────────────────
CREATE TABLE refresh_tokens (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(255) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked     BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── Tenant Settings ───────────────────────────────────────────────────────
CREATE TABLE tenant_settings (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID         NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    setting_key   VARCHAR(100) NOT NULL,
    setting_value TEXT,
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_tenant_settings UNIQUE (tenant_id, setting_key)
);

-- ── Audit Logs ────────────────────────────────────────────────────────────
CREATE TABLE audit_logs (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id    UUID,
    entity_type  VARCHAR(100) NOT NULL,
    entity_id    UUID,
    action       VARCHAR(100) NOT NULL,
    performed_by UUID,
    performed_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    ip_address   VARCHAR(50),
    details      JSONB
);

-- ── Indexes ───────────────────────────────────────────────────────────────
CREATE INDEX idx_users_tenant_id        ON users(tenant_id);
CREATE INDEX idx_users_email            ON users(email);
CREATE INDEX idx_users_deleted_at       ON users(deleted_at) WHERE deleted_at IS NULL;

CREATE INDEX idx_refresh_tokens_user_id     ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token_hash  ON refresh_tokens(token_hash);
CREATE INDEX idx_refresh_tokens_expires_at  ON refresh_tokens(expires_at);

CREATE INDEX idx_audit_logs_tenant_id   ON audit_logs(tenant_id);
CREATE INDEX idx_audit_logs_entity      ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_performed_at ON audit_logs(performed_at DESC);
CREATE INDEX idx_audit_logs_performed_by ON audit_logs(performed_by);

CREATE INDEX idx_tenant_settings_tenant_id ON tenant_settings(tenant_id);

-- ── Seed: Roles ───────────────────────────────────────────────────────────
INSERT INTO roles (id, name) VALUES
    (gen_random_uuid(), 'ROLE_PLATFORM_ADMIN'),
    (gen_random_uuid(), 'ROLE_PROPERTY_MANAGER'),
    (gen_random_uuid(), 'ROLE_LEASING_AGENT'),
    (gen_random_uuid(), 'ROLE_RESIDENT');
