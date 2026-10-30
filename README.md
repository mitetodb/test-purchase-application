# Test Purchase Application

Web application for managing **test purchases** (mystery shopping orders): customers, shops, test purchase orders with items,
status workflow and history, attachments, and price calculation via a separate pricing microservice.

**Live:** https://testkauf.getinweb.de · Pricing service: [price-calculation-microservice](https://github.com/mitetodb/price-calculation-microservice)

## Features
- Roles: Admin, Account Manager, Sales Manager, Customer, Mystery Shopper (Spring Security, role-based UI)
- Test purchases: create/edit, items, status changes with history and optional email notification
- Customers & shops management, user management, profile with avatar upload
- Price calculation (service fee, postage) via the pricing microservice (Feign + `X-API-Key`)
- Attachments (PDF/images), password reset by email (optional SMTP)
- UI in **Bulgarian, English, German** (`?lang=bg|en|de`, navbar switcher, cookie)
- Flyway migrations, actuator health, OpenAPI (springdoc), MySQL backups

## Roles and Permissions

Server-side role-based access control enforced in controllers, services, and repository queries:

| Permission | ADMIN | ACCOUNT_MANAGER | SALES_MANAGER | CUSTOMER | MYSTERY_SHOPPER |
|------------|-------|-----------------|---------------|----------|-----------------|
| **Test Purchases** |
| View all test purchases | ✅ | Only managed customers' | Only owned clients' | Only own company's | Only assigned to them |
| Create test purchase | ✅ | ✅ (for managed customers) | ❌ | ✅ (own company only) | ❌ |
| Edit test purchase | ✅ | ✅ (managed customers) | ❌ | ❌ | ❌ |
| Delete test purchase | ✅ Hard delete | ⚠️ Soft delete (deactivate) | ⚠️ Soft delete (deactivate) | ⚠️ Soft delete (deactivate) | ⚠️ Soft delete (deactivate) |
| Change status | ✅ | ✅ | ❌ | ❌ | ✅ (own assignments) |
| Assign mystery shopper | ✅ | ✅ | ❌ | ❌ | ❌ |
| See mystery shopper name | ✅ | ✅ | ❌ | ❌ | ✅ |
| Preview price | ✅ | ✅ | ✅ | ✅ (own company) | ❌ |
| **Customers** |
| View customers | ✅ | Only managed | Only owned | ❌ | ❌ |
| Create customer | ✅ | ✅ (auto-managed) | ✅ (auto-owned) | ❌ | ❌ |
| Edit customer | ✅ | ✅ (managed only) | ✅ (owned only) | ❌ | ❌ |
| Delete customer | ✅ Hard delete | ⚠️ Soft delete (deactivate) | ⚠️ Soft delete (deactivate) | ❌ | ❌ |
| Set pricing | ✅ | ✅ | ✅ | ❌ | ❌ |
| **Shops** |
| View/manage shops | ✅ | ✅ | ❌ | ❌ | ❌ |
| **Users** |
| Manage users | ✅ | ❌ | ❌ | ❌ | ❌ |
| Delete users | ⚠️ Soft delete (deactivate) | ❌ | ❌ | ❌ | ❌ |
| Create invite tokens | ✅ | ✅ | ❌ | ❌ | ❌ |
| **Registration** |
| Public sign-up | ❌ Invite-only | ❌ Invite-only | ❌ Invite-only | ❌ Invite-only | ✅ Invite token required |

**Key Security Features:**
- **Invite-Only Registration**: `/register` requires a valid invite token (7-day expiry, single-use, SHA-256 hashed). Only ADMIN and ACCOUNT_MANAGER can create invite tokens. All public registrations assign MYSTERY_SHOPPER role server-side.
- **IDOR Protection**: Each role can only access their authorized resources (tested with comprehensive IDOR tests)
- **Soft Delete**: Non-admin delete operations deactivate records via `active` flags; inactive records are filtered from queries. Users are never hard-deleted.
- **Mystery Shopper Privacy**: `SALES_MANAGER` and `CUSTOMER` roles never see mystery shopper names in views, lists, dashboard, or API responses
- **Ownership Links**: 
  - `User.customer` links CUSTOMER role users to their company
  - `Customer.salesManager` links customers to their sales manager
  - `User.managedCustomers` links ACCOUNT_MANAGER to their managed customers
