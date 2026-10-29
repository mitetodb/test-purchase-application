#!/usr/bin/env bash
# MySQL backup with rotation for the testkauf compose stack.
# Cron example (daily 03:15):  15 3 * * * /opt/testkauf/deploy/backup-mysql.sh >> /var/log/testkauf-backup.log 2>&1
# Env: BACKUP_DIR (default deploy/backups), RETENTION_DAYS (14), COMPOSE_FILE, ENV_FILE.
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
COMPOSE_FILE="${COMPOSE_FILE:-$HERE/docker-compose.prod.yml}"
ENV_FILE="${ENV_FILE:-$HERE/.env}"
BACKUP_DIR="${BACKUP_DIR:-$HERE/backups}"
RETENTION_DAYS="${RETENTION_DAYS:-14}"
DATABASES="${DATABASES:-test_purchase_db pricedb}"

mkdir -p "$BACKUP_DIR"; chmod 700 "$BACKUP_DIR"
ts="$(date +%Y%m%d-%H%M%S)"
dc() { docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" "$@"; }

for db in $DATABASES; do
  out="$BACKUP_DIR/${db}-${ts}.sql.gz"
  # password is read inside the container from its own env; never passed on the host CLI
  dc exec -T db sh -c 'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction --routines --triggers --no-tablespaces "$0"' "$db" \
    | gzip -9 > "$out.tmp"
  mv "$out.tmp" "$out"; chmod 600 "$out"
  gzip -t "$out"
  echo "$(date -Is) backup ok: $out ($(du -h "$out" | cut -f1))"
done

find "$BACKUP_DIR" -name '*.sql.gz' -type f -mtime +"$RETENTION_DAYS" -print -delete
# Restore: gunzip -c FILE | docker compose -f deploy/docker-compose.prod.yml exec -T db sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" DBNAME'
