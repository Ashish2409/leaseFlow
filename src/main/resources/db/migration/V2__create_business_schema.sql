-- ============================================================
-- V2 — Business Schema: Properties, Prospects, Applications,
--       Residents, Leases, Templates, Documents, Signatures,
--       Notifications, Renewals, Idempotency
-- ============================================================

-- ── Properties ────────────────────────────────────────────────────────────
CREATE TABLE properties (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id      UUID         NOT NULL REFERENCES tenants(id),
    name           VARCHAR(255) NOT NULL,
    address_line1  VARCHAR(255) NOT NULL,
    address_line2  VARCHAR(255),
    city           VARCHAR(100) NOT NULL,
    state          VARCHAR(2)   NOT NULL,
    zip_code       VARCHAR(20)  NOT NULL,
    status         VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE'
                                CHECK (status IN ('ACTIVE','INACTIVE')),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted_at     TIMESTAMPTZ
);

-- ── Buildings (within a property) ─────────────────────────────────────────
CREATE TABLE buildings (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    property_id UUID         NOT NULL REFERENCES properties(id),
    tenant_id   UUID         NOT NULL REFERENCES tenants(id),
    name        VARCHAR(100) NOT NULL,
    floors      INTEGER,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMPTZ
);

-- ── Floor Plans ───────────────────────────────────────────────────────────
CREATE TABLE floor_plans (
    id          UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID           NOT NULL REFERENCES tenants(id),
    property_id UUID           NOT NULL REFERENCES properties(id),
    name        VARCHAR(100)   NOT NULL,
    bedrooms    INTEGER        NOT NULL,
    bathrooms   NUMERIC(3,1)   NOT NULL,
    sqft        INTEGER,
    base_rent   DECIMAL(12,2)  NOT NULL,
    created_at  TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMPTZ
);

-- ── Units ─────────────────────────────────────────────────────────────────
CREATE TABLE units (
    id            UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID          NOT NULL REFERENCES tenants(id),
    property_id   UUID          NOT NULL REFERENCES properties(id),
    building_id   UUID          REFERENCES buildings(id),
    floor_plan_id UUID          REFERENCES floor_plans(id),
    unit_number   VARCHAR(50)   NOT NULL,
    bedrooms      INTEGER       NOT NULL,
    bathrooms     NUMERIC(3,1)  NOT NULL,
    sqft          INTEGER,
    floor_number  INTEGER,
    monthly_rent  DECIMAL(12,2) NOT NULL,
    status        VARCHAR(50)   NOT NULL DEFAULT 'AVAILABLE'
                               CHECK (status IN ('AVAILABLE','RESERVED','LEASED','MAINTENANCE','OFFLINE')),
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    deleted_at    TIMESTAMPTZ,
    CONSTRAINT uq_unit_number_property UNIQUE (property_id, unit_number)
);

-- ── Prospects ─────────────────────────────────────────────────────────────
CREATE TABLE prospects (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID         NOT NULL REFERENCES tenants(id),
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    email           VARCHAR(255) NOT NULL,
    phone           VARCHAR(50),
    lead_source     VARCHAR(100)
                    CHECK (lead_source IN ('WEBSITE','ILS_ZILLOW','ILS_APARTMENTS','REFERRAL','WALK_IN','PHONE','OTHER')),
    desired_move_in DATE,
    desired_bedrooms INTEGER,
    budget_max      DECIMAL(12,2),
    status          VARCHAR(50)  NOT NULL DEFAULT 'NEW'
                    CHECK (status IN ('NEW','CONTACTED','TOUR_SCHEDULED','APPLIED','APPROVED','REJECTED','CLOSED')),
    assigned_to     UUID         REFERENCES users(id),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ
);

