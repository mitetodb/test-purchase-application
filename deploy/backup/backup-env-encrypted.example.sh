#!/usr/bin/env bash
# Encrypt deploy/.env for off-site storage (passphrase entered interactively or via GPG agent).
# Reference only — never commit ciphertext with weak passphrases in tickets.
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$HERE/../.." && pwd)"
ENV_FILE="${ENV_FILE:-$REPO_ROOT/deploy/.env}"
OUT_DIR="${BACKUP_ROOT:-/root/testkauf-backups}/env"
OUT_FILE="$OUT_DIR/deploy.env.gpg"

[[ -f "$ENV_FILE" ]] || { echo "missing $ENV_FILE" >&2; exit 1; }
mkdir -p "$OUT_DIR"
chmod 700 "$OUT_DIR"

gpg --symmetric --cipher-algo AES256 --output "$OUT_FILE" "$ENV_FILE"
chmod 600 "$OUT_FILE"
echo "$(date -Is) encrypted env written: $OUT_FILE"
