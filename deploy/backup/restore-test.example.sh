#!/usr/bin/env bash
# Throwaway restore test — uses an isolated compose project; does NOT touch production volumes.
# Usage: DEMO_SQL=/path/to/test_purchase_db.sql.gz DEMO_UPLOADS_TAR=/path/to/uploads.tar.gz ./restore-test.example.sh
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
WORKDIR="${WORKDIR:-/tmp/testkauf-restore-test-$$}"
PROJECT="testkauf_restore_test_$$"
COMPOSE="$WORKDIR/docker-compose.restore-test.yml"

cleanup() {
  docker compose -f "$COMPOSE" -p "$PROJECT" down -v --remove-orphans 2>/dev/null || true
  rm -rf "$WORKDIR"
}
trap cleanup EXIT

mkdir -p "$WORKDIR"
cat >"$COMPOSE" <<'YAML'
services:
  db:
    image: mysql:8.4
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: test_purchase_db
    healthcheck:
      test: ["CMD-SHELL", "mysqladmin ping -h 127.0.0.1 -uroot -p\"$$MYSQL_ROOT_PASSWORD\" --silent"]
      interval: 5s
      timeout: 3s
      retries: 20
    volumes:
      - restore-db:/var/lib/mysql
  uploads-helper:
    image: alpine:3.20
    volumes:
      - restore-uploads:/data
    command: ["sleep", "3600"]
volumes:
  restore-db:
  restore-uploads:
YAML

export MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:-restore-test-root-password-change-me}"
docker compose -f "$COMPOSE" -p "$PROJECT" up -d db uploads-helper
docker compose -f "$COMPOSE" -p "$PROJECT" exec -T db \
  sh -c 'until mysqladmin ping -h 127.0.0.1 -uroot -p"$MYSQL_ROOT_PASSWORD" --silent; do sleep 2; done'

if [[ -n "${DEMO_SQL:-}" && -f "$DEMO_SQL" ]]; then
  gunzip -c "$DEMO_SQL" | docker compose -f "$COMPOSE" -p "$PROJECT" exec -T db \
    sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" test_purchase_db'
  docker compose -f "$COMPOSE" -p "$PROJECT" exec -T db \
    sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -Nse "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=\"test_purchase_db\";"' \
    | grep -qv '^0$'
fi

if [[ -n "${DEMO_UPLOADS_TAR:-}" && -f "$DEMO_UPLOADS_TAR" ]]; then
  tar -tzf "$DEMO_UPLOADS_TAR" >/dev/null
  docker run --rm \
    -v "${PROJECT}_restore-uploads:/data" \
    -v "$(dirname "$DEMO_UPLOADS_TAR"):/backup:ro" \
    alpine:3.20 \
    sh -c "tar -xzf /backup/$(basename "$DEMO_UPLOADS_TAR") -C /data"
fi

echo "$(date -Is) restore-test OK (project=$PROJECT)"
docker compose -f "$COMPOSE" -p "$PROJECT" down -v
