#!/usr/bin/env bash
# Local retention for daily / weekly / monthly copies. Reference only — docs/BACKUP.md.
set -euo pipefail
BACKUP_ROOT="${BACKUP_ROOT:-/root/testkauf-backups}"
DAILY_RETENTION_DAYS="${DAILY_RETENTION_DAYS:-14}"
WEEKLY_RETENTION_DAYS="${WEEKLY_RETENTION_DAYS:-56}"
MONTHLY_RETENTION_DAYS="${MONTHLY_RETENTION_DAYS:-183}"

for sub in mysql uploads; do
  dir="$BACKUP_ROOT/$sub"
  [[ -d "$dir" ]] || continue
  find "$dir" -type f -mtime +"$DAILY_RETENTION_DAYS" -print -delete
done

weekly="$BACKUP_ROOT/weekly"
monthly="$BACKUP_ROOT/monthly"
mkdir -p "$weekly" "$monthly"
chmod 700 "$weekly" "$monthly"

# Example: promote newest mysql + uploads from today into weekly on Sundays (operator may use restic instead).
if [[ "$(date +%u)" -eq 7 ]]; then
  ts="$(date +%Y%m%d)"
  latest_mysql="$(ls -t "$BACKUP_ROOT/mysql"/*.sql.gz 2>/dev/null | head -1 || true)"
  latest_uploads="$(ls -t "$BACKUP_ROOT/uploads"/uploads-*.tar.gz 2>/dev/null | head -1 || true)"
  [[ -n "$latest_mysql" ]] && cp -a "$latest_mysql" "$weekly/mysql-${ts}.sql.gz"
  [[ -n "$latest_uploads" ]] && cp -a "$latest_uploads" "$weekly/uploads-${ts}.tar.gz"
fi

if [[ "$(date +%d)" == "01" ]]; then
  ts="$(date +%Y%m)"
  latest_mysql="$(ls -t "$BACKUP_ROOT/mysql"/*.sql.gz 2>/dev/null | head -1 || true)"
  latest_uploads="$(ls -t "$BACKUP_ROOT/uploads"/uploads-*.tar.gz 2>/dev/null | head -1 || true)"
  [[ -n "$latest_mysql" ]] && cp -a "$latest_mysql" "$monthly/mysql-${ts}.sql.gz"
  [[ -n "$latest_uploads" ]] && cp -a "$latest_uploads" "$monthly/uploads-${ts}.tar.gz"
fi

find "$weekly" -type f -mtime +"$WEEKLY_RETENTION_DAYS" -print -delete
find "$monthly" -type f -mtime +"$MONTHLY_RETENTION_DAYS" -print -delete
echo "$(date -Is) rotate-local completed"
