# Monitoring and alerts (issue #18)

This application exposes **operational signals** for external uptime checks (for example **health.getinweb.de**, operated by Dimitar). We **do not** configure that platform from this repository, and we **must not** change host nginx vhosts or unrelated stacks (salon / quoka) while wiring checks.

All examples use **demo placeholders** — no secrets, URLs, or mailboxes belong in git.

---

## What the app reports

Spring Boot Actuator aggregates custom health components into the public endpoint:

| URL (via nginx) | `GET /actuator/health` |
|-----------------|------------------------|
| Allowed through testkauf nginx | Yes — only this path (see `deploy/nginx/testkauf.getinweb.de.conf`) |
| Auth | None (same as today) |
| Response | Top-level `status` (`UP` / `DOWN`) — component details hidden in prod (`management.endpoint.health.show-details=never`) |

Loopback checks on the server (`curl http://127.0.0.1:8090/actuator/health`) see the same aggregate status. Component names are stable for operators who enable details in a non-prod profile:

| Component | Meaning |
|-----------|---------|
| `backup` | Last successful backup younger than **26 h** (configurable) when backup checks are enabled |
| `serverErrors` | In-process count of HTTP **5xx** responses inside this JVM over a sliding window |

**Scope limits**

- **Backup:** reads a **marker file** and/or the newest `*.sql.gz` in a host directory you mount or copy into the container. It does **not** run backup jobs.
- **5xx rate:** counts responses returned **by this Spring app** (including `/error` for unhandled exceptions). Nginx-only 502/504 to upstream are **not** visible here — configure separate nginx log checks on health.getinweb.de if needed.

---

## Environment variables (names only)

Set these in `deploy/.env` on the server (see `.env.example`). Defaults keep backup checks and outbound alerts **off**.

| Property | Env var | Default | Purpose |
|----------|---------|---------|---------|
| `app.monitoring.backup-check-enabled` | `MONITORING_BACKUP_CHECK_ENABLED` | `false` | Require fresh backup for `backup` component |
| `app.monitoring.backup-marker-path` | `MONITORING_BACKUP_MARKER_PATH` | empty | Marker file (see below) |
| `app.monitoring.backup-scan-directory` | `MONITORING_BACKUP_SCAN_DIRECTORY` | empty | Optional newest `*.sql.gz` mtime fallback |
| `app.monitoring.backup-max-age-hours` | `MONITORING_BACKUP_MAX_AGE_HOURS` | `26` | Fail `backup` when older |
| `app.monitoring.server-errors.window-seconds` | `MONITORING_SERVER_ERROR_WINDOW_SECONDS` | `300` | Sliding window for 5xx counter |
| `app.monitoring.server-errors.threshold` | `MONITORING_SERVER_ERROR_THRESHOLD` | `20` | Fail `serverErrors` when count ≥ threshold |
| `app.monitoring.alert.enabled` | `MONITORING_ALERT_ENABLED` | `false` | Enable optional webhook/email stub |
| `app.monitoring.alert.webhook-url` | `MONITORING_ALERT_WEBHOOK_URL` | empty | POST target when alerts enabled |
| `app.monitoring.alert.email-to` | `MONITORING_ALERT_EMAIL_TO` | empty | Recipient when alerts **and** `MAIL_ENABLED=true` |
| `app.monitoring.alert.cron` | `MONITORING_ALERT_CRON` | `0 */5 * * * ?` | Alert evaluation schedule |

---

## Backup marker file (recommended)

After a successful backup orchestrator run, write **one line** — an ISO-8601 instant in UTC — to a file the app container can read read-only.

**Suggested path (document only):** `/var/lib/testkauf/backup-last-success.timestamp`

**Format example (demo):**

```text
2026-11-05T03:15:02Z
```

Reference cron snippet (not executed from git):

```bash
MARKER="/var/lib/testkauf/backup-last-success.timestamp"
date -u +"%Y-%m-%dT%H:%M:%SZ" > "$MARKER"
chmod 600 "$MARKER"
```

Mount that file or directory into the `app` service in compose when enabling `MONITORING_BACKUP_CHECK_ENABLED=true`.

**Alternative:** set `MONITORING_BACKUP_SCAN_DIRECTORY` to a read-only mount of `/root/testkauf-backups/mysql` (newest `*.sql.gz` modification time). Prefer the marker when both DB and uploads must succeed — see [BACKUP.md](BACKUP.md).

---

## Wiring health.getinweb.de (operator checklist)

1. Enable backup checks on the server: `MONITORING_BACKUP_CHECK_ENABLED=true` and set `MONITORING_BACKUP_MARKER_PATH` (and/or scan directory mount).
2. Ensure nightly backup cron updates the marker after **both** DB dumps (and uploads if you use `backup-all`).
3. Add an HTTP check targeting `https://testkauf.getinweb.de/actuator/health` (or loopback URL on baccardi if the dashboard runs there).
4. Alert when HTTP status ≠ 200 or JSON `status` ≠ `UP`.
5. Optional: separate check on nginx access logs for 5xx if upstream errors must be covered.
6. Optional mail/webhook from **this app**: set `MONITORING_ALERT_ENABLED=true` plus webhook or mail env vars. **Default is off** — no outbound calls until explicitly enabled (same policy as deadline reminders).

---

## Optional in-app alert stub

When `MONITORING_ALERT_ENABLED=true`, a scheduled task re-evaluates health. On transition to **unhealthy**, it may:

- `POST` JSON `{ "source": "testkauf", "summary": "...", "issues": [ ... ] }` to `MONITORING_ALERT_WEBHOOK_URL`
- Send plain-text mail to `MONITORING_ALERT_EMAIL_TO` only if `MAIL_ENABLED=true`

No webhook URL and no mail recipient → logs only. Re-alerting is suppressed until health recovers and fails again.

---

## Related docs

- [BACKUP.md](BACKUP.md) — backup layout, 26 h alerting expectation (§7)
- [ADMIN-OPS.md](ADMIN-OPS.md) — deploy checklist and health smoke test
