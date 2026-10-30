# Deployment Guide

This document provides instructions for deploying the Test Purchase Application to a production Hetzner server.

## Prerequisites

- Hetzner Cloud or dedicated server (minimum 2 GB RAM, 20 GB disk)
- Ubuntu 22.04 LTS (or similar Linux distribution)
- Root or sudo access
- Domain name with DNS A record pointing to the server
- Docker and Docker Compose installed

## Directory Structure

The application is deployed to `/opt/testkauf/` on the host:

```
/opt/testkauf/
├── test-purchase-application/       # Main app repository
│   ├── deploy/
│   │   ├── .env                     # Environment configuration (chmod 600)
│   │   ├── docker-compose.prod.yml  # Production compose file
│   │   ├── backup-mysql.sh          # Database backup script
│   │   ├── mysql-init/              # MySQL initialization scripts
│   │   └── nginx/
│   │       └── testkauf.getinweb.de.conf  # nginx vhost config
│   ├── src/                         # Application source
│   └── ...
├── price-calculation-microservice/  # Pricing service repository
│   └── ...
└── uploads/                         # User-uploaded files (Docker volume)
```

Database backups are stored in:
```
/root/testkauf-backups/
├── test_purchase_db_YYYYMMDD_HHMM.sql.gz
└── pricedb_YYYYMMDD_HHMM.sql.gz
```

## Quick Start (Automated)

The fastest way to deploy is using the automated setup script:

```bash
# As root on the server
cd /opt
git clone https://github.com/mitetodb/test-purchase-application.git testkauf/test-purchase-application
cd testkauf/test-purchase-application
git clone https://github.com/mitetodb/price-calculation-microservice.git ../price-calculation-microservice

# Run automated setup with Certbot
bash deploy/server-setup.sh --certbot

# The script will:
# - Install Docker and Docker Compose if needed
# - Create directory structure
# - Generate .env template
# - Build and start services
# - Configure nginx
# - Obtain Let's Encrypt certificate
```

After the script completes:
1. Edit `deploy/.env` with your secrets (see Configuration section)
2. Restart the stack: `cd /opt/testkauf/test-purchase-application && docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env up -d`

## Manual Deployment

### 1. Install Dependencies

```bash
# Update system
apt update && apt upgrade -y

# Install Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sh get-docker.sh

# Install Docker Compose
apt install docker-compose-plugin -y

# Verify installation
docker --version
docker compose version

# Install nginx
apt install nginx -y

# Install Certbot for Let's Encrypt
apt install certbot python3-certbot-nginx -y
```

### 2. Clone Repositories

```bash
# Create directory structure
mkdir -p /opt/testkauf
cd /opt/testkauf

# Clone main application
git clone https://github.com/mitetodb/test-purchase-application.git
cd test-purchase-application

# Clone pricing microservice (sibling directory)
cd ..
git clone https://github.com/mitetodb/price-calculation-microservice.git
```

**Important:** Ensure correct file permissions:
```bash
# Clone with normal umask (022)
umask 022
# If MySQL container fails with permission errors, check that mysql-init/ is readable
```

### 3. Configure Environment

```bash
cd /opt/testkauf/test-purchase-application

# Copy environment template
cp .env.example deploy/.env

# Secure the file
chmod 600 deploy/.env

# Edit with your secrets
nano deploy/.env
```

### 4. Configure Environment Variables

Edit `deploy/.env` and set all required values. **Never commit this file.**

#### Required Settings

```bash
# Database
DB_ROOT_PASSWORD=<generate-strong-password>
DB_PASSWORD=<generate-strong-password>
PRICING_DB_PASSWORD=<generate-strong-password>

# Application
ADMIN_PASSWORD=<initial-admin-password>
APP_BASE_URL=https://testkauf.getinweb.de
SPRING_PROFILES_ACTIVE=prod

# Pricing Service API Key (must match in both services)
PRICING_API_KEY=<generate-api-key>
```

#### Optional Settings

```bash
# Email (Optional - defaults to disabled)
MAIL_ENABLED=true
MAIL_HOST=w00d37b3.kasserver.com
MAIL_PORT=587
MAIL_USERNAME=testkauf@getinweb.de
MAIL_PASSWORD=<your-email-password>
MAIL_STARTTLS=true
MAIL_FROM=testkauf@getinweb.de

# Features
DEMO_DATA_ENABLED=false
SWAGGER_ENABLED=false

# Ports
APP_HOST_PORT=8090
```

**Generate strong secrets:**
```bash
# Generate random passwords
openssl rand -base64 32

# Generate API key
openssl rand -hex 32
```

### 5. Configure nginx

```bash
# Copy vhost configuration
cp deploy/nginx/testkauf.getinweb.de.conf /etc/nginx/sites-available/

# Enable site
ln -s /etc/nginx/sites-available/testkauf.getinweb.de.conf /etc/nginx/sites-enabled/

# Test configuration
nginx -t

# Reload nginx
systemctl reload nginx
```

