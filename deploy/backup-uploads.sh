#!/usr/bin/env bash
# Nightly tar.gz of the uploads Docker volume (phase 1, issue #10).
# The app keeps running: the volume is mounted read-only in a one-off container.
#
# Defaults match the live MySQL job (deploy/backup-mysql.sh stays MySQL-only):
#   volume testkauf_uploads
#   directory /root/testkauf-backups/uploads
#   retention 14 days (find -mtime +N, same rule as the MySQL script)
#   log /var/log/testkauf-backup.log
#
# This file is not installed by deploy/server-setup.sh. Cron example: docs/backup-restore.md.
#
# Env (all optional):
#   BACKUP_DIR          archive directory (default /root/testkauf-backups/uploads)
#   UPLOADS_VOLUME      named volume (default testkauf_uploads)
#   RETENTION_DAYS      default 14
#   LOG_FILE            default /var/log/testkauf-backup.log
#   STATUS_FILE         one-line OK/FAILED status (default /root/testkauf-backups/LAST_STATUS)
#   BACKUP_HEALTH_URL   optional health-check URL; empty = no HTTP call
#   ALPINE_IMAGE        image used only to read the volume (default alpine:3.20)
set -euo pipefail

BACKUP_DIR="${BACKUP_DIR:-/root/testkauf-backups/uploads}"
UPLOADS_VOLUME="${UPLOADS_VOLUME:-testkauf_uploads}"
RETENTION_DAYS="${RETENTION_DAYS:-14}"
LOG_FILE="${LOG_FILE:-/var/log/testkauf-backup.log}"
STATUS_FILE="${STATUS_FILE:-/root/testkauf-backups/LAST_STATUS}"
BACKUP_HEALTH_URL="${BACKUP_HEALTH_URL:-}"
ALPINE_IMAGE="${ALPINE_IMAGE:-alpine:3.20}"
TMP_ARCHIVE=""

utc_now() {
  date -u +"%Y-%m-%dT%H:%M:%SZ"
}

# One line a monitor can grep: "<UTC> OK ..." or "<UTC> FAILED ...".
# fd 3, when open, is the original terminal so a manual run still prints the line.
# Cron (no tty) stays silent and does not need a mailer.
emit_status() {
  local msg="$1"
  mkdir -p -- "$(dirname "$LOG_FILE")"
  printf '%s\n' "$msg" >>"$LOG_FILE"
  if [[ -e /dev/fd/3 ]]; then
    printf '%s\n' "$msg" >&3
  fi
}

write_status_file() {
  local msg="$1"
  local status_dir
  status_dir="$(dirname "$STATUS_FILE")"
  mkdir -p -- "$status_dir"
  chmod 700 "$status_dir"
  (
    umask 077
    printf '%s\n' "$msg" >"$STATUS_FILE"
  )
  chmod 600 "$STATUS_FILE"
}

finish_failure() {
  local rc="$1"
  local reason="$2"
  local stamp msg
  trap - ERR
  set +e
  stamp="$(utc_now)"
  msg="${stamp} FAILED uploads ${reason}"
  if [[ -n "$TMP_ARCHIVE" && -f "$TMP_ARCHIVE" ]]; then
    rm -f -- "$TMP_ARCHIVE"
  fi
  emit_status "$msg"
  write_status_file "$msg"
  if [[ -n "$BACKUP_HEALTH_URL" ]] && command -v curl >/dev/null 2>&1; then
    curl -fsS -m 10 --retry 3 --retry-delay 2 "${BACKUP_HEALTH_URL%/}/fail" >/dev/null 2>&1 || true
  fi
  exit "$rc"
}

die() {
  finish_failure 1 "$1"
}

on_err() {
  local rc=$?
  if [[ "$rc" -eq 0 ]]; then
    rc=1
  fi
  finish_failure "$rc" "backup failed (exit ${rc})"
}
trap on_err ERR

if [[ ! "$RETENTION_DAYS" =~ ^[0-9]+$ ]]; then
  die "RETENTION_DAYS must be a non-negative integer"
fi
if [[ ! "$UPLOADS_VOLUME" =~ ^[A-Za-z0-9][A-Za-z0-9_.-]*$ ]]; then
  die "UPLOADS_VOLUME has unexpected characters"