- **Account Manager Scope**: ACCOUNT_MANAGER can only create/edit test purchases and customers that they manage (enforced in service layer with IDOR tests)

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
export DB_URL=jdbc:mysql://localhost:3306/test_purchase_db DB_USERNAME=... DB_PASSWORD=... ADMIN_PASSWORD=...
mvn spring-boot:run
scripts/ci.sh                           # secret scan + tests + compose check (DOCKER_BUILD=1 for image)
```
Admin: set `ADMIN_PASSWORD` before the first start (created only if the users table is empty).

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
`ADMIN_PASSWORD` (the initial admin is created only when the users table is empty), `MAIL_ENABLED` (default `false`, so mail is a logged no-op; SMTP for ALL-INKL: `MAIL_HOST=w00d37b3.kasserver.com`, `MAIL_PORT=587` + `MAIL_STARTTLS=true` (used on Hetzner, which blocks outbound 465), or `465` + `MAIL_SSL=true` elsewhere, `MAIL_USERNAME`/`MAIL_PASSWORD` from env, `MAIL_FROM=testkauf@getinweb.de`), `DEMO_DATA_ENABLED`, `SWAGGER_ENABLED`.

**Database migrations.** Flyway runs `src/main/resources/db/migration`. `V1__baseline.sql` is the schema at the time Flyway was introduced.
Existing databases are baselined at v1 automatically (`baseline-on-migrate`), and Hibernate runs with `ddl-auto=validate` in prod.
If an old database created by `ddl-auto=update` fails validation, start it once with `JPA_DDL_AUTO=none`, then add a corrective `V2__*.sql`.

**Backups:** `deploy/backup-mysql.sh` (mysqldump → gzip, `RETENTION_DAYS=14` rotation, files saved with mode 600). Example cron line:
`15 3 * * * /opt/testkauf/test-purchase-application/deploy/backup-mysql.sh >> /var/log/testkauf-backup.log 2>&1`

**i18n:** BG / EN / DE message bundles (`messages_*.properties`). Switch the language with the navbar globe menu or `?lang=bg|en|de`; the choice is stored in a cookie.

**API docs:** springdoc is at `/swagger-ui.html` and `/v3/api-docs`. Both are off in prod unless `SWAGGER_ENABLED=true`.

**CI:** `scripts/ci.sh` runs a secret scan, `mvn verify`, a compose config check that fails if an internal port is published, and (with `DOCKER_BUILD=1`) the image build.
The Dockerfile is a multi-stage Java 17 build that runs as a non-root user.

## Documentation

Comprehensive documentation is available in the [`docs/`](docs/) directory:

### Project Documentation

- **[Master Plan](docs/MASTERPLAN.md)** - Development roadmap organized by priority (High, Medium, Later)
- **[Architecture](docs/ARCHITECTURE.md)** - System architecture with Mermaid diagrams, components, and data flows
- **[Roles & Permissions](docs/ROLES.md)** - Complete permission matrix for all five roles
- **[Deployment Guide](docs/DEPLOYMENT.md)** - Production deployment on Hetzner with Docker Compose
- **[Security](docs/SECURITY.md)** - Security practices, current implementation, and planned improvements
- **[Contributing](docs/CONTRIBUTING.md)** - Development workflow, code style, testing, and database migrations

### User Guides

User guides are available in **three languages** (Bulgarian, English, German):

| Role | English | Bulgarian | German |
|------|---------|-----------|--------|
| **Admin** | [EN](docs/guides/en/admin.md) | [BG](docs/guides/bg/admin.md) | [DE](docs/guides/de/admin.md) |
| **Account Manager** | [EN](docs/guides/en/account-manager.md) | [BG](docs/guides/bg/account-manager.md) | [DE](docs/guides/de/account-manager.md) |
| **Sales Manager** | [EN](docs/guides/en/sales-manager.md) | [BG](docs/guides/bg/sales-manager.md) | [DE](docs/guides/de/sales-manager.md) |
| **Customer** | [EN](docs/guides/en/customer.md) | [BG](docs/guides/bg/customer.md) | [DE](docs/guides/de/customer.md) |
| **Mystery Shopper** | [EN](docs/guides/en/mystery-shopper.md) | [BG](docs/guides/bg/mystery-shopper.md) | [DE](docs/guides/de/mystery-shopper.md) |
