#!/usr/bin/env bash
# Verify security headers on a running instance (app directly or via nginx).
# Usage:
#   BASE_URL=http://127.0.0.1:8080 scripts/check-security-headers.sh
#   BASE_URL=https://testkauf.getinweb.de scripts/check-security-headers.sh
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
PATH_TO_CHECK="${1:-/login}"

headers="$(curl -fsSI "${BASE_URL}${PATH_TO_CHECK}")"

count_header() {
  local name="$1"
  echo "$headers" | grep -ci "^${name}:" || true
}

expect_once() {
  local name="$1"
  local n
  n="$(count_header "$name")"
  if [[ "$n" -ne 1 ]]; then
    echo "!! expected exactly one ${name} header, got ${n}"
    echo "$headers" | grep -i "^${name}:" || true
    exit 1
  fi
}

expect_absent() {
  local name="$1"
  local n
  n="$(count_header "$name")"
  if [[ "$n" -ne 0 ]]; then
    echo "!! expected no ${name} header (use CSP frame-ancestors), got ${n}"
    echo "$headers" | grep -i "^${name}:" || true
    exit 1
  fi
}

expect_once "Content-Security-Policy"
expect_once "X-Content-Type-Options"
expect_once "Referrer-Policy"
expect_once "Permissions-Policy"
expect_absent "X-Frame-Options"

if [[ "$BASE_URL" == https://* ]]; then
  expect_once "Strict-Transport-Security"
fi

if echo "$headers" | grep -i "^Content-Security-Policy:" | grep -qi "unsafe-eval"; then
  echo "!! CSP must not include unsafe-eval"
  exit 1
fi

echo "Security headers OK for ${BASE_URL}${PATH_TO_CHECK}"
