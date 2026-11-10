# Admin and operations guide (Testkauf production)

Operations reference for the **Test Purchase Application** on the shared Hetzner host **baccardi**. Demo/staging data only in examples — never paste real customer data or secret values. Environment variable **names** are listed; values live in `deploy/.env` on the server (`chmod 600`).

**Live URL:** https://testkauf.getinweb.de  
**Install root:** `/opt/testkauf/test-purchase-application`  
**Pricing sibling repo:** `/opt/testkauf/price-calculation-microservice`

---

## Shared host rules (read first)

| Rule | Detail |
|------|--------|
| **Do not edit foreign nginx vhosts** | Host nginx already terminates TLS for other products. Only add or maintain **testkauf** using `deploy/nginx/testkauf.getinweb.de.conf`. |
| **Never touch salon stack** | The site **book.getinweb.de** and the **quoka** stack on this server are unrelated — do not restart, reconfigure, or deploy them while working on Testkauf. |
| **Do not publish MySQL ports** | Production compose binds the app to `127.0.0.1:${APP_HOST_PORT}` (default **8090**). MySQL and `pricing-svc` stay on the internal Docker network with **no** host port mapping. |
| **No GitHub Actions deploy** | Production updates are manual `git pull` + `docker compose` on the server (or your runbook automation), not CI deploy jobs. |

---

## Stack overview

```text
Internet ──HTTPS──> host nginx (80/443) ──proxy──> 127.0.0.1:8090 (app)
                                                      │
                        docker network "backend" ◄────┼────> pricing-svc :8081
                                                      │
                                                      └──> MySQL 8.4 (test_purchase_db + pricedb)
```

Compose file: `deploy/docker-compose.prod.yml`  
Env file: `deploy/.env` (copy from repo `.env.example`)

---

## Pre-deploy checklist

1. **Backup both databases** (see [BACKUP.md](BACKUP.md) and §Rollback below) — at minimum run `deploy/backup-mysql.sh` or your `backup-all` wrapper.
2. **Note current git SHA** on the server: `git rev-parse HEAD`.
3. **Confirm health:** `curl -sf http://127.0.0.1:${APP_HOST_PORT:-8090}/actuator/health`.
4. **Pull and rebuild only Testkauf:**

```bash
cd /opt/testkauf/test-purchase-application
git fetch origin master && git checkout master && git pull origin master
docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env up -d --build app pricing-svc
docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env ps
```

5. **Smoke test:** HTTPS login, one list page, optional attachment download.
6. **Flyway:** migrations run on app startup from `src/main/resources/db/migration/` (see README migration list). If startup fails on validation, stop and investigate before retrying.

---

## Rollback procedure

Use when a deploy breaks the app or a migration fails.

1. **Stop writers** (keep `db` up):

```bash
cd /opt/testkauf/test-purchase-application
docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env stop app pricing-svc
```

2. **Check out previous release** (known-good tag or commit):

```bash
git checkout <previous-commit-or-tag>
docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env up -d --build app pricing-svc
```

