# Test Purchase Application

A comprehensive system for managing test purchases (Mystery Shopping orders) used by companies that perform Mystery Shopping services for various clients. The application includes a separate Price Calculation microservice that provides REST API for cost calculations.

## Technology Stack

- **Java**: 17
- **Spring Boot**: 3.4.0
- **Build Tool**: Maven
- **Database**: MySQL 8
- **Frontend**: Spring MVC + Thymeleaf
- **Security**: Spring Security
- **Caching**: Spring Cache
- **Scheduling**: Spring Scheduling
- **Inter-service Communication**: Spring Cloud OpenFeign
- **Email**: Spring Mail

## Architecture

The solution consists of two independent Spring Boot applications:

1. **Main Application** (Port 8080) - Core backend system with web interface
2. **Price Calculation Microservice** (Port 8081) - REST API for price calculations

## Features

### Domain Entities
- **Customer** - Client companies that order mystery shopping services
- **Shop** - Retail locations where test purchases are made
- **TestPurchase** - Test purchase orders with items and status tracking
- **User** - Application users with different roles
- **Item** - Products purchased in test purchases
- **Attachment** - Documents and files attached to test purchases

### Functionalities

#### Test Purchase Management
1. Create new test purchase orders
2. Edit existing test purchases
3. Delete test purchases
4. Change test purchase status with history tracking
5. View test purchase details with attachments and history

#### Customer Management
6. Create new customers
7. Edit customer information
8. Delete customers
9. View customer details and associated test purchases

#### Shop Management
10. Create new shops
11. Edit shop information
12. Delete shops

#### Attachment Management
13. Upload attachments to test purchases
14. Delete attachments (admin only)

### Security Features

- **Authentication**: Form-based login
- **Authorization**: Role-based access control
- **Roles**: 
  - ADMIN - Full system access
  - ACCOUNT_MANAGER - Manages assigned customers
  - SALES_MANAGER - Manages shops and sales
  - CUSTOMER - Views own test purchases
  - MYSTERY_SHOPPER - Creates and manages assigned test purchases
- **CSRF Protection**: Enabled
- **Password Reset**: Email-based password reset functionality
- **User Management**: Admins can manage roles, block/unblock users

### Technical Features

- **Caching**: Implemented for Shops and Customers using Spring Cache
- **Scheduling**: 
  - Daily cleanup of expired password reset tokens (cron)
  - Periodic statistics update for old test purchases (fixed delay)
- **Error Handling**: Custom error pages for 404, 403, 400, 500 errors
- **Validation**: Comprehensive validation on DTOs, entities, and service layers
- **Logging**: AOP-based action logging and service-level logging

## Integrations

### Price Calculation Microservice
- **Feign Client**: Used for inter-service communication
- **Endpoints Used**:
  - POST `/api/pricing/calculate` - Calculate test purchase fees
  - PUT `/api/pricing/customers/{id}/base-fee` - Update customer base fee
  - DELETE `/api/pricing/customers/{id}/base-fee` - Delete customer base fee
  - GET `/api/pricing/customers/{id}/base-fee` - Get customer base fee

## Setup Instructions

### Prerequisites
- Java 17
- Maven 3.6+
- Docker (for MySQL)
- MySQL 8

### Database Setup

1. Start MySQL with Docker:
```bash
docker run --name mysql8_db -p 3306:3306 -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=testpurchase_app -d mysql:8
```

### Application Setup

1. **Start Price Calculation Microservice** (Port 8081):
```bash
cd price-calculation-microservice
mvn spring-boot:run
```

2. **Start Main Application** (Port 8080):
```bash
cd test-purchase-application
mvn spring-boot:run
```

### Default Admin Account
- Username: `admin`
- Password: `123123`
- Created automatically on first startup if no users exist

## Repository Links

- **Main Application**: https://github.com/mitetodb/test-purchase-application
- **Price Calculation Microservice**: https://github.com/mitetodb/price-calculation-microservice

## Testing

The application includes:
- Unit tests for services
- Integration tests for repositories
- API tests for controllers
- Security tests

