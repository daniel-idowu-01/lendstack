-- LendStack core schema
-- Money: NUMERIC(19,2), NGN. IDs: UUID (except audit_log: BIGSERIAL).

CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(255) NOT NULL,
    phone         VARCHAR(32),
    role          VARCHAR(20)  NOT NULL CHECK (role IN ('BORROWER', 'LOAN_OFFICER', 'ADMIN')),
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- PII columns (bvn, nin, bank_account_number) hold AES-256-GCM ciphertext, never plaintext.
CREATE TABLE borrower_profiles (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL UNIQUE REFERENCES users (id),
    bvn                 VARCHAR(512),
    bvn_verified        BOOLEAN NOT NULL DEFAULT FALSE,
    nin                 VARCHAR(512),
    employment_status   VARCHAR(20),
    employer_name       VARCHAR(255),
    monthly_income      NUMERIC(19, 2),
    bank_account_number VARCHAR(512),
    bank_name           VARCHAR(255),
    date_of_birth       DATE,
    address             VARCHAR(255),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE lenders (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                VARCHAR(255) NOT NULL,
    type                VARCHAR(20)  NOT NULL CHECK (type IN ('INDIVIDUAL', 'INSTITUTION')),
    email               VARCHAR(255) NOT NULL UNIQUE,
    wallet_balance      NUMERIC(19, 2) NOT NULL DEFAULT 0,
    max_exposure        NUMERIC(19, 2) NOT NULL,
    current_exposure    NUMERIC(19, 2) NOT NULL DEFAULT 0,
    preferred_risk_tier VARCHAR(10)  NOT NULL DEFAULT 'MEDIUM'
        CHECK (preferred_risk_tier IN ('LOW', 'MEDIUM', 'HIGH')),
    active              BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT lenders_balance_nonneg CHECK (wallet_balance >= 0),
    CONSTRAINT lenders_exposure_nonneg CHECK (current_exposure >= 0)
);

CREATE TABLE loans (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reference             VARCHAR(32) NOT NULL UNIQUE,
    borrower_id           UUID NOT NULL REFERENCES users (id),
    amount                NUMERIC(19, 2) NOT NULL CHECK (amount > 0),
    purpose               VARCHAR(1000) NOT NULL,
    tenure_months         INT NOT NULL CHECK (tenure_months > 0),
    interest_rate_annual  NUMERIC(5, 2),
    status                VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN (
                              'DRAFT', 'SUBMITTED', 'UNDER_REVIEW', 'CREDIT_CHECK',
                              'PENDING_GUARANTOR', 'PENDING_COLLATERAL', 'APPROVED',
                              'DISBURSED', 'ACTIVE', 'DELINQUENT', 'DEFAULTED',
                              'CLOSED', 'REJECTED', 'WRITTEN_OFF')),
    risk_tier             VARCHAR(10) CHECK (risk_tier IN ('LOW', 'MEDIUM', 'HIGH', 'DECLINED')),
    credit_score          INT CHECK (credit_score BETWEEN 0 AND 100),
    guarantors_required   INT NOT NULL DEFAULT 0 CHECK (guarantors_required BETWEEN 0 AND 2),
    collateral_required   BOOLEAN NOT NULL DEFAULT FALSE,
    outstanding_principal NUMERIC(19, 2),
    submitted_at          TIMESTAMPTZ,
    approved_at           TIMESTAMPTZ,
    disbursed_at          TIMESTAMPTZ,
    closed_at             TIMESTAMPTZ,
    rejection_reason      VARCHAR(1000),
    version               BIGINT NOT NULL DEFAULT 0,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_loans_borrower ON loans (borrower_id);
CREATE INDEX idx_loans_status ON loans (status);

CREATE TABLE credit_assessments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    loan_id         UUID NOT NULL REFERENCES loans (id),
    score           INT NOT NULL CHECK (score BETWEEN 0 AND 100),
    risk_tier       VARCHAR(10) NOT NULL CHECK (risk_tier IN ('LOW', 'MEDIUM', 'HIGH', 'DECLINED')),
    breakdown       JSONB,
    overridden      BOOLEAN NOT NULL DEFAULT FALSE,
    override_reason VARCHAR(1000),
    assessed_by     UUID REFERENCES users (id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT override_needs_reason CHECK (NOT overridden OR override_reason IS NOT NULL)
);

CREATE INDEX idx_credit_assessments_loan ON credit_assessments (loan_id);

CREATE TABLE guarantors (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    loan_id        UUID NOT NULL REFERENCES loans (id),
    full_name      VARCHAR(255) NOT NULL,
    email          VARCHAR(255) NOT NULL,
    phone          VARCHAR(32),
    relationship   VARCHAR(100),
    occupation     VARCHAR(100),
    monthly_income NUMERIC(19, 2),
    status         VARCHAR(10) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'ACCEPTED', 'DECLINED', 'EXPIRED')),
    response_token VARCHAR(64) NOT NULL UNIQUE,
    requested_at   TIMESTAMPTZ,
    responded_at   TIMESTAMPTZ,
    expires_at     TIMESTAMPTZ,
    decline_reason VARCHAR(500),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_guarantors_loan ON guarantors (loan_id);
CREATE INDEX idx_guarantors_pending_expiry ON guarantors (expires_at) WHERE status = 'PENDING';

