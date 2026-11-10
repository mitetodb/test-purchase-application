# Project status (English)

Snapshot of what is **live in the codebase on `master`** (through PRs **#75–#81**), what operators must **enable explicitly**, and what remains **deferred**. Demo placeholders only — no production secrets.

**Live app:** https://testkauf.getinweb.de  
**Roadmap:** [GitHub Projects — Testkauf](https://github.com/users/mitetodb/projects/9)

Bulgarian and German summaries: [status/bg.md](status/bg.md), [status/de.md](status/de.md).

---

## Database (Flyway)

Latest migration on `master`: **V15** (`V15__invoices.sql`). There is **no** new Flyway script for monitoring (#81) — health signals are application-only.

| Version | File | Topic |
|---------|------|--------|
| V1 | `V1__baseline.sql` | Baseline |
| V2 | `V2__add_rbac_schema.sql` | RBAC |
| V3 | `V3__add_invite_tokens.sql` | Invite tokens |
| V4 | `V4__add_login_lockout.sql` | Login lockout |
| V5 | `V5__login_rate_limit_key_hash.sql` | Rate limit keys |
| V6 | `V6__user_deactivation_only.sql` | User deactivation |
| V7 | `V7__add_totp_two_factor.sql` | TOTP 2FA |
| V7_1 | `V7_1__totp_recovery_code_invalidation.sql` | Recovery code invalidation |
| V8 | `V8__entity_number_sequences.sql` | Entity numbers |
| V9 | `V9__audit_log.sql` | Audit log |
| V10 | `V10__customer_deactivation_only.sql` | Customer deactivation |
| V11 | `V11__shop_deactivation_and_test_purchase_archive.sql` | Shop deactivation, TP archive |
| V12 | `V12__test_purchase_deadlines_and_reminders.sql` | Deadlines + reminder tracking |
| V13 | `V13__visit_appointment.sql` | Shop visit appointment fields |
| V14 | `V14__questionnaire_and_scores.sql` | Questionnaire responses + scores |
| V15 | `V15__invoices.sql` | Invoices and line items |

---

## Live product features (merged)

| Area | Status | Notes |
|------|--------|--------|
| RBAC, invite-only registration, IDOR tests | Live | Five roles; mystery shopper names hidden from Customer / Sales Manager |
| Login rate limit, lockout, Admin unlock | Live | |
| Password min 12, TOTP for Admin + Account Manager | Live | Recovery codes; re-issue invalidates unused codes |
| Audit log UI (`/audit`) | Live | Includes profile/password/TOTP-related events (#75), invoices (#12), and earlier actions |
| Entity numbers, pagination, filters, CSV/Excel export | Live | Export capped by `app.list-export.max-rows` |
| Deactivate users/customers/shops; archive test purchases | Live | No hard delete in UI |
| Upload magic-byte validation | Live | Attachments retained (no delete API) |
| **PDF report per test purchase** | Live (#11) | `GET /testpurchases/view/{id}/report.pdf` (same access as viewing the purchase) |
| **Deadlines on test purchases** | Live (#16) | Optional date on create/edit; overdue indicator in UI |
| **Deadline reminder e-mail** | Code live; **sending off by default** | Requires `MAIL_ENABLED=true` **and** `DEADLINE_REMINDER_ENABLED=true` |
| **Visit appointment booking** | Live (#24) | One slot per purchase on detail page; confirm flow |
| **Visit appointment e-mail** | Code live; **off by default** | `MAIL_VISIT_APPOINTMENT_ENABLED=true` and `MAIL_ENABLED=true` |
| **Scored questionnaire** | Live (#17) | `/testpurchases/{id}/questionnaire` — Admin, Account Manager, assigned Mystery Shopper |
| **Client score trends** | Live (#17) | `/score-trends` — Admin, Account Manager, Sales Manager, Customer (scoped data) |
| **Invoices + monthly statements** | Live (#12) | `/invoices`, `/invoices/statement`; PDF download; Admin/AM manage lifecycle |
| **Monitoring health components** | Live (#18) | Actuator `backup` + `serverErrors` on `/actuator/health` |
| **In-app monitoring alerts** | Code live; **off by default** | `MONITORING_ALERT_ENABLED=true` (+ webhook and/or mail) |
| Pricing microservice integration | Live | Feign + `X-API-Key` |
| i18n BG / EN / DE | Live | |
| OpenAPI / Swagger | Live when enabled | Default `SWAGGER_ENABLED=false` |

---

## E-mail and alerting (default OFF)

Until operators set the flags in `deploy/.env` **and** the deployed `deploy/docker-compose.prod.yml` lists those keys under `app.environment`, the app **does not send** these messages (they are logged or skipped). The compose env passthrough adds the keys and leaves the defaults off; see [ADMIN-OPS.md](ADMIN-OPS.md).

| Feature | Env vars (names only) | Default |
|---------|------------------------|---------|
| All outbound mail | `MAIL_ENABLED` | `false` |
| Deadline reminders | `DEADLINE_REMINDER_ENABLED` (+ mail) | `false` |
| Visit appointment notifications | `MAIL_VISIT_APPOINTMENT_ENABLED` (+ mail) | `false` |
| Monitoring alert stub | `MONITORING_ALERT_ENABLED` | `false` |

Password reset and status-change mail still follow `MAIL_ENABLED` when operators turn mail on.

See `.env.example`, [ADMIN-OPS.md](ADMIN-OPS.md), and [MONITORING.md](MONITORING.md).

---

## Deferred / not automated in repo

| Item | Issue | Status |
|------|-------|--------|
| **Storage Box off-site backup + production cron** | [#10](https://github.com/mitetodb/test-purchase-application/issues/10) | **Plan in [BACKUP.md](BACKUP.md)**; example scripts under `deploy/backup/`. Server install and Hetzner Storage Box cost await **Dimitar’s decision** — merging docs does not enable cron. |
| **health.getinweb.de check wiring** | [#18](https://github.com/mitetodb/test-purchase-application/issues/18) | App exposes signals; external dashboard configured outside this repo. |
| Expanded RBAC edge-case tests | [#19](https://github.com/mitetodb/test-purchase-application/issues/19) | Ongoing |

---

## Documentation map

| Doc | Purpose |
|-----|---------|
| [ROLES.md](ROLES.md) | Permission matrix (invoices, questionnaire, etc.) |
| [user-guide/](user-guide/) | Role guides BG / EN / DE |
| [ADMIN-OPS.md](ADMIN-OPS.md) | Deploy, rollback, 2FA break-glass |
| [BACKUP.md](BACKUP.md) | Backup/restore plan (#10) |
| [MONITORING.md](MONITORING.md) | Actuator components and env |
| [SECURITY.md](SECURITY.md) | Security practices |

**Do not merge or deploy from this documentation PR alone.** Production updates remain manual (`git pull` + compose on baccardi); no GitHub Actions deploy.
