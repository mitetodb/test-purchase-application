# Security

This document outlines security practices, considerations, and planned improvements for the Test Purchase Application.

## Current Security Implementation

### Authentication

**User Authentication**
- Password-based authentication using Spring Security
- Passwords hashed with BCrypt (adaptive hashing with salt)
- Session management with secure cookies
- Password reset via email with time-limited tokens

**Default Admin Account**
- Created on first startup (when users table is empty)
- Password set via `ADMIN_PASSWORD` environment variable (minimum 12 characters)
- Username: `admin`
- **Important:** Change password after first login; complete TOTP enrollment on first sign-in

**Two-factor authentication (TOTP)**
- Required after login for **ADMIN** and **ACCOUNT_MANAGER**
- Secrets encrypted at rest with `APP_TOTP_ENCRYPTION_KEY`
- Ten one-time recovery codes per enrollment; regenerating codes **invalidates** previous unused codes (`invalidated_at` / reason stored — rows not deleted)
- Endpoints: `/2fa/enroll`, `/2fa/verify`, `/2fa/recovery`, `/2fa/recovery-codes`

**Login protection**
- Rate limiting on login and pending-2FA endpoints (`LoginRateLimitFilter`)
- Account lockout after repeated failed password attempts; Admin **Unlock** on `/users`
- Generic error messages on failed login (no user enumeration)

### Authorization

**Role-Based Access Control (RBAC)**
- Five roles: Admin, Account Manager, Sales Manager, Customer, Mystery Shopper
- Enforced at controller and service layers using `@PreAuthorize` annotations
- Role stored in database (cannot be modified by users)
- Detailed permissions matrix: see [ROLES.md](ROLES.md)

**Method-Level Security**
```java
@PreAuthorize("hasRole('ADMIN')")
public void deactivateUser(UUID id) { ... }

@PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNT_MANAGER')")
public void assignMysteryShopperToPurchase(...) { ... }
```

**View-Level Security**
- Thymeleaf templates use `sec:authorize` for conditional rendering
- Direct URL access blocked by Spring Security filters
- Mystery shopper names hidden from Sales Manager and Customer roles

### Browser Security Headers

**Application (Spring Security)**
- `Content-Security-Policy` with `frame-ancestors 'self'` (no separate `X-Frame-Options` header)
- `X-Content-Type-Options: nosniff`
- `Referrer-Policy: strict-origin-when-cross-origin`
- `Permissions-Policy` disables geolocation, microphone, and camera by default
- `script-src` uses `'unsafe-inline'` only where required for existing Thymeleaf inline scripts and `onclick` handlers; `'unsafe-eval'` is not allowed
- `style-src` uses `'unsafe-inline'` for inline presentation attributes in templates

**Reverse proxy (testkauf nginx vhost only)**
- Security headers (including `Strict-Transport-Security` on HTTPS) come from the Spring app only; **do not** duplicate them in nginx
- `server_tokens off` hides nginx version on the testkauf vhost

**Verify headers (demo / local)**
```bash
# Direct app (integration tests cover this path)
BASE_URL=http://127.0.0.1:8080 scripts/check-security-headers.sh /login

# Production site (HTTPS; includes HSTS from the app)
BASE_URL=https://testkauf.getinweb.de scripts/check-security-headers.sh /login

# Manual spot check
curl -sI https://testkauf.getinweb.de/login | grep -iE '^(content-security-policy|x-frame-options|x-content-type-options|referrer-policy|permissions-policy|strict-transport-security|server:)'
```
Expect exactly one of each app header, no `X-Frame-Options`, and `Server` without an nginx version when the updated vhost is deployed.

### Network Security

**Architecture**
- App binds to `127.0.0.1:8090` (localhost only)
- MySQL and pricing service on internal Docker network (no public ports)
- HTTPS termination at nginx reverse proxy
- Let's Encrypt TLS certificates

**Inter-Service Communication**
- App ↔ Pricing Service: `X-API-Key` header authentication
- App ↔ Database: JDBC with least-privilege users
- Pricing Service ↔ Database: JDBC with separate least-privilege user

### Data Security

**Database**
- Passwords stored with BCrypt hashing (adaptive cost factor)
- Sensitive data (emails, personal info) in MySQL with encryption at rest
- Two databases with separate users (principle of least privilege)
- Regular automated backups (gzip compressed, mode 600)

**File Uploads**
- Avatars: JPEG/PNG only (`app.upload.avatar.allowed-types`)
- Attachments: JPEG, PNG, PDF, DOCX, XLSX by default (`app.upload.attachment.allowed-types`)
- Validation via **magic bytes** (Apache Tika), ZIP/OOXML inspection, blocked legacy `.doc`/`.xls`/macro types
- Max size: `spring.servlet.multipart.max-file-size` (default **10MB**)
- Storage: Docker volume `uploads` in production (avatars under `uploads/avatars/`)
- **No delete API** — attachment delete route returns **404**; files stay with the test purchase

### Session Management

**Security Features**
- Secure, HTTP-only cookies
- Session timeout after inactivity
- CSRF protection enabled (Spring Security default)
- No session fixation vulnerability (new session on login)

