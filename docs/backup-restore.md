# Nightly backups and restore (phase 1)

Phase 1 of [issue #10](https://github.com/mitetodb/test-purchase-application/issues/10): back up the uploads volume on the same server as the existing MySQL dump, keep 14 days, make a failed run visible, and record one restore drill on synthetic data.

This does not install cron, logrotate, or a health-check hook. It does not copy backups off the server. Phase 2 (encrypted off-site copy) is a future step and is not configured here.

`deploy/backup-mysql.sh` stays MySQL-only so the live 03:15 UTC job keeps its current behavior. Uploads are `deploy/backup-uploads.sh`.

## What is backed up

| Asset | Where it lives | Script | Archive |
| --- | --- | --- | --- |
| Database `test_purchase_db` | MySQL service `db` in `deploy/docker-compose.prod.yml` | `deploy/backup-mysql.sh` | `/root/testkauf-backups/mysql/test_purchase_db-YYYYMMDD-HHMMSS.sql.gz` |
| Database `pricedb` | same server | `deploy/backup-mysql.sh` | `/root/testkauf-backups/mysql/pricedb-YYYYMMDD-HHMMSS.sql.gz` |
| Uploaded files | Docker volume `testkauf_uploads`, mounted at `/app/uploads` in the app container | `deploy/backup-uploads.sh` | `/root/testkauf-backups/uploads/uploads-YYYYMMDD-HHMMSS.tar.gz` and a sibling `.sha256` file |

The uploads read uses a one-off container. The volume is mounted read-only, so the app is not stopped and the backup process cannot change live files:

```bash
docker run --rm -v testkauf_uploads:/data:ro -v /root/testkauf-backups/uploads:/backup alpine:3.20 \
  tar -czf /backup/uploads-YYYYMMDD-HHMMSS.tar.gz.tmp -C /data .
```

The script writes that archive to a `.tmp` name, then renames it, runs `gzip -t` and `tar -tzf`, and writes `sha256sum` output to `uploads-YYYYMMDD-HHMMSS.tar.gz.sha256` (mode 600). Timestamps are UTC. A non-blocking `flock` on `BACKUP_DIR/.lock` refuses a second run while one is still going.

Passwords stay in `deploy/.env` on the server (mode 600). `deploy/backup-uploads.sh` does not read that file. Do not commit `.env`, checksums of production archives, or backup files.

## Retention

Both scripts default to `RETENTION_DAYS=14` and delete with `find -mtime +14`:

- MySQL: `*.sql.gz` under `BACKUP_DIR` (production: `/root/testkauf-backups/mysql`)
- Uploads: `uploads-*.tar.gz` and the matching `.sha256` files under `/root/testkauf-backups/uploads`

A partial `uploads-*.tar.gz.tmp` older than one day is removed. The fresh archive is not.

## Visible failure

Each uploads run appends one status line to `/var/log/testkauf-backup.log` and rewrites `/root/testkauf-backups/LAST_STATUS` with the same line. Cron does not need a mailer. There is no `MAILTO`.

Success:

```text
2026-11-26T03:25:07Z OK uploads /root/testkauf-backups/uploads/uploads-20261126-032501.tar.gz sha256=<64 hex> size_bytes=<n> hook=off
```

Failure (exit code is the failing command's status, or 1):

```text
2026-11-26T03:25:07Z FAILED uploads volume testkauf_uploads does not exist
```

A monitor can alert when `LAST_STATUS` matches ` FAILED ` or when that file is older than a day. `hook=off` means no health URL was set.

`BACKUP_HEALTH_URL` is empty by default and the script does not call the network. If you set it, a successful run sends HTTP GET to that URL (`hook=ok` or `hook=failed` on the OK line). A failed run GETs the URL with `/fail` appended. A hook error does not change the backup result: the archive is still the source of truth, and a successful archive with a dead hook exits 0 with `hook=failed`.

Example logrotate config (not installed): [`deploy/logrotate/testkauf-backup.example`](../deploy/logrotate/testkauf-backup.example). It uses `copytruncate` because both scripts append and there is no daemon to signal.

The MySQL script does not write `LAST_STATUS`. Its cron line already appends stdout to the same log. Leave that job as it is.

## Cron example (do not install from git)

Root crontab, 03:25 UTC, after the existing 03:15 MySQL dump. The uploads script appends its own log line, so this entry has no shell redirect (a redirect would copy progress into the log a second time).

```cron
15 3 * * * BACKUP_DIR=/root/testkauf-backups/mysql COMPOSE_FILE=/opt/testkauf/test-purchase-application/deploy/docker-compose.prod.yml ENV_FILE=/opt/testkauf/test-purchase-application/deploy/.env /usr/local/bin/testkauf-backup-mysql.sh >> /var/log/testkauf-backup.log 2>&1
25 3 * * * BACKUP_DIR=/root/testkauf-backups/uploads UPLOADS_VOLUME=testkauf_uploads /usr/local/bin/testkauf-backup-uploads.sh
```

The 03:15 line is the job already described by `deploy/server-setup.sh`. That script still installs only the MySQL line. Re-running it refreshes the MySQL line and leaves a `testkauf-backup-uploads.sh` line in place.

## Verify an archive

```bash
cd /root/testkauf-backups/uploads
sha256sum --strict -c uploads-YYYYMMDD-HHMMSS.tar.gz.sha256
gzip -t uploads-YYYYMMDD-HHMMSS.tar.gz
tar -tzf uploads-YYYYMMDD-HHMMSS.tar.gz
gzip -t /root/testkauf-backups/mysql/test_purchase_db-YYYYMMDD-HHMMSS.sql.gz
gzip -t /root/testkauf-backups/mysql/pricedb-YYYYMMDD-HHMMSS.sql.gz
```

`sha256sum -c` must print `OK` and exit 0. Do not restore an archive that fails the checksum.

## Restore

Practice on a copy first (see the drill below). A production restore replaces database rows and files. Stop writers before the restore. Backup itself does not need a stop.

1. Stop the app container (leave `db` running). From the repo checkout:

   ```bash
   docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env stop app
   ```

2. Restore `test_purchase_db` (repeat with `pricedb` and that dump):

   ```bash
   gunzip -c /root/testkauf-backups/mysql/test_purchase_db-YYYYMMDD-HHMMSS.sql.gz \
     | docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env exec -T db \
         sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" test_purchase_db'
   ```

   The password is read inside the container from `MYSQL_ROOT_PASSWORD`. It is not placed on the host command line.

3. Check row counts you care about, for example `SELECT COUNT(*) FROM <table>`.

4. Replace uploads only after `sha256sum -c` succeeds. This deletes current files in the volume, then extracts the archive:

   ```bash
   docker run --rm \
     -v testkauf_uploads:/data \
     -v /root/testkauf-backups/uploads:/backup:ro \
     alpine:3.20 \
     sh -c 'find /data -mindepth 1 -delete && tar -xzf /backup/uploads-YYYYMMDD-HHMMSS.tar.gz -C /data'
   ```

5. Start the app again:

   ```bash
   docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env up -d app
   ```

6. Confirm `sha256sum -c` still matches the archive, and that a known file under the volume has the checksum you recorded before the wipe.

## Restore drill (synthetic data)

Ran once on this change, on a local Docker engine, not on the production server. Databases and the file below were created for the drill and then removed. No production `.env`, URL, or customer data was used.

Date: 2026-11-26, about 09:56 UTC. Docker 29.1.3, images `mysql:8.4` and `alpine:3.20`, on a local engine. Workspace `/tmp/testkauf-drill` (mode 700). The env file held only `MYSQL_ROOT_PASSWORD` for this drill and is not recorded here. The password was not printed. Each `mysql` / `mysqldump` call also printed `mysql: [Warning] Using a password on the command line interface can be insecure.` Those repeated warning lines are omitted below.

Compose file (`/tmp/testkauf-drill/compose.yml`), project name `testkaufdrill`, no published ports:

```yaml
name: testkaufdrill
services:
  db:
    image: mysql:8.4
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
    command: ["--innodb-buffer-pool-size=64M", "--performance-schema=OFF"]
    volumes:
      - drilldb:/var/lib/mysql
    healthcheck:
      test: ["CMD-SHELL", "mysqladmin ping -h 127.0.0.1 -uroot -p\"$$MYSQL_ROOT_PASSWORD\" --silent"]
      interval: 5s
      timeout: 5s
      retries: 40
      start_period: 20s
volumes:
  drilldb:
```

```text
+ docker compose -f /tmp/testkauf-drill/compose.yml --env-file /tmp/testkauf-drill/drill.env up -d --wait
 Container testkaufdrill-db-1  Started
 Container testkaufdrill-db-1  Healthy

+ mysql (inside db) created test_purchase_db.drill_rows and pricedb.drill_rows
test_purchase_db count=3
1:alpha
2:beta
3:gamma
pricedb count=2
1:price-a
2:price-b

+ docker volume create testkauf_uploads
testkauf_uploads
+ docker run --rm -v testkauf_uploads:/data alpine:3.20 \
    sh -c 'mkdir -p /data/avatars && printf "%s\n" "phase1-demo-upload" > /data/avatars/demo-phase1.txt'
+ docker run -d --name drill-app-sim -v testkauf_uploads:/data alpine:3.20 sleep 3600
file sha256 before backup:
8435e482a4a0b0bb8878e1ff53df63c36424cb49f934ba25deaeb2d2b717f2ef  /data/avatars/demo-phase1.txt
host content sha256:
8435e482a4a0b0bb8878e1ff53df63c36424cb49f934ba25deaeb2d2b717f2ef  -

+ plant expired archive (touch -d "20 days ago")
2026-11-06 uploads-20000101-000000.tar.gz
2026-11-06 uploads-20000101-000000.tar.gz.sha256

+ docker ps (before uploads backup)
NAMES                STATUS                   IMAGE
drill-app-sim        Up Less than a second    alpine:3.20
testkaufdrill-db-1   Up 6 seconds (healthy)   mysql:8.4

+ COMPOSE_FILE=.../compose.yml ENV_FILE=.../drill.env BACKUP_DIR=/tmp/testkauf-drill/mysql \
    deploy/backup-mysql.sh
2026-11-26T09:56:56+00:00 backup ok: /tmp/testkauf-drill/mysql/test_purchase_db-20261126-095656.sql.gz (4.0K)
2026-11-26T09:56:56+00:00 backup ok: /tmp/testkauf-drill/mysql/pricedb-20261126-095656.sql.gz (4.0K)
mysql backup exit=0
-rw------- 755 test_purchase_db-20261126-095656.sql.gz
-rw------- 737 pricedb-20261126-095656.sql.gz

+ BACKUP_DIR=/tmp/testkauf-drill/uploads UPLOADS_VOLUME=testkauf_uploads \
    LOG_FILE=/tmp/testkauf-drill/testkauf-backup.log \
    STATUS_FILE=/tmp/testkauf-drill/LAST_STATUS \
    deploy/backup-uploads.sh
uploads backup exit=0
hook hits (BACKUP_HEALTH_URL unset): (none)
LAST_STATUS:
2026-11-26T09:56:57Z OK uploads /tmp/testkauf-drill/uploads/uploads-20261126-095656.tar.gz sha256=9ae75b7ddccf8f2d8a9ba18ff2521fd5dfe759cba7681429a7594bded72605c9 size_bytes=171 hook=off
log:
2026-11-26T09:56:57Z INFO uploads removed expired /tmp/testkauf-drill/uploads/uploads-20000101-000000.tar.gz
2026-11-26T09:56:57Z OK uploads /tmp/testkauf-drill/uploads/uploads-20261126-095656.tar.gz sha256=9ae75b7ddccf8f2d8a9ba18ff2521fd5dfe759cba7681429a7594bded72605c9 size_bytes=171 hook=off
-rw------- 171 uploads-20261126-095656.tar.gz
-rw------- 97 uploads-20261126-095656.tar.gz.sha256
expired archive still present? no

+ docker ps (after uploads backup)
NAMES                STATUS                   IMAGE
drill-app-sim        Up Less than a second    alpine:3.20
testkaufdrill-db-1   Up 7 seconds (healthy)   mysql:8.4
live file still present:
8435e482a4a0b0bb8878e1ff53df63c36424cb49f934ba25deaeb2d2b717f2ef  /data/avatars/demo-phase1.txt

+ sha256sum --strict -c uploads-20261126-095656.tar.gz.sha256 && gzip -t && tar -tzf
uploads-20261126-095656.tar.gz: OK
gzip -t exit=0
./
./avatars/
./avatars/demo-phase1.txt
gzip -t both sql dumps exit=0

+ wipe: DELETE rows, then DROP TABLE, then delete the file in the volume
count after delete main=0 pricedb=0
ERROR 1146 (42S02) at line 1: Table 'test_purchase_db.drill_rows' doesn't exist
select after drop exit=1
volume file listing after delete: (empty)
+ docker stop drill-app-sim
drill-app-sim

+ gunzip -c test_purchase_db-20261126-095656.sql.gz | docker compose exec -T db \
    sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" test_purchase_db'
+ same for pricedb-20261126-095656.sql.gz
restored test_purchase_db count=3
1:alpha
2:beta
3:gamma
restored pricedb count=2
1:price-a
2:price-b

+ docker run --rm -v testkauf_uploads:/data -v /tmp/testkauf-drill/uploads:/backup:ro alpine:3.20 \
    sh -c 'find /data -mindepth 1 -delete && tar -xzf /backup/uploads-20261126-095656.tar.gz -C /data'
file sha256 after restore:
8435e482a4a0b0bb8878e1ff53df63c36424cb49f934ba25deaeb2d2b717f2ef  /data/avatars/demo-phase1.txt
PASS file sha256 matches before backup and host content (8435e482a4a0b0bb8878e1ff53df63c36424cb49f934ba25deaeb2d2b717f2ef)
PASS row counts test_purchase_db=3 pricedb=2
```

The db container stayed `Up (healthy)` across the uploads backup (6 seconds, then 7 seconds). The stand-in container that had the volume mounted was not stopped for the backup, and the live file checksum did not change. It was stopped only before the restore, which replaces volume contents.

Failure and hook checks, after the restore (same script, synthetic volume):

```text
+ UPLOADS_VOLUME=testkauf_uploads_missing BACKUP_HEALTH_URL=http://127.0.0.1:8765/ping
missing-volume exit=1
LAST_STATUS:
2026-11-26T09:57:09Z FAILED uploads volume testkauf_uploads_missing does not exist
hook hits:
/ping/fail
docker volume inspect testkauf_uploads_missing: volume was not created

+ BACKUP_HEALTH_URL=http://127.0.0.1:8765/ping (volume exists)
hook-success exit=0
LAST_STATUS:
2026-11-26T09:57:10Z OK uploads /tmp/testkauf-drill/uploads/uploads-20261126-095710.tar.gz sha256=1a791f707b6e6fe19c59a24239f0c056e56f7900ea9c28398b5d69965df95282 size_bytes=174 hook=ok
hook hits:
/ping

+ BACKUP_HEALTH_URL=http://127.0.0.1:9/ping (nothing listening)
hook-down exit=0
LAST_STATUS:
2026-11-26T09:57:11Z OK uploads /tmp/testkauf-drill/uploads/uploads-20261126-095711.tar.gz sha256=1a791f707b6e6fe19c59a24239f0c056e56f7900ea9c28398b5d69965df95282 size_bytes=174 hook=failed
```

The two later archives are extra hook runs after the restore. The restore itself used `uploads-20261126-095656.tar.gz` and `sha256sum -c` on that file. With `BACKUP_HEALTH_URL` unset, the listener received no request.

Drill containers, the compose volume, and `testkauf_uploads` were removed after these checks. Nothing was installed on a server.

## Install on the server (operator, not this change)

Do not run `deploy/server-setup.sh` for this, and do not restart the app. The 03:15 MySQL cron stays as it is.

```bash
install -d -m 700 /root/testkauf-backups /root/testkauf-backups/uploads
install -m 700 \
  /opt/testkauf/test-purchase-application/deploy/backup-uploads.sh \
  /usr/local/bin/testkauf-backup-uploads.sh

docker volume inspect testkauf_uploads

BACKUP_DIR=/root/testkauf-backups/uploads \
UPLOADS_VOLUME=testkauf_uploads \
  /usr/local/bin/testkauf-backup-uploads.sh
echo "exit=$?"
cat /root/testkauf-backups/LAST_STATUS
cd /root/testkauf-backups/uploads && sha256sum --strict -c uploads-*.tar.gz.sha256
```

If the checkout is not under `/opt/testkauf/test-purchase-application`, use the path that already holds `deploy/backup-mysql.sh`.

Add the 03:25 UTC line from the cron section to root's crontab. Optional, after review:

```bash
install -m 644 \
  /opt/testkauf/test-purchase-application/deploy/logrotate/testkauf-backup.example \
  /etc/logrotate.d/testkauf-backup
```

Leave `BACKUP_HEALTH_URL` unset until you have a hook you intend to call. Do not put that URL in git.

## Phase 2

An encrypted off-site copy of the database dumps and the uploads archive is a future step. This phase does not install restic, borg, rclone, or any sync off the server. The longer placeholder notes in [BACKUP.md](BACKUP.md) are not activated by installing `deploy/backup-uploads.sh`.