**Important:** Ensure DNS A record is set before obtaining certificate.

### 6. Obtain SSL Certificate

```bash
# Request Let's Encrypt certificate
certbot --nginx -d testkauf.getinweb.de

# Follow prompts
# Certificate will be automatically renewed by certbot
```

**Note:** After Certbot modifies the nginx config, remove any IPv6 listeners if you're on a shared host:
```bash
# Edit the site config
nano /etc/nginx/sites-available/testkauf.getinweb.de.conf

# Remove these lines if present:
#   listen [::]:443 ssl ipv6only=on;
#   listen [::]:80;

# Reload nginx
systemctl reload nginx
```

The `deploy/server-setup.sh` script handles this automatically.

### 7. Build and Start Services

```bash
cd /opt/testkauf/test-purchase-application

# Build and start all services
docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env up -d --build

# Check status
docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env ps

# All services should be "healthy"
```

**Wait for health checks:**
```bash
# Watch until all services are healthy
watch docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env ps
```

Expected output:
```
NAME          STATUS
app           Up (healthy)
db            Up (healthy)
pricing-svc   Up (healthy)
```

### 8. Verify Deployment

```bash
# Check application logs
docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env logs -f app

# Test health endpoint
curl http://localhost:8090/actuator/health

# Test public HTTPS access
curl https://testkauf.getinweb.de

# Check nginx logs
tail -f /var/log/nginx/access.log
tail -f /var/log/nginx/error.log
```

### 9. Set Up Database Backups

```bash
# Test backup script
/opt/testkauf/test-purchase-application/deploy/backup-mysql.sh

# Verify backup files
ls -lh /root/testkauf-backups/

# Add cron job for nightly backups at 3:15 AM
crontab -e

# Add this line:
15 3 * * * /opt/testkauf/test-purchase-application/deploy/backup-mysql.sh >> /var/log/testkauf-backup.log 2>&1
```

**Backup script features:**
- Dumps both databases (`test_purchase_db` and `pricedb`)
- Compresses with gzip
- Rotates old backups (default 14 days retention)
- Creates files with mode 600 (secure)
- Logs to specified location

### 10. First Login

```bash
# Default admin credentials
Username: admin
Password: <ADMIN_PASSWORD from .env>
```

**Important:** Change the admin password immediately after first login!

## Configuration Reference

### Database Configuration

The stack uses a single MySQL 8.4 instance with two databases:

| Database | User | Used By |
|----------|------|---------|
| `test_purchase_db` | `appuser` | Main application |
| `pricedb` | `pricinguser` | Pricing microservice |

Users have least-privilege access (only their respective databases).

### Port Configuration

| Service | Internal Port | Host Binding |
|---------|---------------|--------------|
| app | 8080 | 127.0.0.1:8090 |
| pricing-svc | 8081 | (not exposed) |
| db | 3306 | (not exposed) |
| nginx | 80, 443 | 0.0.0.0:80, 0.0.0.0:443 |

**Security:** Only nginx ports are publicly exposed. All other services are on an internal Docker network.

### Resource Limits

All services have memory limits defined in `docker-compose.prod.yml`:

- **app**: 1GB
- **pricing-svc**: 512MB
- **db**: 1GB

Adjust based on your server capacity.

## Database Restore

To restore from a backup:

```bash
# Stop the application (database must be running)
cd /opt/testkauf/test-purchase-application
docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env stop app pricing-svc

# Decompress backup
gunzip -c /root/testkauf-backups/test_purchase_db_20261029_0315.sql.gz > /tmp/restore.sql

# Restore to MySQL
docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env exec -T db \
  mysql -u root -p"${DB_ROOT_PASSWORD}" test_purchase_db < /tmp/restore.sql

# Repeat for pricing database if needed
gunzip -c /root/testkauf-backups/pricedb_20261029_0315.sql.gz > /tmp/restore-pricing.sql
docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env exec -T db \
  mysql -u root -p"${DB_ROOT_PASSWORD}" pricedb < /tmp/restore-pricing.sql

# Clean up
rm /tmp/restore.sql /tmp/restore-pricing.sql

# Restart services
docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env start app pricing-svc
```

## Updates and Maintenance

### Updating the Application

```bash
cd /opt/testkauf/test-purchase-application

# Pull latest code
git pull origin master

# Rebuild and restart
docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env up -d --build app

# Check logs
docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env logs -f app
```

### Updating the Pricing Service

```bash
cd /opt/testkauf/price-calculation-microservice

# Pull latest code
git pull origin master

# Rebuild and restart (from main app directory)
cd /opt/testkauf/test-purchase-application
docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env up -d --build pricing-svc

# Check logs
docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env logs -f pricing-svc
```

### Database Migrations