-- ── Prospect Activities ───────────────────────────────────────────────────
CREATE TABLE prospect_activities (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    prospect_id   UUID         NOT NULL REFERENCES prospects(id) ON DELETE CASCADE,
    activity_type VARCHAR(50)  NOT NULL
                  CHECK (activity_type IN ('CALL','EMAIL','TOUR','NOTE','STATUS_CHANGE','OTHER')),
    notes         TEXT,
    performed_by  UUID         REFERENCES users(id),
    occurred_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ── Applications ──────────────────────────────────────────────────────────
CREATE TABLE applications (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID        NOT NULL REFERENCES tenants(id),
    prospect_id   UUID        REFERENCES prospects(id),
    unit_id       UUID        NOT NULL REFERENCES units(id),
    status        VARCHAR(50) NOT NULL DEFAULT 'SUBMITTED'
                  CHECK (status IN ('SUBMITTED','UNDER_REVIEW','APPROVED','APPROVED_WITH_CONDITIONS','REJECTED','WITHDRAWN')),
    rejection_reason VARCHAR(255),
    conditions    TEXT,
    submitted_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    decided_at    TIMESTAMPTZ,
    decided_by    UUID        REFERENCES users(id),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at    TIMESTAMPTZ
);

-- ── Application Occupants (primary + co-applicants + dependants) ──────────
CREATE TABLE application_occupants (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id   UUID        NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
    first_name       VARCHAR(100) NOT NULL,
    last_name        VARCHAR(100) NOT NULL,
    date_of_birth    DATE,
    email            VARCHAR(255),
    phone            VARCHAR(50),
    relationship_type VARCHAR(50) NOT NULL
                     CHECK (relationship_type IN ('PRIMARY','CO_APPLICANT','GUARANTOR','MINOR_DEPENDENT','OTHER')),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── Application Employment ────────────────────────────────────────────────
CREATE TABLE application_employment (
    id              UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    occupant_id     UUID          NOT NULL REFERENCES application_occupants(id) ON DELETE CASCADE,
    employer_name   VARCHAR(255)  NOT NULL,
    job_title       VARCHAR(255),
    employment_type VARCHAR(50)   NOT NULL
                    CHECK (employment_type IN ('FULL_TIME','PART_TIME','SELF_EMPLOYED','RETIRED','UNEMPLOYED','OTHER')),
    monthly_income  DECIMAL(12,2) NOT NULL,
    start_date      DATE,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

-- ── Application Documents ─────────────────────────────────────────────────
CREATE TABLE application_documents (
    id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id UUID        NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
    occupant_id    UUID        REFERENCES application_occupants(id),
    document_type  VARCHAR(100) NOT NULL
                   CHECK (document_type IN ('GOVERNMENT_ID','PAY_STUB','BANK_STATEMENT','TAX_RETURN','PRIOR_LANDLORD_LETTER','OTHER')),
    storage_key    VARCHAR(500) NOT NULL,
    original_filename VARCHAR(255),
    verified       BOOLEAN      NOT NULL DEFAULT FALSE,
    uploaded_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ── Application Pets ──────────────────────────────────────────────────────
CREATE TABLE application_pets (
    id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id UUID        NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
    pet_type       VARCHAR(50) NOT NULL CHECK (pet_type IN ('DOG','CAT','BIRD','FISH','OTHER')),
    breed          VARCHAR(100),
    name           VARCHAR(100),
    weight_lbs     DECIMAL(5,1),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── Application Vehicles ──────────────────────────────────────────────────
CREATE TABLE application_vehicles (
    id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id UUID        NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
    make           VARCHAR(100),
    model          VARCHAR(100),
    year           INTEGER,
    color          VARCHAR(50),
    license_plate  VARCHAR(50),
    state          VARCHAR(2),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── Residents ─────────────────────────────────────────────────────────────
CREATE TABLE residents (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id      UUID         NOT NULL REFERENCES tenants(id),
    user_id        UUID         REFERENCES users(id),
    first_name     VARCHAR(100) NOT NULL,
    last_name      VARCHAR(100) NOT NULL,
    email          VARCHAR(255) NOT NULL,
    phone          VARCHAR(50),
    date_of_birth  DATE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted_at     TIMESTAMPTZ
);

-- ── Resident Emergency Contacts ───────────────────────────────────────────
CREATE TABLE resident_emergency_contacts (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    resident_id UUID         NOT NULL REFERENCES residents(id) ON DELETE CASCADE,
    name        VARCHAR(255) NOT NULL,
    relationship VARCHAR(100),
    phone       VARCHAR(50)  NOT NULL,
    email       VARCHAR(255),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ── Lease Templates ───────────────────────────────────────────────────────
CREATE TABLE lease_templates (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID         REFERENCES tenants(id),  -- NULL = platform template
    name          VARCHAR(255) NOT NULL,
    template_type VARCHAR(100) NOT NULL
                  CHECK (template_type IN ('LEASE_AGREEMENT','PET_ADDENDUM','PARKING_ADDENDUM',
                                           'GUARANTOR_AGREEMENT','MOVE_IN_CHECKLIST',
                                           'RENEWAL_OFFER','NOTICE_TO_VACATE','UTILITY_ADDENDUM')),
    state_code    VARCHAR(2),
    version       INTEGER      NOT NULL DEFAULT 1,
    content       TEXT         NOT NULL,
    status        VARCHAR(50)  NOT NULL DEFAULT 'DRAFT'
                  CHECK (status IN ('DRAFT','UNDER_REVIEW','PUBLISHED','ARCHIVED')),
    published_by  UUID         REFERENCES users(id),
    published_at  TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted_at    TIMESTAMPTZ
);

-- ── Leases ────────────────────────────────────────────────────────────────
CREATE TABLE leases (
    id                UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id         UUID          NOT NULL REFERENCES tenants(id),
    application_id    UUID          REFERENCES applications(id),
    unit_id           UUID          NOT NULL REFERENCES units(id),
    template_id       UUID          REFERENCES lease_templates(id),
    template_version  INTEGER,
    parent_lease_id   UUID          REFERENCES leases(id),  -- for renewals
    lease_start       DATE          NOT NULL,
    lease_end         DATE          NOT NULL,
    monthly_rent      DECIMAL(12,2) NOT NULL,
    security_deposit  DECIMAL(12,2) NOT NULL,
    grace_period_days INTEGER       NOT NULL DEFAULT 5,
    late_fee_amount   DECIMAL(12,2),
    status            VARCHAR(50)   NOT NULL DEFAULT 'DRAFT'
                      CHECK (status IN ('DRAFT','GENERATED','SENT','PARTIALLY_SIGNED','SIGNED','ACTIVE','EXPIRED','TERMINATED')),
    activated_at      TIMESTAMPTZ,
    terminated_at     TIMESTAMPTZ,
    termination_reason TEXT,
    created_by        UUID          REFERENCES users(id),
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    deleted_at        TIMESTAMPTZ
);

-- ── Lease Parties (primary tenant, co-tenants, guarantors) ───────────────
CREATE TABLE lease_parties (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    lease_id      UUID        NOT NULL REFERENCES leases(id) ON DELETE CASCADE,
    resident_id   UUID        NOT NULL REFERENCES residents(id),
    party_type    VARCHAR(50) NOT NULL
                  CHECK (party_type IN ('PRIMARY_TENANT','CO_TENANT','GUARANTOR')),
    signing_order INTEGER     NOT NULL DEFAULT 1,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_lease_party UNIQUE (lease_id, resident_id)
);

-- ── Lease Charges ─────────────────────────────────────────────────────────
CREATE TABLE lease_charges (
    id            UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    lease_id      UUID          NOT NULL REFERENCES leases(id) ON DELETE CASCADE,
    charge_type   VARCHAR(100)  NOT NULL
                  CHECK (charge_type IN ('MONTHLY_RENT','SECURITY_DEPOSIT','PET_FEE',
                                         'PARKING_FEE','UTILITY_FEE','LATE_FEE','OTHER')),
    amount        DECIMAL(12,2) NOT NULL,
    frequency     VARCHAR(50)   NOT NULL
                  CHECK (frequency IN ('RECURRING','ONE_TIME','CONDITIONAL')),
    due_day       INTEGER,
    description   VARCHAR(255),
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

-- ── Lease Amendments ──────────────────────────────────────────────────────
CREATE TABLE lease_amendments (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    lease_id         UUID        NOT NULL REFERENCES leases(id),
    amendment_type   VARCHAR(100) NOT NULL
                     CHECK (amendment_type IN ('RENT_CHANGE','TERM_EXTENSION','ADDENDUM_ADDED','OTHER')),
    previous_value   TEXT,
    new_value        TEXT,
    effective_date   DATE        NOT NULL,
    status           VARCHAR(50) NOT NULL DEFAULT 'DRAFT'
                     CHECK (status IN ('DRAFT','SENT','SIGNED','ACTIVE')),
    created_by       UUID        REFERENCES users(id),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── Lease Documents ───────────────────────────────────────────────────────
CREATE TABLE lease_documents (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    lease_id        UUID        NOT NULL REFERENCES leases(id),
    document_type   VARCHAR(100) NOT NULL
                    CHECK (document_type IN ('LEASE_AGREEMENT','PET_ADDENDUM','PARKING_ADDENDUM',
                                             'GUARANTOR_AGREEMENT','MOVE_IN_CHECKLIST','RENEWAL_OFFER',
                                             'NOTICE_TO_VACATE','AMENDMENT','OTHER')),
    storage_key     VARCHAR(500) NOT NULL,
    version         INTEGER      NOT NULL DEFAULT 1,
    is_signed       BOOLEAN      NOT NULL DEFAULT FALSE,
    generated_by    UUID         REFERENCES users(id),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ── Signature Requests ────────────────────────────────────────────────────
CREATE TABLE signature_requests (
    id                 UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    lease_document_id  UUID        NOT NULL REFERENCES lease_documents(id),
    signer_id          UUID        NOT NULL REFERENCES residents(id),
    signing_order      INTEGER     NOT NULL DEFAULT 1,
    status             VARCHAR(50) NOT NULL DEFAULT 'PENDING'
                       CHECK (status IN ('PENDING','VIEWED','SIGNED','DECLINED')),
    decline_reason     TEXT,
    token_hash         VARCHAR(255) UNIQUE,
    token_expires_at   TIMESTAMPTZ,
    sent_at            TIMESTAMPTZ,
    viewed_at          TIMESTAMPTZ,
    completed_at       TIMESTAMPTZ,
    ip_address         VARCHAR(50),
    user_agent         TEXT,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── Notifications ─────────────────────────────────────────────────────────
CREATE TABLE notifications (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id        UUID         NOT NULL REFERENCES tenants(id),
    recipient_user_id UUID        REFERENCES users(id),
    recipient_email  VARCHAR(255) NOT NULL,
    event_type       VARCHAR(100) NOT NULL,
    channel          VARCHAR(50)  NOT NULL DEFAULT 'EMAIL'
                     CHECK (channel IN ('EMAIL','SMS','IN_APP')),
    subject          VARCHAR(255),
    body             TEXT,
    status           VARCHAR(50)  NOT NULL DEFAULT 'PENDING'
                     CHECK (status IN ('PENDING','SENT','DELIVERED','FAILED')),
    retry_count      INTEGER      NOT NULL DEFAULT 0,
    sent_at          TIMESTAMPTZ,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ── Renewal Offers ────────────────────────────────────────────────────────
CREATE TABLE renewal_offers (
    id              UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    lease_id        UUID          NOT NULL REFERENCES leases(id),
    tenant_id       UUID          NOT NULL REFERENCES tenants(id),
    proposed_rent   DECIMAL(12,2) NOT NULL,
    proposed_start  DATE          NOT NULL,
    proposed_end    DATE          NOT NULL,
    status          VARCHAR(50)   NOT NULL DEFAULT 'PENDING'
                    CHECK (status IN ('PENDING','ACCEPTED','DECLINED','EXPIRED')),
    sent_at         TIMESTAMPTZ,
    responded_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

-- ── Idempotency Keys ──────────────────────────────────────────────────────
CREATE TABLE idempotency_keys (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    idempotency_key VARCHAR(255) NOT NULL UNIQUE,
    endpoint        VARCHAR(255) NOT NULL,
    response_status INTEGER,
    response_body   TEXT,
    expires_at      TIMESTAMPTZ  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ── Indexes ───────────────────────────────────────────────────────────────
-- Properties
CREATE INDEX idx_properties_tenant_id  ON properties(tenant_id);
CREATE INDEX idx_properties_state      ON properties(state);

-- Buildings
CREATE INDEX idx_buildings_property_id ON buildings(property_id);
CREATE INDEX idx_buildings_tenant_id   ON buildings(tenant_id);

-- Units
CREATE INDEX idx_units_tenant_id       ON units(tenant_id);
CREATE INDEX idx_units_property_id     ON units(property_id);
CREATE INDEX idx_units_status          ON units(tenant_id, status);
CREATE INDEX idx_units_deleted_at      ON units(deleted_at) WHERE deleted_at IS NULL;

-- Prospects
CREATE INDEX idx_prospects_tenant_id   ON prospects(tenant_id);
CREATE INDEX idx_prospects_status      ON prospects(tenant_id, status);
CREATE INDEX idx_prospects_email       ON prospects(tenant_id, email);
CREATE INDEX idx_prospects_assigned_to ON prospects(assigned_to);

-- Applications
CREATE INDEX idx_applications_tenant_id   ON applications(tenant_id);
CREATE INDEX idx_applications_unit_id     ON applications(unit_id);
CREATE INDEX idx_applications_prospect_id ON applications(prospect_id);
CREATE INDEX idx_applications_status      ON applications(tenant_id, status);

-- Residents
CREATE INDEX idx_residents_tenant_id  ON residents(tenant_id);
CREATE INDEX idx_residents_email      ON residents(tenant_id, email);
CREATE INDEX idx_residents_user_id    ON residents(user_id);

-- Leases
CREATE INDEX idx_leases_tenant_id       ON leases(tenant_id);
CREATE INDEX idx_leases_unit_id         ON leases(unit_id);
CREATE INDEX idx_leases_status          ON leases(tenant_id, status);
CREATE INDEX idx_leases_application_id  ON leases(application_id);
CREATE INDEX idx_leases_lease_end       ON leases(lease_end) WHERE status = 'ACTIVE';

-- Lease parties
CREATE INDEX idx_lease_parties_lease_id    ON lease_parties(lease_id);
CREATE INDEX idx_lease_parties_resident_id ON lease_parties(resident_id);

-- Lease documents
CREATE INDEX idx_lease_documents_lease_id ON lease_documents(lease_id);

-- Signature requests
CREATE INDEX idx_sig_requests_document_id  ON signature_requests(lease_document_id);
CREATE INDEX idx_sig_requests_signer_id    ON signature_requests(signer_id);
CREATE INDEX idx_sig_requests_token_hash   ON signature_requests(token_hash);

-- Notifications
CREATE INDEX idx_notifications_tenant_id   ON notifications(tenant_id);
CREATE INDEX idx_notifications_status      ON notifications(status);
CREATE INDEX idx_notifications_event_type  ON notifications(event_type);

-- Renewal offers
CREATE INDEX idx_renewal_offers_lease_id   ON renewal_offers(lease_id);
CREATE INDEX idx_renewal_offers_status     ON renewal_offers(tenant_id, status);

-- Idempotency
CREATE INDEX idx_idempotency_expires_at ON idempotency_keys(expires_at);
