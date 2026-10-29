#!/usr/bin/env bash
# Idempotent server bootstrap for /opt/testkauf on a host whose nginx already serves other sites.
# Encodes the fixes from the first deploy:
#  - clone with a normal umask (MySQL runs as uid 999 and must read deploy/mysql-init; a 077 umask broke init)
#  - secrets generated here, written only to a root-only deploy/.env (never printed)
#  - nginx: ADD only our site file; strip any IPv6 listeners certbot adds (see nginx template)
#  - daily MySQL backup via cron
# Usage (as root):  bash server-setup.sh [--certbot]
set -euo pipefail
BASE=/opt/testkauf; DOMAIN=testkauf.getinweb.de
umask 022
mkdir -p "$BASE"; cd "$BASE"
for r in test-purchase-application price-calculation-microservice; do
  [ -d "$r/.git" ] || git clone -q "https://github.com/mitetodb/$r.git"
  git -C "$r" pull -q --ff-only
done
APP=$BASE/test-purchase-application; ENVF=$APP/deploy/.env
if [ ! -f "$ENVF" ]; then
  ( umask 077; cp "$APP/.env.example" "$ENVF" )
  g(){ openssl rand -hex 24; }
  for v in MYSQL_ROOT_PASSWORD DB_PASSWORD PRICING_DB_PASSWORD PRICING_API_KEY; do
    sed -i "s|^$v=.*|$v=$(g)|" "$ENVF"
  done
  sed -i -e 's|^ADMIN_PASSWORD=.*|ADMIN_PASSWORD=|' -e 's|^MAIL_PASSWORD=.*|MAIL_PASSWORD=|' -e 's|^MAIL_ENABLED=.*|MAIL_ENABLED=false|' \
         -e "s|^PRICING_BUILD_CONTEXT=.*|PRICING_BUILD_CONTEXT=$BASE/price-calculation-microservice|" "$ENVF"
fi
chown root:root "$ENVF"; chmod 600 "$ENVF"
chmod 755 "$APP/deploy/mysql-init"; chmod 755 "$APP/deploy/mysql-init/"*.sh

docker compose -f "$APP/deploy/docker-compose.prod.yml" --env-file "$ENVF" up -d --build

SITE=/etc/nginx/sites-available/$DOMAIN
if [ ! -f "$SITE" ]; then
  cat > "$SITE" <<NGX
server {
    listen 80;
    server_name $DOMAIN;
    client_max_body_size 12m;
    location ~ ^/actuator/(?!health\$) { return 404; }
    location / {
        proxy_pass http://127.0.0.1:8090;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }
}
NGX
  ln -sf "$SITE" /etc/nginx/sites-enabled/$DOMAIN
fi
if [ "${1:-}" = "--certbot" ]; then
  certbot --nginx -d "$DOMAIN" --non-interactive --agree-tos --register-unsafely-without-email --redirect
fi
sed -i '/listen \[::\]/d' "$SITE"     # shared host: never add IPv6 default listeners (see template)
nginx -t && systemctl reload nginx

install -m 700 "$APP/deploy/backup-mysql.sh" /usr/local/bin/testkauf-backup-mysql.sh
CRON="15 3 * * * BACKUP_DIR=/root/testkauf-backups/mysql COMPOSE_FILE=$APP/deploy/docker-compose.prod.yml ENV_FILE=$ENVF /usr/local/bin/testkauf-backup-mysql.sh >> /var/log/testkauf-backup.log 2>&1"
( crontab -l 2>/dev/null | grep -v testkauf-backup; echo "$CRON" ) | crontab -
echo "setup done"
