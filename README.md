# Test Purchase Application

Web application for managing **test purchases** (mystery shopping orders): customers, shops, test purchase orders with items,
status workflow and history, attachments, and price calculation via a separate pricing microservice.

**Live:** https://testkauf.getinweb.de · Pricing service: [price-calculation-microservice](https://github.com/mitetodb/price-calculation-microservice)

## Features
- Roles: Admin, Account Manager, Sales Manager, Customer, Mystery Shopper (Spring Security, server-side RBAC)
- Test purchases: create/edit, items, optional **deadline** date, status changes with history and optional email notification; **archive** (no hard delete)
- **PDF report** per test purchase (`/testpurchases/view/{id}/report.pdf`); list **CSV/Excel export** (capped by `app.list-export.max-rows`)
- **Shop visit appointment** on purchase detail (schedule / confirm); optional visit e-mail when enabled on server
- **Scored questionnaire** after visits; **score trends** dashboard (`/score-trends`) for client analytics
- **Invoices** and **monthly statements** per client (`/invoices`, `/invoices/statement`); invoice PDF; lifecycle DRAFT → ISSUED → PAID / CANCELLED
- Customers, shops, and users: **deactivate/reactivate only** (no hard delete in UI)
- TOTP **2FA** for Admin and Account Manager; one-time **recovery codes** (invalidated on re-issue, rows retained)
- Login **rate limiting** and account **lockout**; Admin **Unlock** on user list
- **Audit log** (Admin): filter by action, entity, actor, date (includes profile/password/TOTP, invoices, and data changes)
- Sequential **entity numbers** (users, customers, shops, test purchases, invoices)
- List **pagination**, **filters**, and export on major lists
- **Invite-only** registration for mystery shoppers (`/register?token=…`)
- **Password policy:** minimum length 12 (`app.security.password.min-length`)
- Upload **magic-byte** validation (attachments and avatars), size limits via multipart settings
- Price calculation (service fee, postage) via the pricing microservice (Feign + `X-API-Key`)
- Attachments retained with purchases (**no delete** endpoint — returns 404)
- **Monitoring signals** on `/actuator/health` (`backup`, `serverErrors`) for external checks — optional in-app alerts default **off**
- **Deadline reminder e-mails** scheduled in app but default **off** (`DEADLINE_REMINDER_ENABLED=false`; requires `MAIL_ENABLED=true`)
- UI in **Bulgarian, English, German** (`?lang=bg|en|de`, navbar switcher, cookie)
- Flyway migrations through **V15**, actuator health, OpenAPI (springdoc), MySQL backup scripts (full off-site plan: [docs/BACKUP.md](docs/BACKUP.md))

