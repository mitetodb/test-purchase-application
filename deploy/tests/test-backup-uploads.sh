#!/usr/bin/env bash
# Demo-data harness for deploy/backup-uploads.sh. Needs a local Docker engine.
# Does not install cron and does not use production paths.
#
# Usage (from the repo root, or with any cwd):
#   deploy/tests/test-backup-uploads.sh
#
# Covers: a successful backup, two runs forced into the same UTC second
# (distinct suffixes, first archive and checksum kept), a same-name refusal
# that must not delete the first archive, a forced gzip -t failure, and
# chmod 640 on an existing log file.
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
REPO="$(cd "$HERE/../.." && pwd)"
SCRIPT="$REPO/deploy/backup-uploads.sh"
ROOT="$(mktemp -d /tmp/testkauf-backup-harness.XXXXXX)"
BACKUP="$ROOT/uploads"
LOG="$ROOT/testkauf-backup.log"
STATUS="$ROOT/LAST_STATUS"
DATEBIN="$ROOT/fakedate"
GZIPBIN="$ROOT/fakegzip"
VOL="testkaufharness$$"

cleanup() {
  docker volume rm -f "$VOL" >/dev/null 2>&1 || true
  rm -rf "$ROOT"
}
trap cleanup EXIT

fail() {
  echo "FAIL $*" >&2
  exit 1
}

run_backup() {
  BACKUP_DIR="$BACKUP" \
  UPLOADS_VOLUME="$VOL" \
  LOG_FILE="$LOG" \
  STATUS_FILE="$STATUS" \
    "$SCRIPT"
}

checksum_ok() {
  local archive="$1"
  (
    cd -- "$BACKUP"
    sha256sum --strict -c "$(basename "$archive").sha256"
  )
}

archive_file_sha() {
  sha256sum "$1" | awk 'NR==1 { print $1 }'
}

echo "harness root=$ROOT volume=$VOL"
install -d -m 700 "$BACKUP" "$DATEBIN" "$GZIPBIN"
: >"$LOG"
chmod 600 "$LOG"

docker volume create "$VOL" >/dev/null
docker run --rm -v "$VOL":/data alpine:3.20 \
  sh -c 'mkdir -p /data/avatars && printf "%s\n" "harness-demo-upload" > /data/avatars/demo.txt'

cat >"$DATEBIN/date" << 'EOF'
#!/bin/sh
if [ "$1" = "-u" ] && [ "$2" = "+%Y%m%d-%H%M%S" ]; then
  printf '%s\n' '20000101-000000'
  exit 0
fi
exec /usr/bin/date "$@"
EOF
chmod 755 "$DATEBIN/date"

cat >"$GZIPBIN/gzip" << 'EOF'
#!/bin/sh
echo "gzip: forced failure for harness" >&2
exit 1
EOF
chmod 755 "$GZIPBIN/gzip"

echo "=== success ==="
run_backup
[[ "$(stat -c %a "$LOG")" == "640" ]] || fail "log mode is not 640"
mapfile -t archives < <(find "$BACKUP" -maxdepth 1 -name 'uploads-*.tar.gz' -type f | sort)
[[ "${#archives[@]}" -eq 1 ]] || fail "expected 1 archive, got ${#archives[@]}"
checksum_ok "${archives[0]}"
partials="$(find "$BACKUP" -maxdepth 1 -name '*.partial' -type f | wc -l)"
[[ "$partials" -eq 0 ]] || fail "partials left after success"
echo "PASS success ${archives[0]} log_mode=640"

echo "=== same UTC second, distinct names ==="
PATH="$DATEBIN:$PATH" BACKUP_NAME_SUFFIX=aaa run_backup
PATH="$DATEBIN:$PATH" BACKUP_NAME_SUFFIX=bbb run_backup
first_same="$BACKUP/uploads-20000101-000000-aaa.tar.gz"
second_same="$BACKUP/uploads-20000101-000000-bbb.tar.gz"
[[ -f "$first_same" && -f "$second_same" ]] || fail "distinct same-second archives missing"
[[ "$first_same" != "$second_same" ]] || fail "same-second names were not distinct"
checksum_ok "$first_same"
checksum_ok "$second_same"
echo "PASS same-second distinct names; first checksum OK"

echo "=== same UTC second, same suffix (must not delete the first) ==="
PATH="$DATEBIN:$PATH" BACKUP_NAME_SUFFIX=same run_backup
same_archive="$BACKUP/uploads-20000101-000000-same.tar.gz"
[[ -f "$same_archive" ]] || fail "same-suffix archive missing"
before="$(archive_file_sha "$same_archive")"
set +e
PATH="$DATEBIN:$PATH" BACKUP_NAME_SUFFIX=same run_backup
refuse_rc=$?
set -e
[[ "$refuse_rc" -ne 0 ]] || fail "second same-name run exited 0"
[[ -f "$same_archive" ]] || fail "refusal deleted the existing archive"
after="$(archive_file_sha "$same_archive")"
[[ "$before" == "$after" ]] || fail "existing archive bytes changed ($before vs $after)"
checksum_ok "$same_archive"
partials="$(find "$BACKUP" -maxdepth 1 -name '*.partial' -type f | wc -l)"
[[ "$partials" -eq 0 ]] || fail "partials left after refusal"
echo "PASS refusal exit=$refuse_rc archive unchanged sha256=$after"

echo "=== forced gzip -t failure ==="
before_count="$(find "$BACKUP" -maxdepth 1 -name 'uploads-*.tar.gz' -type f | wc -l)"
set +e
PATH="$GZIPBIN:$PATH" run_backup
fail_rc=$?
set -e
[[ "$fail_rc" -ne 0 ]] || fail "forced gzip failure exited 0"
after_count="$(find "$BACKUP" -maxdepth 1 -name 'uploads-*.tar.gz' -type f | wc -l)"
[[ "$before_count" == "$after_count" ]] || fail "gzip failure changed archive count ($before_count -> $after_count)"
partials="$(find "$BACKUP" -maxdepth 1 -name '*.partial' -type f | wc -l)"
[[ "$partials" -eq 0 ]] || fail "partials left after gzip failure"
while IFS= read -r -d '' archive; do
  checksum_ok "$archive"
done < <(find "$BACKUP" -maxdepth 1 -name 'uploads-*.tar.gz' -type f -print0)
grep -q ' FAILED ' "$STATUS" || fail "LAST_STATUS is not FAILED"
echo "PASS forced failure exit=$fail_rc partials=0 earlier archives still verify"

echo "HARNESS_OK"