fi
if ! command -v docker >/dev/null 2>&1; then
  die "docker not found on PATH"
fi
if ! command -v flock >/dev/null 2>&1; then
  die "flock not found on PATH"
fi
if ! command -v sha256sum >/dev/null 2>&1; then
  die "sha256sum not found on PATH"
fi

mkdir -p -- "$BACKUP_DIR"
chmod 700 "$BACKUP_DIR"
mkdir -p -- "$(dirname "$LOG_FILE")"
touch "$LOG_FILE"

# Manual runs show the final OK/FAILED line. Cron has no tty, so fd 3 is discarded.
if [[ -t 1 ]]; then
  exec 3>&1
else
  exec 3>/dev/null
fi
exec >>"$LOG_FILE" 2>&1

LOCK_FILE="${BACKUP_DIR}/.lock"
exec 9>"$LOCK_FILE"
if ! flock -n 9; then
  die "another uploads backup is already running"
fi
chmod 600 "$LOCK_FILE" || true

if ! docker volume inspect "$UPLOADS_VOLUME" >/dev/null 2>&1; then
  die "volume ${UPLOADS_VOLUME} does not exist"
fi

ts="$(date -u +%Y%m%d-%H%M%S)"
archive="${BACKUP_DIR}/uploads-${ts}.tar.gz"
TMP_ARCHIVE="${BACKUP_DIR}/uploads-${ts}.tar.gz.tmp"
checksum="${archive}.sha256"

if [[ -e "$archive" || -e "$TMP_ARCHIVE" || -e "$checksum" ]]; then
  die "refusing to overwrite existing archive for ${ts}"
fi

# Read-only volume mount: no app stop, no write into the live uploads.
docker run --rm \
  -v "${UPLOADS_VOLUME}:/data:ro" \
  -v "${BACKUP_DIR}:/backup" \
  "$ALPINE_IMAGE" \
  tar -czf "/backup/$(basename "$TMP_ARCHIVE")" -C /data .

mv -- "$TMP_ARCHIVE" "$archive"
TMP_ARCHIVE=""
chmod 600 "$archive"
gzip -t "$archive"
tar -tzf "$archive" >/dev/null

(
  cd -- "$BACKUP_DIR"
  sha256sum "$(basename "$archive")" > "$(basename "$checksum")"
)
chmod 600 "$checksum"
(
  cd -- "$BACKUP_DIR"
  sha256sum --quiet --strict -c "$(basename "$checksum")"
)

remove_expired() {
  local pattern="$1"
  local old base
  while IFS= read -r -d '' old; do
    rm -f -- "$old"
    base="$(basename "$old")"
    if [[ "$base" == *.tar.gz ]]; then
      rm -f -- "${old}.sha256"
    fi
    printf '%s\n' "$(utc_now) INFO uploads removed expired ${old}"
  done < <(find "$BACKUP_DIR" -name "$pattern" -type f -mtime +"$RETENTION_DAYS" -print0)
}

remove_expired 'uploads-*.tar.gz'
remove_expired 'uploads-*.tar.gz.sha256'

while IFS= read -r -d '' stale; do
  rm -f -- "$stale"
  printf '%s\n' "$(utc_now) INFO uploads removed stale partial ${stale}"
done < <(find "$BACKUP_DIR" -name 'uploads-*.tar.gz.tmp' -type f -mtime +1 -print0)

hook_state="off"
if [[ -n "$BACKUP_HEALTH_URL" ]]; then
  if ! command -v curl >/dev/null 2>&1; then
    hook_state="skipped"
  elif curl -fsS -m 10 --retry 3 --retry-delay 2 "$BACKUP_HEALTH_URL" >/dev/null 2>&1; then
    hook_state="ok"
  else
    hook_state="failed"
  fi
fi

digest="$(awk 'NR==1 { print $1 }' "$checksum")"
size_bytes="$(stat -c '%s' "$archive")"
stamp="$(utc_now)"
ok_line="${stamp} OK uploads ${archive} sha256=${digest} size_bytes=${size_bytes} hook=${hook_state}"
emit_status "$ok_line"
write_status_file "$ok_line"
