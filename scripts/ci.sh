#!/usr/bin/env bash
# Local/CI pipeline: secret scan -> mvn verify -> compose config check -> optional docker build.
# Usage: scripts/ci.sh            (DOCKER_BUILD=1 to also build the image)
set -euo pipefail
cd "$(dirname "$0")/.."

echo "==> 1/4 secret scan (tracked files)"
pattern='(spring\.(datasource|mail)\.password=[^$[:space:]]|MYSQL_ROOT_PASSWORD:[[:space:]]+[^$[:space:]]|-----BEGIN [A-Z ]*PRIVATE KEY-----|AKIA[0-9A-Z]{16}|setPassword\("[^"]+"\))'
if git ls-files -z | grep -zv -e '^\.env\.example$' -e '^scripts/ci\.sh$' -e '^src/test/' | xargs -0 grep -nIE "$pattern" 2>/dev/null; then
  echo "!! possible hardcoded secret found (see above)"; exit 1
fi
if git ls-files | grep -E '(^|/)\.env$'; then echo "!! .env is tracked"; exit 1; fi
echo "    clean"

echo "==> 2/4 mvn verify"
mvn -B -q verify

echo "==> 3/4 compose config"
if command -v docker >/dev/null && docker compose version >/dev/null 2>&1; then
  docker compose -f deploy/docker-compose.prod.yml --env-file .env.example config -q
  if docker compose -f deploy/docker-compose.prod.yml --env-file .env.example config | grep -E 'published: "?(3306|8081|8025|1025)'; then
    echo "!! internal port published"; exit 1
  fi
  echo "    ok"
else
  echo "    docker compose not available - skipped"
fi

echo "==> 4/4 docker build"
if [[ "${DOCKER_BUILD:-0}" == "1" ]]; then
  docker build -t test-purchase-app:ci .
else
  echo "    skipped (set DOCKER_BUILD=1)"
fi
echo "CI OK"
