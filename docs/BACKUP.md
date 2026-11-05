# Backup and restore plan (Testkauf production)

This document is the **operational plan** for backing up and restoring the Test Purchase Application stack on a **single Hetzner server** running Docker Compose under `/opt/testkauf`. It satisfies [issue #10](https://github.com/mitetodb/test-purchase-application/issues/10): database dumps, uploaded files, encrypted off-server copies, failure visibility, and a tested restore procedure.

**Scope:** MySQL 8.4 (`test_purchase_db`, `pricedb`), Docker volume `uploads` (avatars under `uploads/avatars/`, attachments under `uploads/`), and **host-only** configuration in `deploy/.env`. It does **not** cover nginx, other vhosts, or unrelated services on the same machine.

**Important:** Scripts under `deploy/backup/` and the examples below are **reference only**. Nothing in this repository is wired into cron, systemd, or compose on your server until an operator installs it deliberately. The existing nightly job (if any) may still use `deploy/backup-mysql.sh` alone until you extend it per this plan.

---

## 1. What to back up

| Asset | Location | Why | Method |
|-------|----------|-----|--------|
| Main app database | MySQL `test_purchase_db` | Users, customers, purchases, metadata | `mysqldump --single-transaction` **inside** the `db` container |
| Pricing database | MySQL `pricedb` | Price rules used by `pricing-svc` | Same as above |
| Uploaded files | Docker named volume `uploads` → `/app/uploads` in `app` | Avatars, receipt/photo attachments (not in SQL dumps) | Archive volume contents (e.g. `tar` via a one-off container) |
| Compose + env **names** | Git repo at `/opt/testkauf/test-purchase-application` | Rebuild stack | Git pull; **secrets are not in git** |
| Secrets | `deploy/.env` on server (`chmod 600`, root or deploy user) | DB passwords, API keys, mail | **Separate** encrypted backup; never commit, never put in restic/borg unencrypted without client-side crypto |
| MySQL data directory | Docker volume `db-data` | Optional full-disk recovery | **Not** required if logical dumps + uploads are healthy; optional extra `tar` of volume for speed |

### 1.1 Database dumps (logical)

Use consistent, InnoDB-safe dumps from the running container (password read **inside** the container, not on the host command line):

```bash
docker compose -f "${COMPOSE_FILE}" --env-file "${ENV_FILE}" exec -T db \
  sh -c 'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" \
    --single-transaction --routines --triggers --no-tablespaces "$0"' \
  test_purchase_db | gzip -9 > "${BACKUP_DIR}/test_purchase_db-${TS}.sql.gz"
```

Repeat for `pricedb`. The production helper `deploy/backup-mysql.sh` already implements this pattern for both databases.

After each dump:

- `gzip -t` on the file
- Optional: `zcat … | head` and check for `Dump completed` footer

### 1.2 Uploads volume (files)

The compose file mounts one volume for all user files:

```yaml
# deploy/docker-compose.prod.yml (excerpt)
volumes:
  - uploads:/app/uploads
```

Back up the **volume**, not only a host path (there is no bind mount in production compose):

```bash
docker run --rm \
  -v testkauf_uploads:/data:ro \
  -v "${BACKUP_DIR}:/backup" \
  alpine:3.20 \
  tar -czf "/backup/uploads-${TS}.tar.gz" -C /data .
```

Adjust the volume name if `docker volume ls` shows a different project prefix (e.g. `testkauf_uploads` for project name `testkauf`).

### 1.3 `deploy/.env` (secrets)

- Keep a **second copy** outside the server (password manager, encrypted USB, or encrypted archive in an off-site restic/borg repo).
- Encrypt before leaving the server, e.g. `gpg --symmetric --cipher-algo AES256 deploy/.env` with a passphrase stored **only** off-server.
- Rotate backup encryption passphrases when you rotate production secrets.
- **Never** store plaintext `.env` in git, ticket attachments, or unencrypted object storage.

Reference scripts: `deploy/backup/backup-env-encrypted.example.sh` (operator must edit paths; not executed by default).

---

## 2. Where backups live on the server

Suggested layout (root-only):

Create the **`env/`** subdirectory before first encrypted `.env` backup (restic/borg examples assume it exists):

```bash
BACKUP_ROOT=/root/testkauf-backups
install -d -m 700 "${BACKUP_ROOT}"/{mysql,uploads,env,daily,weekly,monthly,logs}
```

Layout:

```text
/root/testkauf-backups/
├── mysql/              # *.sql.gz (daily lineage)
├── uploads/            # uploads-*.tar.gz
├── env/                # deploy.env.gpg (optional, encrypted) — create with install -d above
├── daily/              # symlinks or hardlinks to latest daily sets
├── weekly/
├── monthly/
└── logs/               # optional per-run logs
```

Directory mode `700`, file mode `600`. Existing installs may use `/root/testkauf-backups/` with SQL files at the top level; migrating to subdirectories is optional.

---

## 3. Schedule and retention

Goals from issue #10: nightly coverage, bounded retention, and tiers for longer recovery.

| Tier | When | What | Retention |
|------|------|------|-----------|
| **Daily** | Every night (e.g. 03:15 local) | Full set: both DB dumps + uploads archive | **14 days** |
| **Weekly** | Sunday after daily (or Monday 04:00) | Copy/link that day’s daily set into `weekly/` | **8 weeks** |
| **Monthly** | First day of month | Copy/link into `monthly/` | **6 months** |

Implementation options:

1. **restic** with `--keep-daily 14 --keep-weekly 8 --keep-monthly 6` on a single repository (recommended if off-site tool is restic).
2. **find -mtime** rotation per directory (see `deploy/backup/rotate-local.example.sh`).

Run **daily** jobs in a fixed order: DB dumps → uploads → integrity checks → off-site sync → alert on non-zero exit.

Example cron (placeholders — install only after review):

```cron
# /etc/cron.d/testkauf-backup (example — not shipped active)
SHELL=/bin/bash
PATH=/usr/local/sbin:/usr/local/bin:/sbin:/bin:/usr/sbin:/usr/bin

15 3 * * * root COMPOSE_FILE=/opt/testkauf/test-purchase-application/deploy/docker-compose.prod.yml \
  ENV_FILE=/opt/testkauf/test-purchase-application/deploy/.env \
  BACKUP_ROOT=/root/testkauf-backups \
  /opt/testkauf/test-purchase-application/deploy/backup/backup-all.example.sh \
  >> /var/log/testkauf-backup.log 2>&1
```

---

## 4. Off-server encrypted copy

Assume a **Hetzner Storage Box** (or similar) reachable over **SSH/SFTP**, used only for backups.

### 4.1 Principles

- **Client-side encryption** before data leaves the app server (restic and borg both encrypt at rest in the repo).
- **Dedicated SSH key** for backup (`backup@testkauf@storagebox.example.com`), forced command or restricted to upload path, no shell on Storage Box if provider allows.
- **Restic/borg repository password** (or borg key) stored **off-server** (password manager + paper backup); losing it means losing backups.
- Repo holds: `mysql/`, `uploads/`, optionally `deploy.env.gpg` — not plaintext `.env`.

### 4.2 restic over SFTP (example)

On the server (once):

```bash
export RESTIC_REPOSITORY=sftp:backup@testkauf@storagebox.example.com:23/backups/testkauf
export RESTIC_PASSWORD_FILE=/root/.config/testkauf/restic-password   # mode 600; content from password manager
export RESTIC_CACHE_DIR=/var/cache/restic-testkauf

restic init   # once
```

After local backup:

```bash
restic backup /root/testkauf-backups/mysql /root/testkauf-backups/uploads /root/testkauf-backups/env \
  --tag testkauf,nightly

restic forget --keep-daily 14 --keep-weekly 8 --keep-monthly 6 --prune
restic check --read-data-subset=5%
```

See `deploy/backup/offsite-restic.example.sh`.

### 4.3 borg over SSH (alternative)

```bash
export BORG_REPO=ssh://backup@testkauf@storagebox.example.com/./backups/testkauf.borg
export BORG_PASSPHRASE_COMMAND='cat /root/.config/testkauf/borg-passphrase'  # file mode 600

borg create --stats ::'{hostname}-{now:%Y-%m-%d}' /root/testkauf-backups/mysql /root/testkauf-backups/uploads
borg prune --keep-daily=14 --keep-weekly=8 --keep-monthly=6
borg check --verify-data
```

Use **either** restic **or** borg, not both writing the same files without coordination.

---

## 5. Integrity checks

Run after every backup job (automated in `deploy/backup/backup-all.example.sh`):

| Check | Command / action |
|-------|------------------|
| Gzip integrity | `gzip -t /root/testkauf-backups/mysql/*.sql.gz` |
| Tar integrity | `tar -tzf uploads-*.tar.gz >/dev/null` |
| SQL sanity | `zcat latest.sql.gz \| tail -5` contains `Dump completed` |
| Restic | `restic check` (nightly quick, weekly `--read-data-subset`) |
| Borg | `borg check` |
| Size drift | Alert if today’s total size is &lt;50% of 7-day median (possible incomplete dump) |

Log success lines with ISO timestamps; any failure must trigger alerting (section 7).

---

## 6. Restore runbook

**Use demo/staging data for practice.** Production restore stops write traffic and replaces live data — schedule a maintenance window.

### 6.1 Prerequisites

- Known-good backup set: `test_purchase_db-*.sql.gz`, `pricedb-*.sql.gz`, `uploads-*.tar.gz`
- `deploy/.env` available (from encrypted off-site copy if server lost)
- Compose file path: `/opt/testkauf/test-purchase-application/deploy/docker-compose.prod.yml`

### 6.2 Restore MySQL (production stack)

1. **Stop writers** (keep `db` running):

   ```bash
   cd /opt/testkauf/test-purchase-application
   docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env stop app pricing-svc
   ```

2. **Restore main database:**

   ```bash
   gunzip -c /root/testkauf-backups/mysql/test_purchase_db-YYYYMMDD-HHMMSS.sql.gz | \
     docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env exec -T db \
     sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" test_purchase_db'
   ```

3. **Restore pricing database** (same pattern, database name `pricedb`).

4. **Restore uploads volume** (destructive — replaces current files):

   ```bash
   docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env stop app
   docker run --rm \
     -v testkauf_uploads:/data \
     -v /root/testkauf-backups/uploads:/backup:ro \
     alpine:3.20 \
     sh -c 'find /data -mindepth 1 -maxdepth 1 -exec rm -rf {} + && tar -xzf /backup/uploads-YYYYMMDD-HHMMSS.tar.gz -C /data'
   ```

5. **Start services:**

   ```bash
   docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env up -d app pricing-svc
   ```

6. **Verify:** `curl -sf http://127.0.0.1:${APP_HOST_PORT:-8090}/actuator/health`, log in, open a known attachment/avatar URL.

### 6.3 Throwaway restore test (required periodically)

Do **not** touch production volumes. Use an isolated compose project:

```bash
DEMO_SQL=/path/to/demo/test_purchase_db.sql.gz \
DEMO_PRICEDB_SQL=/path/to/demo/pricedb.sql.gz \
DEMO_UPLOADS_TAR=/path/to/demo/uploads.tar.gz \
/opt/testkauf/test-purchase-application/deploy/backup/restore-test.example.sh
```

Details in `deploy/backup/restore-test.example.sh`: starts temporary MySQL + empty `uploads`, restores **copies** of backup files for **`test_purchase_db` and `pricedb`**, runs sanity queries and `tar -tzf` listing, then `docker compose down -v`.

**Cadence:** at least **once after initial backup setup**, then **quarterly**, and after any restore script change. Record date and operator in your runbook log (spreadsheet or ticket).

### 6.4 Full server loss

1. Provision new server, install Docker, clone repos, restore `deploy/.env` from encrypted backup.
2. `docker compose … up -d db` until healthy.
3. Restore both SQL dumps and uploads as in §6.2.
4. Pull off-site restic/borg snapshot if local disks are gone.
5. Reconfigure nginx/TLS on the host (out of scope for this doc).

---

## 7. Monitoring and alerting

Backup failure must be **visible** (issue #10). After backup automation is installed on the server, also wire **application** health if using health.getinweb.de:

| Signal | Mechanism |
|--------|-----------|
| Cron exit code | Wrap scripts with `set -euo pipefail`; last line `echo OK` |
| Log file | Append to `/var/log/testkauf-backup.log`; ship to central logs if available |
| **Actuator `backup` component** | Enable `MONITORING_BACKUP_CHECK_ENABLED=true` and maintain a marker file or scan directory — see [MONITORING.md](MONITORING.md) |
| Heartbeat | Optional [Healthchecks.io](https://healthchecks.io) `curl -fsS -m 10 --retry 5 https://hc.example/ping/<uuid>` on success only |
| Email | `mail -s "testkauf backup FAILED" ops@example.com < /var/log/testkauf-backup.log` on failure |
| Metrics | Push exit code to Prometheus node textfile collector |

Example wrapper: `deploy/backup/notify-on-failure.example.sh`.

**Alert on:** non-zero exit, missing new file for &gt;26 hours, `restic check` / `borg check` failure, integrity check failure.

---

## 8. GDPR, anonymization, and backups

Personal data includes profiles, customers, purchases, **avatars**, and **attachments** ([SECURITY.md](SECURITY.md)). Formal Art. 17 erasure is handled **manually** by anonymizing the live database and files — not by deleting historical rows that other records reference.

**Interaction with backups:**

1. **Live system:** After anonymization, production DB and `uploads/` no longer contain the erased subject’s personal data (replace avatar files, redact/anonymize columns per internal procedure).
2. **Backups:** Any **snapshot taken before anonymization** still contains the old personal data until that backup age **expires** (daily/weekly/monthly/off-site retention).
3. **Restore:** Restoring an old backup can **reintroduce** personal data that was anonymized later. Treat restore as a data-processing decision; prefer forward recovery; if restore is required, re-run anonymization on the restored system if the subject’s erasure still applies.
4. **Off-site repos:** Same retention rules apply; erasure requests do not require immediate deletion of every historical snapshot unless legal counsel directs a targeted purge (restic/borg `forget` by snapshot ID — expert operation).

Document anonymization date and backup retention in privacy records so stakeholders know **maximum** residual retention in backups.

---

## 9. Security checklist

- [ ] Backup directories root-only (`700`) and files `600`
- [ ] No secrets in git; CI secret scan stays clean
- [ ] SSH key for Storage Box dedicated, passphrase protected
- [ ] Restic/borg encryption password only off-server
- [ ] Off-site repo not publicly listable
- [ ] Restore tests use **demo** copies, not production `.env` in tickets
- [ ] Nightly job logs reviewed or alerted

---

## 10. Reference scripts (not auto-run)

| File | Purpose |
|------|---------|
| `deploy/backup/backup-all.example.sh` | Orchestrate DB + uploads + integrity |
| `deploy/backup/backup-uploads.example.sh` | Archive `uploads` volume |
| `deploy/backup/rotate-local.example.sh` | Daily/weekly/monthly file retention |
| `deploy/backup/offsite-restic.example.sh` | Push to Storage Box via restic |
| `deploy/backup/restore-test.example.sh` | Throwaway restore verification |
| `deploy/backup/notify-on-failure.example.sh` | Alert hook pattern |
| `deploy/backup/backup-env-encrypted.example.sh` | GPG wrapper for `.env` |
| `deploy/backup/README.md` | Install notes |

Existing MySQL-only script (may already be cron-installed): `deploy/backup-mysql.sh`.

---

## 11. Acceptance mapping (issue #10)

| Criterion | How this plan addresses it |
|-----------|------------------------------|
| Nightly uploads + DB with retention | §1–3, `backup-all.example.sh`, retention table |
| Encrypted off-server copy | §4, `offsite-restic.example.sh` |
| Backup failure visible | §7, `notify-on-failure.example.sh` |
| Restore documented and tested | §6, `restore-test.example.sh` |
| No credentials in repo | Placeholders only; real secrets in server `.env` |

Implementing the plan on the server is a separate change (cron, restic install, Storage Box account) and is intentionally **not** done by merging documentation alone — **awaiting operator cost/decision for Hetzner Storage Box** per [STATUS.md](STATUS.md).
