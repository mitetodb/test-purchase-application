#!/usr/bin/env bash
# Push local backup dirs to an encrypted restic repo (e.g. Hetzner Storage Box over SFTP).
# Set RESTIC_REPOSITORY and RESTIC_PASSWORD_FILE on the host before use. Reference only.
set -euo pipefail
BACKUP_ROOT="${BACKUP_ROOT:-/root/testkauf-backups}"
TAG="${RESTIC_TAG:-testkauf,nightly}"

: "${RESTIC_REPOSITORY:?set RESTIC_REPOSITORY, e.g. sftp:backup@storagebox.example.com:23/backups/testkauf}"
: "${RESTIC_PASSWORD_FILE:?set RESTIC_PASSWORD_FILE to a mode-600 file}"

export RESTIC_PASSWORD_FILE
export RESTIC_CACHE_DIR="${RESTIC_CACHE_DIR:-/var/cache/restic-testkauf}"

restic snapshots >/dev/null 2>&1 || restic init

restic backup \
  "$BACKUP_ROOT/mysql" \
  "$BACKUP_ROOT/uploads" \
  "$BACKUP_ROOT/env" \
  --tag "$TAG"

restic forget --keep-daily 14 --keep-weekly 8 --keep-monthly 6 --prune
restic check
echo "$(date -Is) restic offsite sync OK"