Flyway runs automatically on application startup:

1. Migrations are in `src/main/resources/db/migration/`
2. New migrations should follow naming: `V<version>__<description>.sql`
3. Never modify existing migration files
4. Flyway validates and runs new migrations in order

**Example migration:**
```bash
# New migration file
src/main/resources/db/migration/V2__add_customer_phone.sql
```

### Viewing Logs

```bash
cd /opt/testkauf/test-purchase-application

# All services
docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env logs -f

# Specific service
docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env logs -f app
docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env logs -f db
docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env logs -f pricing-svc

# Backup logs
tail -f /var/log/testkauf-backup.log

# nginx logs
tail -f /var/log/nginx/access.log
tail -f /var/log/nginx/error.log
```

## Troubleshooting

### Service Won't Start

```bash
# Check service status
docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env ps

# Check health
docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env exec app curl http://localhost:8080/actuator/health

# Inspect service
docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env logs app

# Restart service
docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env restart app
```

### Database Connection Issues

```bash
# Verify database is running
docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env exec db mysql -u root -p -e "SHOW DATABASES;"

# Check connection from app
docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env exec app env | grep DB_

# Verify database users
docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env exec db mysql -u root -p -e "SELECT user, host FROM mysql.user;"
```

### Pricing Service Not Reachable

```bash
# Check pricing service health
docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env exec pricing-svc curl http://localhost:8081/actuator/health

# Test from app container
docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env exec app curl -H "X-API-Key: ${PRICING_API_KEY}" http://pricing-svc:8081/api/v1/prices/calculate

# Verify API key matches
docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env exec app env | grep PRICING_API_KEY
docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env exec pricing-svc env | grep PRICING_API_KEY
```

### nginx 502 Bad Gateway

```bash
# Verify app is running on localhost:8090
curl http://localhost:8090/actuator/health

# Check nginx error logs
tail -f /var/log/nginx/error.log

# Test nginx config
nginx -t

# Reload nginx
systemctl reload nginx
```

### Email Not Sending

```bash
# Check mail configuration
docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env exec app env | grep MAIL_

# Check application logs for mail errors
docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env logs app | grep -i mail

# Test SMTP connection (if using ALL-INKL)
telnet w00d37b3.kasserver.com 587
```

**Note:** If `MAIL_ENABLED=false`, emails are logged but not sent (default behavior).

### Disk Space Issues

```bash
# Check disk usage
df -h

# Check Docker disk usage
docker system df

# Clean up old images and containers
docker system prune -a

# Check backup size
du -sh /root/testkauf-backups/

# Adjust retention in backup script if needed
nano /opt/testkauf/test-purchase-application/deploy/backup-mysql.sh
# Change: RETENTION_DAYS=14
```

## Security Considerations

1. **Secrets Management**
   - Keep `deploy/.env` with mode 600
   - Never commit `.env` to Git
   - Use strong passwords (32+ characters)
   - Rotate secrets periodically

2. **Network Security**
   - Only nginx ports (80/443) are exposed
   - Database and pricing service are internal only
   - App binds to localhost only (127.0.0.1:8090)

3. **TLS/SSL**
   - Let's Encrypt certificates auto-renew
   - Force HTTPS in nginx config
   - HSTS headers enabled

4. **Database Backups**
   - Stored in `/root/` (root-only access)
   - Files created with mode 600
   - Keep backups on separate volume/server for disaster recovery

5. **Updates**
   - Keep system packages updated: `apt update && apt upgrade`
   - Monitor security advisories for Java, Spring Boot, MySQL
   - Test updates in staging before production

## Performance Tuning

### Database Optimization

```bash
# Increase MySQL memory if available
# Edit docker-compose.prod.yml:
mem_limit: 2g  # for db service

# Add MySQL configuration
# Create deploy/mysql-conf.d/my.cnf with:
[mysqld]
innodb_buffer_pool_size = 1G
max_connections = 200
```

### Application Tuning

```bash
# Increase Java heap size
# Edit docker-compose.prod.yml, add to app service:
environment:
  JAVA_OPTS: "-Xmx768m -Xms512m"
```

### nginx Tuning

```bash
# Edit /etc/nginx/nginx.conf
worker_processes auto;
worker_connections 1024;

# Enable gzip compression
gzip on;
gzip_types text/plain text/css application/json application/javascript;
```

## Monitoring

Consider adding monitoring tools:

- **Uptime monitoring**: UptimeRobot, Pingdom
- **Application monitoring**: Spring Boot Actuator metrics
- **Log aggregation**: ELK stack, Grafana Loki
- **Resource monitoring**: Netdata, Grafana + Prometheus

## Support

For issues or questions:
- GitHub Issues: [test-purchase-application/issues](https://github.com/mitetodb/test-purchase-application/issues)
- Check application logs first
- Include relevant log excerpts when reporting issues
