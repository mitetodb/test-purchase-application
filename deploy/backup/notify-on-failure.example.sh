#!/usr/bin/env bash
# Wrap a backup command and notify on failure. Reference only — docs/BACKUP.md.
# Usage: notify-on-failure.example.sh /path/to/backup-all.example.sh
set -euo pipefail
LOG="${BACKUP_LOG:-/var/log/testkauf-backup.log}"
HC_URL="${HEALTHCHECKS_URL:-}"          # e.g. https://hc.example.com/ping/your-uuid
ALERT_EMAIL="${BACKUP_ALERT_EMAIL:-}"   # e.g. ops@example.com

if [[ $# -lt 1 ]]; then
  echo "usage: $0 command..." >&2
  exit 2
fi

if "$@" >>"$LOG" 2>&1; then
  [[ -n "$HC_URL" ]] && curl -fsS -m 10 --retry 3 "$HC_URL" >/dev/null || true
  exit 0
fi

{
  echo "===== BACKUP FAILED $(date -Is) ====="
  echo "command: $*"
  tail -n 80 "$LOG" || true
} >>"$LOG"

if [[ -n "$ALERT_EMAIL" ]] && command -v mail >/dev/null; then
  tail -n 80 "$LOG" | mail -s "testkauf backup FAILED on $(hostname -f 2>/dev/null || hostname)" "$ALERT_EMAIL" || true
fi

exit 1