3. **If the database migrated forward and is incompatible**, restore SQL from backup (destructive to DB contents since backup time):

   - Restore **`test_purchase_db`** and **`pricedb`** using the steps in [BACKUP.md §6.2](BACKUP.md#62-restore-mysql-production-stack).
   - Restore **uploads** volume if needed (same section; use the corrected wipe command that removes dotfiles).

4. **Start services** and verify health and login.

5. **Document** incident time, backup set used, and commits involved.

---

## Environment configuration (names only)

From `.env.example` and `application.properties` — set values only in `deploy/.env`:

| Area | Variables |
|------|-----------|
| MySQL | `MYSQL_ROOT_PASSWORD`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, `PRICING_DB_NAME`, `PRICING_DB_USERNAME`, `PRICING_DB_PASSWORD` |
| App DB URL (local dev) | `DB_URL`, `JPA_DDL_AUTO`, `FLYWAY_ENABLED` |
| Pricing API | `PRICING_API_KEY`, `PRICING_API_BASE_URL`, `PRICING_BUILD_CONTEXT`, `PRICING_IMAGE`, `PRICING_LOG_LEVEL` |
| App | `SPRING_PROFILES_ACTIVE`, `APP_BASE_URL`, `APP_HOST_PORT`, `ADMIN_PASSWORD`, `ADMIN_EMAIL`, `DEMO_DATA_ENABLED`, `DEMO_PURCHASES`, `DEMO_USERS`, `SWAGGER_ENABLED`, `UPLOAD_DIR`, `UPLOAD_ATTACHMENT_ALLOWED_TYPES`, `UPLOAD_AVATAR_ALLOWED_TYPES` |
| Mail | `MAIL_ENABLED`, `MAIL_HOST`, `MAIL_PORT`, `MAIL_SSL`, `MAIL_STARTTLS`, `MAIL_SMTP_AUTH`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`, `MAIL_VISIT_APPOINTMENT_ENABLED` |
| Deadlines | `DEADLINE_REMINDER_ENABLED`, `DEADLINE_REMINDER_DAYS_BEFORE`, `DEADLINE_REMINDER_CRON` |
| Monitoring | `MONITORING_BACKUP_CHECK_ENABLED`, `MONITORING_BACKUP_MARKER_PATH`, `MONITORING_BACKUP_SCAN_DIRECTORY`, `MONITORING_BACKUP_MAX_AGE_HOURS`, `MONITORING_SERVER_ERROR_*`, `MONITORING_ALERT_*` |
| Security | `APP_TOTP_ENCRYPTION_KEY`, `APP_TOTP_ISSUER`, `app.security.login.*`, `app.security.password.min-length` (default 12), `app.security.two-factor.max-session-failed-attempts` |
| Lists | `app.list-pagination.default-page-size`, `app.list-pagination.max-page-size`, `app.list-export.max-rows` |

Initial admin user is created only when the `users` table is empty and `ADMIN_PASSWORD` is set (minimum 12 characters).

### Docker Compose: which `deploy/.env` keys reach the app container

`docker compose --env-file deploy/.env` uses that file for **variable substitution in the compose YAML**. Only keys listed under the `app` service `environment:` block in `deploy/docker-compose.prod.yml` are passed into the running Spring Boot container.

`DEADLINE_REMINDER_*`, `MAIL_VISIT_APPOINTMENT_ENABLED`, and `MONITORING_*` are bound in `application.properties`. They reach the container when that `environment` block lists them. The compose env passthrough adds exactly these keys, with defaults matching `application.properties`:

| Variable | Default when forwarded |
|----------|------------------------|
| `MAIL_VISIT_APPOINTMENT_ENABLED` | `false` |
| `DEADLINE_REMINDER_ENABLED` | `false` |
| `DEADLINE_REMINDER_DAYS_BEFORE` | `7,1,0` |
| `DEADLINE_REMINDER_CRON` | `0 0 8 * * ?` |
| `MONITORING_BACKUP_CHECK_ENABLED` | `false` |
| `MONITORING_BACKUP_MARKER_PATH` | empty |
| `MONITORING_BACKUP_SCAN_DIRECTORY` | empty |
| `MONITORING_BACKUP_MAX_AGE_HOURS` | `26` |
| `MONITORING_SERVER_ERROR_WINDOW_SECONDS` | `300` |
| `MONITORING_SERVER_ERROR_THRESHOLD` | `20` |
| `MONITORING_ALERT_ENABLED` | `false` |
| `MONITORING_ALERT_WEBHOOK_URL` | empty |
| `MONITORING_ALERT_EMAIL_TO` | empty |
| `MONITORING_ALERT_CRON` | `0 */5 * * * ?` |

Listing the keys does **not** turn anything on. Outbound mail, deadline reminders, visit-appointment mail, backup checks, monitoring alerts, and off-site backup stay off until an operator sets the relevant flag in `deploy/.env`. A compose file that does not list a key still ignores that key in `deploy/.env`.

`MONITORING_BACKUP_MARKER_PATH` and `MONITORING_BACKUP_SCAN_DIRECTORY` are paths **inside** the app container. Host backup files are not mounted. The read-only volume examples under `app.volumes` stay commented; uncomment them only when backup checks are intentionally enabled, and point the env vars at the container paths (suggested marker: `/var/lib/testkauf/backup-last-success.timestamp`, see [MONITORING.md](MONITORING.md)).

---

## Two-factor authentication (Admin / Account Manager)

TOTP is **required** for roles **ADMIN** and **ACCOUNT_MANAGER** after password login. Recovery codes are one-time; when a new set is issued, previous unused codes are **invalidated** (timestamp + reason in DB) — rows are **not** deleted.

### End-user recovery (no admin)

1. After password login, open **Verify authenticator** (`/2fa/verify`).
2. Use **Use a recovery code** with a saved code, or enter the current TOTP from the authenticator app.
3. After enrollment, save the 10 recovery codes shown once on `/2fa/recovery-codes`.

### Admin: login lockout (failed password / rate limit)

1. Go to **Users** (`/users`).
2. For a locked account, click **Unlock** (`POST /users/unlock/{id}`).
3. Ask the user to sign in again (and complete 2FA if applicable).

### Admin: lost authenticator and no recovery codes

There is **no** “reset 2FA” button in the UI. Break-glass procedure (production maintenance window, audit the action):

1. Confirm identity of the user out of band.
2. Optionally **Unlock** if the account is locked.
3. Reset TOTP flags in MySQL so the user must **re-enroll** on next login:

```bash
cd /opt/testkauf/test-purchase-application
# Replace USERNAME with the account username (demo example only)
docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env exec db \
  sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" test_purchase_db -e "
    UPDATE users
    SET totp_enabled = 0,
        totp_secret_encrypted = NULL,
        totp_last_time_step = NULL
    WHERE username = '\''demo_admin'\'';"'
```

4. User logs in with password → redirected to **`/2fa/enroll`** → new TOTP and recovery codes.
5. Old `user_totp_recovery_code` rows remain for audit; consumable codes are those without `used_at` and without `invalidated_at`.

---

## Backups and restore

Full plan: **[BACKUP.md](BACKUP.md)** (schedules, restic, integrity checks, GDPR notes).

Quick nightly script already on many installs:

```cron
15 3 * * * /opt/testkauf/test-purchase-application/deploy/backup-mysql.sh >> /var/log/testkauf-backup.log 2>&1
```

Ensure **`pricedb`** is included in every backup and restore test (not only `test_purchase_db`).

---

## Monitoring (health.getinweb.de)

External uptime checks for missing backups and elevated 5xx rates are configured on **health.getinweb.de** (not in this repo). The app exposes Actuator components `backup` and `serverErrors` on **`/actuator/health`**; enable checks with `MONITORING_BACKUP_CHECK_ENABLED` and optional alert stubs with `MONITORING_ALERT_ENABLED` (default **false**).

Full wiring guide: **[MONITORING.md](MONITORING.md)** (marker file path, JSON fields, env names only).

---

## Related documentation

- [DEPLOYMENT.md](DEPLOYMENT.md) — first-time install and nginx/TLS notes  
- [SECURITY.md](SECURITY.md) — RBAC, uploads, session security  
- [ROLES.md](ROLES.md) — permission matrix  
- [STATUS.md](STATUS.md) — live features vs deferred (#10)  
- User guides: [docs/user-guide/](user-guide/)
