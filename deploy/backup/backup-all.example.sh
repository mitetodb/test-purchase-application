#!/usr/bin/env bash
# Reference orchestrator — NOT installed by server-setup.sh. See docs/BACKUP.md.
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$HERE/../.." && pwd)"
COMPOSE_FILE="${COMPOSE_FILE:-$REPO_ROOT/deploy/docker-compose.prod.yml}"
ENV_FILE="${ENV_FILE:-$REPO_ROOT/deploy/.env}"
BACKUP_ROOT="${BACKUP_ROOT:-/root/testkauf-backups}"
MYSQL_DIR="$BACKUP_ROOT/mysql"
UPLOADS_DIR="$BACKUP_ROOT/uploads"
LOG_TAG="testkauf-backup"

mkdir -p "$MYSQL_DIR" "$UPLOADS_DIR"
chmod 700 "$BACKUP_ROOT" "$MYSQL_DIR" "$UPLOADS_DIR"

export COMPOSE_FILE ENV_FILE BACKUP_DIR="$MYSQL_DIR"
"$REPO_ROOT/deploy/backup-mysql.sh"

export BACKUP_DIR="$UPLOADS_DIR"
"$HERE/backup-uploads.example.sh"

for f in "$MYSQL_DIR"/*.sql.gz; do
  [[ -f "$f" ]] || continue
  gzip -t "$f"
done
for f in "$UPLOADS_DIR"/uploads-*.tar.gz; do
  [[ -f "$f" ]] || continue
  tar -tzf "$f" >/dev/null
done

if [[ -x "$HERE/rotate-local.example.sh" ]]; then
  BACKUP_ROOT="$BACKUP_ROOT" "$HERE/rotate-local.example.sh"
fi
if [[ -n "${RESTIC_REPOSITORY:-}" && -x "$HERE/offsite-restic.example.sh" ]]; then
  "$HERE/offsite-restic.example.sh"
fi

echo "$(date -Is) $LOG_TAG: backup-all completed OK"
