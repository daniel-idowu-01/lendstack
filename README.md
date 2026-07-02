# LendStack — Loan Management System (Nigerian market)

Full-stack LMS for individual personal loans with CBN-aware compliance: full
lifecycle state machine, rule-based credit scoring, multi-lender funding,
guarantors with 72h consent windows, collateral verification, reducing-balance
amortization, Paystack repayments, and an append-only audit trail.

## Stack

- **Backend** — Java 21 / Spring Boot 3.3 (Maven), PostgreSQL 16, Flyway,
  Spring Security + JWT, MapStruct, springdoc OpenAPI. `backend/`
- **Frontend** — Next.js (App Router) + TypeScript, Tailwind CSS, shadcn-style
  UI kit, TanStack Query, React Hook Form + Zod, Axios. `frontend/`
- **Payments** — Paystack (checkout + HMAC-SHA512-verified webhooks).

## Quick start (Docker)

```bash
cp .env.example .env
# Fill in: JWT_SECRET (32+ chars), PII_ENCRYPTION_KEY (openssl rand -base64 32),
# PAYSTACK_SECRET_KEY (test key)
docker compose up --build
```

- Frontend: http://localhost:3000
- API + Swagger: http://localhost:8080/swagger-ui.html

With `SPRING_PROFILES_ACTIVE=dev` (the default), demo data is seeded:

| Role         | Email                | Password      |
|--------------|----------------------|---------------|
| Admin        | admin@lendstack.ng   | Admin@1234    |
| Loan officer | officer@lendstack.ng | Officer@1234  |
| Borrower     | amaka@example.com    | Borrower@1234 |

Plus three funded lenders across the LOW/MEDIUM/HIGH risk tiers.

## The loan lifecycle

```
DRAFT → SUBMITTED → UNDER_REVIEW → CREDIT_CHECK → PENDING_GUARANTOR
      → PENDING_COLLATERAL → APPROVED → DISBURSED → ACTIVE ⇄ DELINQUENT

UNDER_REVIEW / CREDIT_CHECK → REJECTED                (terminal)
PENDING_GUARANTOR → UNDER_REVIEW                      (guarantor declined/expired)
ACTIVE → CLOSED                                       (fully repaid — automatic)
DELINQUENT → DEFAULTED → WRITTEN_OFF | CLOSED
```

Every transition is audit-logged; anything outside the graph requires an ADMIN
override with a mandatory reason (also logged). The `audit_log` table is
append-only, enforced by a Postgres trigger and a package-private repository.

## Compliance notes (CBN / NDPC)

- Interest rate cap, tenure bounds (1–24 months), penalty rate, grace period,
  guarantor tiers and collateral threshold live in `system_config` — seeded
  from env vars, edited live by ADMIN at `/admin` (audited), never hardcoded.
- BVN linkage is mandatory: applications cannot pass SUBMITTED without one.
- Amortization uses the **reducing balance** method only.
- BVN/NIN/account numbers are AES-256-GCM encrypted at rest, masked in every
  log line (Logback converter) and in audit snapshots, and only ever returned
  masked to clients.

## Stubs to replace for production

| Stub | Where | Replace with |
|------|-------|--------------|
| BVN verification | `integration/bvn/StubBvnVerificationService` | NIBSS iValidate / licensed aggregator |
| Email/SMS delivery | `notification/NotificationWorker` | Termii, SendGrid, SES… |
| Disbursement transfer | `repayment/DisbursementService#executeTransfer` | Paystack Transfers / NIP |
| Lender wallet funding | `lender/LenderService#topUpWallet` | Reconciled bank inflows |

## Local development without Docker

Backend (needs Postgres on :5432 and JDK 21 — or use the Maven image):

```bash
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Frontend:

```bash
cd frontend && npm install && npm run dev
```

Paystack webhooks in local dev: expose :8080 (e.g. ngrok) and point the
dashboard webhook to `/api/v1/payments/webhook/paystack`. The redirect-callback
verify path also settles payments, so checkout works even without webhooks.
