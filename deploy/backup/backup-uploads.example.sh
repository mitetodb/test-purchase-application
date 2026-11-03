#!/usr/bin/env bash
# Archive Docker volume uploads (avatars + attachments). Reference only — docs/BACKUP.md.
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$HERE/../.." && pwd)"
COMPOSE_FILE="${COMPOSE_FILE:-$REPO_ROOT/deploy/docker-compose.prod.yml}"
ENV_FILE="${ENV_FILE:-$REPO_ROOT/deploy/.env}"
BACKUP_DIR="${BACKUP_DIR:-/root/testkauf-backups/uploads}"
UPLOADS_VOLUME="${UPLOADS_VOLUME:-testkauf_uploads}"

mkdir -p "$BACKUP_DIR"
chmod 700 "$BACKUP_DIR"
ts="$(date +%Y%m%d-%H%M%S)"
out="$BACKUP_DIR/uploads-${ts}.tar.gz"

docker run --rm \
  -v "${UPLOADS_VOLUME}:/data:ro" \
  -v "${BACKUP_DIR}:/backup" \
  alpine:3.20 \
  tar -czf "/backup/uploads-${ts}.tar.gz" -C /data .

chmod 600 "$out"
tar -tzf "$out" >/dev/null
echo "$(date -Is) uploads backup ok: $out ($(du -h "$out" | cut -f1))"