### Input Validation

**Current Implementation**
- Jakarta Bean Validation annotations (`@NotBlank`, `@Email`, `@Size`)
- Thymeleaf auto-escaping (XSS prevention)
- SQL injection protection via JPA/Hibernate parameterized queries

### Logging and Monitoring

**Current Capabilities**
- Spring Boot Actuator health endpoints (`/actuator/health` public)
- Application logs via Docker logs
- **Audit log** (Admin UI `/audit`) for sensitive changes — see `AuditLogService`
- Failed login lockout and rate-limit counters in database
- Password reset token usage logged

**Remaining gaps**
- No centralized SIEM/alerting integration
- No antivirus scanning on uploads

### Email Security

**Configuration**
- STARTTLS for SMTP (port 587) on Hetzner (blocks port 465)
- SSL for SMTP (port 465) on other hosts
- Configurable mail server (ALL-INKL in production)
- Optional: disabled by default (`MAIL_ENABLED=false`)

**Password Reset Security**
- Time-limited tokens (default: 24 hours)
- Single-use tokens (deleted after use)
- Tokens stored hashed in database
- Reset links sent via email only

**Password policy**
- Minimum length **12** enforced on registration and password change (`PasswordPolicyService`)
- Passwords hashed with BCrypt

**Registration**
- `/register` requires valid invite token; role assigned server-side as **MYSTERY_SHOPPER** only

## Known Security Limitations

### High Priority (See MASTERPLAN.md)