CREATE TABLE collaterals (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    loan_id             UUID NOT NULL REFERENCES loans (id),
    type                VARCHAR(20) NOT NULL
        CHECK (type IN ('PROPERTY', 'VEHICLE', 'FIXED_DEPOSIT', 'EQUIPMENT')),
    description         VARCHAR(1000) NOT NULL,
    estimated_value     NUMERIC(19, 2) NOT NULL CHECK (estimated_value > 0),
    verification_status VARCHAR(12) NOT NULL DEFAULT 'UNVERIFIED'
        CHECK (verification_status IN ('UNVERIFIED', 'VERIFIED', 'REJECTED')),
    verified_by         UUID REFERENCES users (id),
    verified_at         TIMESTAMPTZ,
    rejection_reason    VARCHAR(500),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_collaterals_loan ON collaterals (loan_id);

CREATE TABLE loan_fundings (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    loan_id          UUID NOT NULL REFERENCES loans (id),
    lender_id        UUID NOT NULL REFERENCES lenders (id),
    amount           NUMERIC(19, 2) NOT NULL CHECK (amount > 0),
    principal_repaid NUMERIC(19, 2) NOT NULL DEFAULT 0,
    interest_earned  NUMERIC(19, 2) NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (loan_id, lender_id)
);

CREATE INDEX idx_loan_fundings_lender ON loan_fundings (lender_id);

CREATE TABLE repayment_installments (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    loan_id            UUID NOT NULL REFERENCES loans (id),
    installment_number INT NOT NULL,
    due_date           DATE NOT NULL,
    principal_due      NUMERIC(19, 2) NOT NULL,
    interest_due       NUMERIC(19, 2) NOT NULL,
    penalty_due        NUMERIC(19, 2) NOT NULL DEFAULT 0,
    total_due          NUMERIC(19, 2) NOT NULL,
    amount_paid        NUMERIC(19, 2) NOT NULL DEFAULT 0,
    status             VARCHAR(10) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'PAID', 'OVERDUE', 'WAIVED')),
    paid_at            TIMESTAMPTZ,
    waived_by          UUID REFERENCES users (id),
    waived_reason      VARCHAR(500),
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (loan_id, installment_number)
);

CREATE INDEX idx_installments_loan ON repayment_installments (loan_id);
CREATE INDEX idx_installments_due ON repayment_installments (due_date, status);

CREATE TABLE payment_transactions (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    loan_id           UUID NOT NULL REFERENCES loans (id),
    installment_id    UUID NOT NULL REFERENCES repayment_installments (id),
    reference         VARCHAR(64) NOT NULL UNIQUE,
    amount            NUMERIC(19, 2) NOT NULL CHECK (amount > 0),
    status            VARCHAR(12) NOT NULL DEFAULT 'INITIALIZED'
        CHECK (status IN ('INITIALIZED', 'SUCCESS', 'FAILED', 'ABANDONED')),
    authorization_url VARCHAR(1000),
    channel           VARCHAR(32),
    paid_at           TIMESTAMPTZ,
    gateway_response  VARCHAR(500),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_payment_tx_loan ON payment_transactions (loan_id);

CREATE TABLE loan_documents (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    loan_id       UUID NOT NULL REFERENCES loans (id),
    collateral_id UUID REFERENCES collaterals (id),
    uploaded_by   UUID NOT NULL REFERENCES users (id),
    doc_type      VARCHAR(32) NOT NULL,
    file_name     VARCHAR(255) NOT NULL,
    content_type  VARCHAR(128) NOT NULL,
    storage_path  VARCHAR(512) NOT NULL,
    size_bytes    BIGINT NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_loan_documents_loan ON loan_documents (loan_id);

CREATE TABLE notification_outbox (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_email     VARCHAR(255) NOT NULL,
    recipient_name      VARCHAR(255),
    subject             VARCHAR(255) NOT NULL,
    body                VARCHAR(4000) NOT NULL,
    type                VARCHAR(32) NOT NULL,
    status              VARCHAR(10) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
    related_entity_type VARCHAR(32),
    related_entity_id   VARCHAR(64),
    sent_at             TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_notification_outbox_status ON notification_outbox (status);

CREATE TABLE system_config (
    config_key   VARCHAR(64) PRIMARY KEY,
    config_value VARCHAR(255) NOT NULL,
    value_type   VARCHAR(10) NOT NULL CHECK (value_type IN ('NUMBER', 'BOOLEAN', 'STRING')),
    description  VARCHAR(500),
    updated_by   VARCHAR(255),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================
-- Append-only audit trail. UPDATE/DELETE are blocked by trigger:
-- the table can only ever grow.
-- ============================================================
CREATE TABLE audit_log (
    id           BIGSERIAL PRIMARY KEY,
    entity_type  VARCHAR(40) NOT NULL,
    entity_id    VARCHAR(64) NOT NULL,
    action       VARCHAR(64) NOT NULL,
    performed_by VARCHAR(255) NOT NULL,
    old_value    JSONB,
    new_value    JSONB,
    reason       VARCHAR(1000),
    timestamp    TIMESTAMPTZ NOT NULL DEFAULT now(),
    ip_address   VARCHAR(64)
);

CREATE INDEX idx_audit_entity ON audit_log (entity_type, entity_id);
CREATE INDEX idx_audit_timestamp ON audit_log (timestamp);
CREATE INDEX idx_audit_performed_by ON audit_log (performed_by);

CREATE OR REPLACE FUNCTION prevent_audit_mutation() RETURNS trigger AS
$$
BEGIN
    RAISE EXCEPTION 'audit_log is append-only: % is not permitted', TG_OP;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_audit_log_immutable
    BEFORE UPDATE OR DELETE ON audit_log
    FOR EACH ROW
EXECUTE FUNCTION prevent_audit_mutation();