Run tests with:
```bash
mvn test
```

## Project Structure

```
src/main/java/app/
├── client/          # Feign clients for microservice communication
├── config/          # Configuration classes
├── controller/      # MVC controllers
├── exception/       # Custom exceptions and handlers
├── model/           # Entities, DTOs, enums
├── repository/      # JPA repositories
├── security/        # Security configuration
└── service/         # Business logic services
```

## License

This project is part of a Spring Advanced course assignment.

---

## Production deployment (testkauf.getinweb.de)

**Stack** (`deploy/docker-compose.prod.yml`): `app` (this repo), `pricing-svc` (built from the sibling repo
[`price-calculation-microservice`](https://github.com/mitetodb/price-calculation-microservice), or set `PRICING_IMAGE`) and `db` (MySQL 8.4, one instance with
two databases and least-privilege users) on an **internal** Docker network. Every service has a `mem_limit` and a healthcheck.
MySQL and pricing-svc publish **no ports**. The app is bound only to `127.0.0.1:${APP_HOST_PORT}` (default 8090).
Mailpit is available as an optional profile (`--profile mail`), and its UI is not published.

```bash
# on the server, with both repos side by side, e.g. /opt/testkauf/{test-purchase-application,price-calculation-microservice}
cd test-purchase-application
cp .env.example deploy/.env && chmod 600 deploy/.env   # fill in strong secrets (openssl rand -base64 32)
docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env up -d --build
docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env ps     # all healthy?
```

**HTTPS / reverse proxy.** The server already runs another stack whose proxy owns 80/443, so this stack does **not** start its own proxy.
See `deploy/nginx/testkauf.getinweb.de.conf`:
- **Option A (host nginx):** copy the vhost to `/etc/nginx/sites-available`, enable it, then run `certbot --nginx -d testkauf.getinweb.de`. The upstream is `127.0.0.1:8090`.
- **Option B (proxy in Docker):** add the proxy's external network to `app` (see the commented `proxy` network), remove `app.ports`, and point the proxy at `http://app:8080`.

Set a DNS A/AAAA record for `testkauf.getinweb.de` pointing at the server before you request the certificate.

**Configuration** is env only (`.env.example` lists every variable). Key settings:
`SPRING_PROFILES_ACTIVE=prod`, `APP_BASE_URL` (used in password-reset links), `PRICING_API_KEY` (sent as `X-API-Key`; it must match the pricing service),
`ADMIN_PASSWORD` (the initial admin is created only when the users table is empty), `MAIL_ENABLED` (default `false`, so mail is a logged no-op), `DEMO_DATA_ENABLED`, `SWAGGER_ENABLED`.

**Database migrations.** Flyway runs `src/main/resources/db/migration`. `V1__baseline.sql` is the schema at the time Flyway was introduced.
Existing databases are baselined at v1 automatically (`baseline-on-migrate`), and Hibernate runs with `ddl-auto=validate` in prod.
If an old database created by `ddl-auto=update` fails validation, start it once with `JPA_DDL_AUTO=none`, then add a corrective `V2__*.sql`.

**Backups:** `deploy/backup-mysql.sh` (mysqldump → gzip, `RETENTION_DAYS=14` rotation, files saved with mode 600). Example cron line:
`15 3 * * * /opt/testkauf/test-purchase-application/deploy/backup-mysql.sh >> /var/log/testkauf-backup.log 2>&1`

**i18n:** BG / EN / DE message bundles (`messages_*.properties`). Switch the language with the navbar globe menu or `?lang=bg|en|de`; the choice is stored in a cookie.

**API docs:** springdoc is at `/swagger-ui.html` and `/v3/api-docs`. Both are off in prod unless `SWAGGER_ENABLED=true`.

**CI:** `scripts/ci.sh` runs a secret scan, `mvn verify`, a compose config check that fails if an internal port is published, and (with `DOCKER_BUILD=1`) the image build.
The Dockerfile is a multi-stage Java 17 build that runs as a non-root user.