1. **[#19 Role-based access control enforced on the server](https://github.com/mitetodb/test-purchase-application/issues/19)** — continue expanding integration tests for edge cases.

2. **[#10 Backups of uploaded files plus an off-server copy](https://github.com/mitetodb/test-purchase-application/issues/10)** — operational plan in [BACKUP.md](BACKUP.md); server install is manual.

### Implemented (formerly open issues)

- **[#7 Login rate limiting and account lockout](https://github.com/mitetodb/test-purchase-application/issues/7)** — implemented
- **[#8 Password policy (min 12) and TOTP 2FA for Admin and Account Manager](https://github.com/mitetodb/test-purchase-application/issues/8)** — implemented
- **[#9 Upload validation by magic bytes with an allowlist](https://github.com/mitetodb/test-purchase-application/issues/9)** — implemented
- **[#13 Full audit log for clients, prices and users](https://github.com/mitetodb/test-purchase-application/issues/13)** — implemented (Admin audit UI)
- **[#15 Pagination and filters](https://github.com/mitetodb/test-purchase-application/issues/15)** — implemented on major lists
- **[#20 Public /register allows role=ADMIN](https://github.com/mitetodb/test-purchase-application/issues/20)** — invite-only registration; MYSTERY_SHOPPER only

## Security Best Practices

### For Administrators

1. **Change Default Passwords**
   - Change admin password immediately after deployment
   - Use strong, unique passwords (32+ characters)
   - Generate with: `openssl rand -base64 32`

2. **Secure Environment Variables**
   - Keep `deploy/.env` with mode 600 (root only)
   - Never commit `.env` to version control
   - Rotate secrets periodically

3. **Keep Software Updated**
   ```bash
   # System updates
   apt update && apt upgrade -y
   
   # Monitor security advisories for:
   # - Java 17
   # - Spring Boot 3
   # - MySQL 8.4
   # - Docker
   ```

4. **Monitor Logs**
   ```bash
   # Application logs
   docker compose -f deploy/docker-compose.prod.yml logs -f app
   
   # nginx access logs (watch for suspicious patterns)
   tail -f /var/log/nginx/access.log
   
   # nginx error logs
   tail -f /var/log/nginx/error.log
   ```

5. **Regular Backups**
   - Database backups run nightly via cron
   - Test restore procedure periodically
   - Store backups off-site for disaster recovery
   - Plan for file upload backups (see [#10 Backups of uploaded files plus an off-server copy](https://github.com/mitetodb/test-purchase-application/issues/10))

6. **Limit Access**
   - Use SSH keys instead of passwords
   - Disable root SSH login
   - Use firewall (ufw) to restrict ports
   - Only expose ports 80 (HTTP) and 443 (HTTPS)

7. **TLS Configuration**
   - Let's Encrypt certificates auto-renew
   - Test with: `certbot renew --dry-run`
   - Ensure HSTS is sent by the app (Spring Security) on HTTPS responses; do not duplicate it in nginx; run `scripts/check-security-headers.sh` after deploy

### For Developers

1. **Secure Coding Practices**
   - Always use `@PreAuthorize` on sensitive endpoints
   - Validate all user input
   - Use parameterized queries (JPA does this by default)
   - Escape output in templates (Thymeleaf does this by default)
   - Never log sensitive data (passwords, API keys)

2. **Dependency Management**
   - Keep Maven dependencies updated
   - Monitor for CVEs: `mvn dependency-check:check`
   - Review dependency updates carefully

3. **Testing**
   - Write security tests for authorization logic
   - Test role-based access control
   - Run `scripts/ci.sh` before committing
   - Never commit secrets (use `.gitignore`)

4. **Code Review**
   - Review all authentication/authorization changes
   - Check for SQL injection vulnerabilities
   - Verify input validation
   - Look for hardcoded secrets

5. **Secret Management**
   - Use environment variables for all secrets
   - Never commit `.env` or `application-prod.properties`
   - Use `git-secrets` or similar tools to prevent accidental commits
   - Example: `scripts/ci.sh` includes secret scanning

### For Users

1. **Strong Passwords**
   - Use unique passwords (not reused)
   - Minimum **12 characters** (enforced on change/register)
   - Use a password manager

2. **Account Security**
   - Log out when finished
   - Don't share credentials
   - Report suspicious activity to admin
   - Verify password reset emails (check sender)

3. **Browser Security**
   - Use latest browser version
   - Don't save passwords in browser (use password manager)
   - Avoid public WiFi without VPN
   - Verify HTTPS lock icon in browser

## Incident Response

### If Security Breach Suspected

1. **Immediate Actions**
   ```bash
   # Review recent access logs
   tail -n 1000 /var/log/nginx/access.log | grep -i suspicious_pattern
   
   # Check application logs for errors
   docker compose -f deploy/docker-compose.prod.yml logs --tail=1000 app | grep -i error
   
   # Check active sessions (if session management in database)
   docker compose -f deploy/docker-compose.prod.yml exec db mysql -u root -p -e "SELECT * FROM spring_session;"
   ```

2. **Containment**
   ```bash
   # Stop application if necessary
   docker compose -f deploy/docker-compose.prod.yml stop app
   
   # Change all secrets in deploy/.env
   nano deploy/.env
   
   # Force password reset for affected users (via admin panel)
   ```

3. **Investigation**
   - Identify affected systems and data
   - Determine attack vector
   - Document findings
   - Check database for unauthorized changes

4. **Recovery**
   - Restore from backup if data compromised
   - Update and patch systems
   - Restart services with new secrets
   - Monitor for further suspicious activity

5. **Post-Incident**
   - Document incident and response
   - Update security procedures
   - Implement additional controls
   - Notify affected users if required

## Compliance Considerations

### GDPR (if applicable)

**Personal Data Collected:**
- User names, emails, passwords (hashed)
- Customer information
- Test purchase details
- Uploaded files (avatars, attachments)

**User Rights:**
- Right to access: Admins can export user data
- Right to erasure: Normal admin operations never delete users or customers, only deactivate. A formal GDPR Art. 17 erasure request is an exception: it is handled manually by the owner (Dimitar), after his explicit decision, by anonymizing the personal data of the deactivated account (no hard delete of rows that other records reference).
- Right to rectification: Users can edit own profiles
- Right to portability: Manual export by admin

**Technical Measures:**
- Encryption in transit (HTTPS)
- Encryption at rest (MySQL data files)
- Access control (role-based)
- Audit logging — Admin `/audit` UI and `audit_log` table ([#13](https://github.com/mitetodb/test-purchase-application/issues/13))

### Data Retention

**Current Policy:**
- Deactivated users/customers/shops and archived test purchases are retained indefinitely for audit and reporting
- No in-application hard delete for users, customers, shops, test purchases, or attachments
- Database backups retained for 14 days
- Application logs rotate automatically (Docker default)

**Recommendations:**
- Define formal data retention policy
- Consider archival export before long-term storage
- Consider longer backup retention for compliance

## Security Roadmap

See [MASTERPLAN.md](MASTERPLAN.md) for the complete security improvement roadmap.

### Immediate (High Priority)

1. Complete RBAC test coverage — [#19](https://github.com/mitetodb/test-purchase-application/issues/19)
2. Implement upload + DB off-site backup automation on server — [#10](https://github.com/mitetodb/test-purchase-application/issues/10) / [BACKUP.md](BACKUP.md)

### Done

- Rate limiting and lockout ([#7](https://github.com/mitetodb/test-purchase-application/issues/7))
- Password min length + TOTP 2FA ([#8](https://github.com/mitetodb/test-purchase-application/issues/8))
- Upload magic-byte validation ([#9](https://github.com/mitetodb/test-purchase-application/issues/9))
- Audit log UI ([#13](https://github.com/mitetodb/test-purchase-application/issues/13))
- CSP / security headers ([#14](https://github.com/mitetodb/test-purchase-application/issues/14))
- Pagination, filters, export ([#15](https://github.com/mitetodb/test-purchase-application/issues/15))
- Invite-only registration ([#20](https://github.com/mitetodb/test-purchase-application/issues/20))

### Future

9. Security vulnerability scanning (OWASP Dependency-Check, SonarQube)
10. Web Application Firewall (WAF)
11. Intrusion Detection System (IDS)
12. Security Information and Event Management (SIEM)

## Security Contacts

For security issues:
1. **Do not** open public GitHub issues for security vulnerabilities
2. Contact repository maintainers directly
3. Provide detailed information about the vulnerability
4. Allow time for a fix before public disclosure

## References

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [Spring Security Reference](https://docs.spring.io/spring-security/reference/)
- [CWE Top 25](https://cwe.mitre.org/top25/)
- [NIST Cybersecurity Framework](https://www.nist.gov/cyberframework)