See **[docs/STATUS.md](docs/STATUS.md)** for live vs deferred features (e.g. issue #10 Storage Box backup awaits operator decision).

## Roles and Permissions

Server-side role-based access control enforced in controllers, services, and repository queries:

| Permission | ADMIN | ACCOUNT_MANAGER | SALES_MANAGER | CUSTOMER | MYSTERY_SHOPPER |
|------------|-------|-----------------|---------------|----------|-----------------|
| **Test Purchases** |
| View all test purchases | ✅ | Only managed customers' | Only owned clients' | Only own company's | Only assigned to them |
| Create test purchase | ✅ | ✅ (for managed customers) | ❌ | ✅ (own company only) | ❌ |
| Edit test purchase | ✅ | ✅ (managed customers) | ❌ | ❌ | ❌ |
| Archive test purchase | ✅ | ❌ | ❌ | ❌ | ❌ |
| Deactivate shop | ✅ | ❌ | ❌ | ❌ | ❌ |
| Change status | ✅ | ✅ | ❌ | ❌ | ✅ (own assignments) |
| Assign mystery shopper | ✅ | ✅ | ❌ | ❌ | ❌ |
| See mystery shopper name | ✅ | ✅ | ❌ | ❌ | ✅ |
| Preview price | ✅ | ✅ | ✅ | ✅ (own company) | ❌ |
| **Customers** |
| View customers | ✅ | Only managed | Only owned | ❌ | ❌ |
| Create customer | ✅ | ✅ (auto-managed) | ✅ (auto-owned) | ❌ | ❌ |
| Edit customer | ✅ | ✅ (managed only) | ✅ (owned only) | ❌ | ❌ |
| Deactivate customer | ✅ (list) | ❌ (Admin UI) | ❌ | ❌ | ❌ |
| Reactivate customer | ✅ | ❌ | ❌ | ❌ | ❌ |
| Set pricing | ✅ | ✅ | ✅ | ❌ | ❌ |
| **Shops** |
| View/manage shops | ✅ | ✅ | ❌ | ❌ | ❌ |
| Deactivate / reactivate shop | ✅ | ❌ | ❌ | ❌ | ❌ |
| **Users** |
| Manage users | ✅ | ❌ | ❌ | ❌ | ❌ |
| Delete users | ❌ Deactivate only | ❌ | ❌ | ❌ | ❌ |
| Create invite tokens | ✅ | ✅ | ❌ | ❌ | ❌ |
| **Registration** |
| Public sign-up | ❌ Invite-only | ❌ Invite-only | ❌ Invite-only | ❌ Invite-only | ✅ Invite token required |

**Key Security Features:**
- **Invite-Only Registration**: `/register` requires a valid invite token (7-day expiry, single-use, SHA-256 hashed). Only ADMIN and ACCOUNT_MANAGER can create invite tokens. All public registrations assign MYSTERY_SHOPPER role server-side.
- **IDOR Protection**: Each role can only access their authorized resources (tested with comprehensive IDOR tests)
- **No hard delete in UI:** Users, customers, and shops are deactivated; test purchases are archived; attachments are retained. GDPR Art. 17 erasure is manual anonymization by the owner.
- **Mystery Shopper Privacy**: `SALES_MANAGER` and `CUSTOMER` roles never see mystery shopper names in views, lists, dashboard, or API responses
- **Ownership Links**: 
  - `User.customer` links CUSTOMER role users to their company
  - `Customer.salesManager` links customers to their sales manager
  - `User.managedCustomers` links ACCOUNT_MANAGER to their managed customers
- **Account Manager Scope**: ACCOUNT_MANAGER can only create/edit test purchases and customers that they manage (enforced in service layer with IDOR tests)

See [docs/ROLES.md](docs/ROLES.md) for the complete permission matrix.

## Architecture
```
 Browser ──HTTPS──> host nginx (80/443, Let's Encrypt)
                         │  proxy_pass 127.0.0.1:8090
                         ▼
               ┌──────────────────────┐        docker network "backend" (internal)
               │ app  (Spring Boot 3, │──Feign + X-API-Key──> pricing-svc :8081/api
               │ Thymeleaf, Java 17)  │                          │
               └─────────┬────────────┘                          │
                         │ JDBC                                  │ JDBC
                         ▼                                       ▼
                    MySQL 8.4  (db: test_purchase_db, pricedb)  — no published ports
                         ▲
               cron: deploy/backup-mysql.sh (gzip, rotation)
  optional: SMTP (ALL-INKL 587/STARTTLS) · Mailpit (--profile mail, UI not public)
```

## Local run
Requirements: Java 17, Maven 3.9, MySQL 8 (or Docker).
```bash
cp .env.example .env                    # fill in values; never commit .env
docker compose up -d --build            # app on http://localhost:8080 (+ MySQL on 127.0.0.1:3307, pricing-svc)
# or without Docker for the app:
export DB_URL=jdbc:mysql://localhost:3306/test_purchase_db DB_USERNAME=root DB_PASSWORD=<database-password> ADMIN_PASSWORD=<strong-password>
mvn spring-boot:run
scripts/ci.sh                           # secret scan + tests + compose check (DOCKER_BUILD=1 for image)
```
Admin: set `ADMIN_PASSWORD` (min 12 characters) before the first start (created only if the users table is empty).

## Production deployment (testkauf.getinweb.de)

**Stack** (`deploy/docker-compose.prod.yml`): `app` (this repo), `pricing-svc` (built from the sibling repo
[`price-calculation-microservice`](https://github.com/mitetodb/price-calculation-microservice), or set `PRICING_IMAGE`) and `db` (MySQL 8.4, one instance with
two databases and least-privilege users) on an **internal** Docker network. Every service has a `mem_limit` and a healthcheck.
MySQL and pricing-svc publish **no ports**. The app is bound only to `127.0.0.1:${APP_HOST_PORT}` (default 8090).
Mailpit is available as an optional profile (`--profile mail`), and its UI is not published.

```bash
# quickest: as root on the server ->  bash deploy/server-setup.sh --certbot   (idempotent)
# manual: with both repos side by side, e.g. /opt/testkauf/{test-purchase-application,price-calculation-microservice}
cd test-purchase-application
cp .env.example deploy/.env && chmod 600 deploy/.env   # fill in strong secrets (openssl rand -base64 32)
docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env up -d --build
docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env ps     # all healthy?
```

**HTTPS / reverse proxy.** The server already runs another stack whose proxy owns 80/443, so this stack does **not** start its own proxy.
See `deploy/nginx/testkauf.getinweb.de.conf`:
- **Option A (host nginx):** copy the vhost to `/etc/nginx/sites-available`, enable it, then run `certbot --nginx -d testkauf.getinweb.de`. The upstream is `127.0.0.1:8090`.
- **Option B (proxy in Docker):** add the proxy's external network to `app` (see the commented `proxy` network), remove `app.ports`, and point the proxy at `http://app:8080`.

> **Shared host / IPv6:** after `certbot --nginx`, delete any `listen [::]:443 ssl ipv6only=on` / `listen [::]:80` lines it added to *our* site file. If another site on the same nginx has an AAAA record but no IPv6 listener, our IPv6 listener becomes the IPv6 default server and that site's IPv6 visitors receive the testkauf certificate (TLS name mismatch). `deploy/server-setup.sh` strips them automatically.

> **Clone permissions:** clone with a normal umask (022). With umask 077 the MySQL container (uid 999) cannot read `deploy/mysql-init` and the DB never becomes healthy.

Set a DNS A record for `testkauf.getinweb.de` pointing at the server before you request the certificate.

**Configuration** is env only (`.env.example` lists every variable). Key settings:
`SPRING_PROFILES_ACTIVE=prod`, `APP_BASE_URL` (used in password-reset links), `PRICING_API_KEY` (sent as `X-API-Key`; it must match the pricing service),
`ADMIN_PASSWORD` (the initial admin is created only when the users table is empty; min 12 characters required), `MAIL_ENABLED` (default `false`, so mail is a logged no-op; SMTP for ALL-INKL: `MAIL_HOST=w00d37b3.kasserver.com`, `MAIL_PORT=587` + `MAIL_STARTTLS=true` (used on Hetzner, which blocks outbound 465), or `465` + `MAIL_SSL=true` elsewhere, `MAIL_USERNAME`/`MAIL_PASSWORD` from env, `MAIL_FROM=testkauf@getinweb.de`), `DEMO_DATA_ENABLED`, `SWAGGER_ENABLED`.

**Database migrations.** Flyway runs scripts in `src/main/resources/db/migration/` in order:

| Version | File |
|---------|------|
| V1 | `V1__baseline.sql` |
| V2 | `V2__add_rbac_schema.sql` |
| V3 | `V3__add_invite_tokens.sql` |
| V4 | `V4__add_login_lockout.sql` |
| V5 | `V5__login_rate_limit_key_hash.sql` |
| V6 | `V6__user_deactivation_only.sql` |
| V7 | `V7__add_totp_two_factor.sql` |
| V7_1 | `V7_1__totp_recovery_code_invalidation.sql` |
| V8 | `V8__entity_number_sequences.sql` |
| V9 | `V9__audit_log.sql` |
| V10 | `V10__customer_deactivation_only.sql` |
| V11 | `V11__shop_deactivation_and_test_purchase_archive.sql` |
| V12 | `V12__test_purchase_deadlines_and_reminders.sql` |
| V13 | `V13__visit_appointment.sql` |
| V14 | `V14__questionnaire_and_scores.sql` |
| V15 | `V15__invoices.sql` |

Monitoring (#81) adds **no** new Flyway version — health components are application-only.

`V1__baseline.sql` is the schema at the time Flyway was introduced.
Existing databases are baselined at v1 automatically (`baseline-on-migrate`), and Hibernate runs with `ddl-auto=validate` in prod.
If an old database created by `ddl-auto=update` fails validation, start it once with `JPA_DDL_AUTO=none`, then add a corrective `V2__*.sql`.

**Backups:** `deploy/backup-mysql.sh` (mysqldump → gzip, `RETENTION_DAYS=14` rotation, files saved with mode 600). Example cron line:
`15 3 * * * /opt/testkauf/test-purchase-application/deploy/backup-mysql.sh >> /var/log/testkauf-backup.log 2>&1`

**i18n:** BG / EN / DE message bundles (`messages_*.properties`). Switch the language with the navbar globe menu or `?lang=bg|en|de`; the choice is stored in a cookie.

**API docs:** springdoc is at `/swagger-ui.html` and `/v3/api-docs`. Both are off by default unless `SWAGGER_ENABLED=true`.

**CI:** `scripts/ci.sh` runs a secret scan, `mvn verify`, a compose config check that fails if an internal port is published, and (with `DOCKER_BUILD=1`) the image build.
The Dockerfile is a multi-stage Java 17 build that runs as a non-root user.

## Documentation

Comprehensive documentation is available in the [`docs/`](docs/) directory.

📋 **[Testkauf Roadmap](https://github.com/users/mitetodb/projects/9)** - Track progress on GitHub Projects

### Project Documentation

- **[Status (live vs deferred)](docs/STATUS.md)** - Feature wave #75–#81, Flyway V15, e-mail defaults off; [BG](docs/status/bg.md) / [DE](docs/status/de.md) summaries
- **[Original Vision](docs/ORIGINAL_VISION.md)** - Original project goals and feature implementation status
- **[Master Plan](docs/MASTERPLAN.md)** - Development roadmap organized by priority (High, Medium, Later)
- **[Architecture](docs/ARCHITECTURE.md)** - System architecture with Mermaid diagrams, components, and data flows
- **[Roles & Permissions](docs/ROLES.md)** - Complete permission matrix for all five roles
- **[Deployment Guide](docs/DEPLOYMENT.md)** - Production deployment on Hetzner with Docker Compose
- **[Admin & operations](docs/ADMIN-OPS.md)** - baccardi host, pre-deploy backup, rollback, 2FA break-glass (env key names only)
- **[Backup plan](docs/BACKUP.md)** - MySQL, uploads, off-site copy, restore runbook
- **[Security](docs/SECURITY.md)** - Security practices, current implementation, and planned improvements
- **[Contributing](docs/CONTRIBUTING.md)** - Development workflow, code style, testing, and database migrations

### User Guides

User guides live under **`docs/user-guide/{en,bg,de}/`** (Admin, Account Manager, test buyer, client):

| Role | English | Bulgarian | German |
|------|---------|-----------|--------|
| **Admin** | [EN](docs/user-guide/en/admin.md) | [BG](docs/user-guide/bg/admin.md) | [DE](docs/user-guide/de/admin.md) |
| **Account Manager** | [EN](docs/user-guide/en/account-manager.md) | [BG](docs/user-guide/bg/account-manager.md) | [DE](docs/user-guide/de/account-manager.md) |
| **Test buyer (Mystery Shopper)** | [EN](docs/user-guide/en/test-buyer.md) | [BG](docs/user-guide/bg/test-buyer.md) | [DE](docs/user-guide/de/test-buyer.md) |
| **Client (Customer)** | [EN](docs/user-guide/en/client.md) | [BG](docs/user-guide/bg/client.md) | [DE](docs/user-guide/de/client.md) |

Legacy copies under `docs/guides/` redirect here. **Sales Manager** permissions are documented in [ROLES.md](docs/ROLES.md) only.
