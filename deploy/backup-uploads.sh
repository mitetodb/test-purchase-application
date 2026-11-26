#!/usr/bin/env bash
# Nightly tar.gz of the uploads Docker volume (phase 1, issue #10).
# The app keeps running: the volume is mounted read-only in a one-off container.
#
# The archive and its sha256 file are written under temp names in BACKUP_DIR.
# gzip -t, tar -tzf, and sha256sum -c must pass before those temps are renamed
# to the final uploads-YYYYMMDD-HHMMSS.tar.gz and .sha256 names. A failure
# deletes the temps, writes FAILED, and exits non-zero. No final name is left behind.
#
# Defaults match the live MySQL job (deploy/backup-mysql.sh stays MySQL-only):
#   volume testkauf_uploads
#   directory /root/testkauf-backups/uploads
#   retention 14 days (find -maxdepth 1 -mtime +N)
#   log /var/log/testkauf-backup.log (created mode 640 if missing)
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
umask 077

BACKUP_DIR="${BACKUP_DIR:-/root/testkauf-backups/uploads}"
UPLOADS_VOLUME="${UPLOADS_VOLUME:-testkauf_uploads}"
RETENTION_DAYS="${RETENTION_DAYS:-14}"
LOG_FILE="${LOG_FILE:-/var/log/testkauf-backup.log}"
STATUS_FILE="${STATUS_FILE:-/root/testkauf-backups/LAST_STATUS}"
BACKUP_HEALTH_URL="${BACKUP_HEALTH_URL:-}"
ALPINE_IMAGE="${ALPINE_IMAGE:-alpine:3.20}"
TMP_ARCHIVE=""
TMP_CHECKSUM=""
FINAL_ARCHIVE=""
FINAL_CHECKSUM=""
PUBLISHED=0

utc_now() {
  date -u +"%Y-%m-%dT%H:%M:%SZ"
}

# Create the log only when it is missing, mode 640 (rw-r-----).
# An existing file keeps whatever mode the operator or logrotate already set.
ensure_log_file() {
  local dir
  dir="$(dirname "$LOG_FILE")"
  mkdir -p -- "$dir"
  if [[ ! -e "$LOG_FILE" ]]; then
    : >"$LOG_FILE"
    chmod 640 "$LOG_FILE"
  fi
}

# One line a monitor can grep: "<UTC> OK ..." or "<UTC> FAILED ...".
# fd 3, when open, is the original terminal so a manual run still prints the line.
# Cron (no tty) stays silent. Set MAILTO="" in the crontab anyway.
emit_status() {
  local msg="$1"
  ensure_log_file
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
  printf '%s\n' "$msg" >"$STATUS_FILE"
  chmod 600 "$STATUS_FILE"
}

# Temps are always removed. Final names are removed unless publish finished.
cleanup_incomplete() {
  local f
  for f in "$TMP_ARCHIVE" "$TMP_CHECKSUM"; do
    if [[ -n "$f" && -e "$f" ]]; then
      rm -f -- "$f"
    fi
  done
  if [[ "$PUBLISHED" != 1 ]]; then
    for f in "$FINAL_ARCHIVE" "$FINAL_CHECKSUM"; do
      if [[ -n "$f" && -e "$f" ]]; then
        rm -f -- "$f"
      fi
    done
  fi
}

finish_failure() {
  local rc="$1"
  local reason="$2"
  local stamp msg
  trap - ERR
  set +e
  stamp="$(utc_now)"
  msg="${stamp} FAILED uploads ${reason}"
  cleanup_incomplete
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
ensure_log_file

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
FINAL_ARCHIVE="${BACKUP_DIR}/uploads-${ts}.tar.gz"
FINAL_CHECKSUM="${FINAL_ARCHIVE}.sha256"
TMP_ARCHIVE="${BACKUP_DIR}/uploads-${ts}.tar.gz.partial"
TMP_CHECKSUM="${BACKUP_DIR}/uploads-${ts}.tar.gz.sha256.partial"

if [[ -e "$FINAL_ARCHIVE" || -e "$FINAL_CHECKSUM" || -e "$TMP_ARCHIVE" || -e "$TMP_CHECKSUM" ]]; then
  die "refusing to overwrite existing archive for ${ts}"
fi

# Read-only volume mount: no app stop, no write into the live uploads.
# The container writes the temp name only. The final name does not exist yet.
docker run --rm \
  -v "${UPLOADS_VOLUME}:/data:ro" \
  -v "${BACKUP_DIR}:/backup" \
  "$ALPINE_IMAGE" \
  tar -czf "/backup/$(basename "$TMP_ARCHIVE")" -C /data .

chmod 600 "$TMP_ARCHIVE"
gzip -t "$TMP_ARCHIVE"
tar -tzf "$TMP_ARCHIVE" >/dev/null
(
  cd -- "$BACKUP_DIR"
  sha256sum "$(basename "$TMP_ARCHIVE")" > "$(basename "$TMP_CHECKSUM")"
  sha256sum --quiet --strict -c "$(basename "$TMP_CHECKSUM")"
)
chmod 600 "$TMP_CHECKSUM"

# Checks passed. Publish by renaming. The checksum line must name the final file.
digest="$(awk 'NR==1 { print $1 }' "$TMP_CHECKSUM")"
mv -- "$TMP_ARCHIVE" "$FINAL_ARCHIVE"
TMP_ARCHIVE=""
printf '%s  %s\n' "$digest" "$(basename "$FINAL_ARCHIVE")" >"$TMP_CHECKSUM"
chmod 600 "$TMP_CHECKSUM"
(
  cd -- "$BACKUP_DIR"
  sha256sum --quiet --strict -c "$(basename "$TMP_CHECKSUM")"
)
mv -- "$TMP_CHECKSUM" "$FINAL_CHECKSUM"
TMP_CHECKSUM=""
PUBLISHED=1

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
  done < <(find "$BACKUP_DIR" -maxdepth 1 -name "$pattern" -type f -mtime +"$RETENTION_DAYS" -print0)
}

remove_expired 'uploads-*.tar.gz'
remove_expired 'uploads-*.tar.gz.sha256'

while IFS= read -r -d '' stale; do
  rm -f -- "$stale"
  printf '%s\n' "$(utc_now) INFO uploads removed stale partial ${stale}"
done < <(find "$BACKUP_DIR" -maxdepth 1 -name 'uploads-*.partial' -type f -mtime +1 -print0)

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

size_bytes="$(stat -c '%s' "$FINAL_ARCHIVE")"
stamp="$(utc_now)"
ok_line="${stamp} OK uploads ${FINAL_ARCHIVE} sha256=${digest} size_bytes=${size_bytes} hook=${hook_state}"
emit_status "$ok_line"
write_status_file "$ok_line"
