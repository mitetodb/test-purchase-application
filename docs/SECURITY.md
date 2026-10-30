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
- Password set via `ADMIN_PASSWORD` environment variable
- Username: `admin`
- **Important:** Change password immediately after first login

### Authorization

**Role-Based Access Control (RBAC)**
- Five roles: Admin, Account Manager, Sales Manager, Customer, Mystery Shopper
- Enforced at controller and service layers using `@PreAuthorize` annotations
- Role stored in database (cannot be modified by users)
- Detailed permissions matrix: see [ROLES.md](ROLES.md)

**Method-Level Security**
```java
@PreAuthorize("hasRole('ADMIN')")
public void deleteUser(UUID id) { ... }

@PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNT_MANAGER')")
public void assignMysteryShopperToPurchase(...) { ... }
```

**View-Level Security**
- Thymeleaf templates use `sec:authorize` for conditional rendering
- Direct URL access blocked by Spring Security filters
- Mystery shopper names hidden from Sales Manager and Customer roles

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
- Avatars: User profile pictures
- Attachments: PDF and images for test purchases
- Storage: Local filesystem (`/opt/testkauf/uploads/`)
- **Current Limitations:**
  - No file type validation beyond extension check
  - No file size limits enforced
  - No antivirus scanning
  - See [#9 Upload Validation](https://github.com/mitetodb/test-purchase-application/issues/9)

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
- Spring Boot Actuator health endpoints (admin only)
- Application logs via Docker logs
- Password reset token usage logged
- **Limitations:**
  - No comprehensive audit logging
  - No failed login tracking
  - No alerting on security events
  - See [#14 Audit Logging](https://github.com/mitetodb/test-purchase-application/issues/14)

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

## Known Security Limitations

### High Priority (See MASTERPLAN.md)

1. **[#19 RBAC Enforcement](https://github.com/mitetodb/test-purchase-application/issues/19)**
   - Some endpoints may not have proper authorization checks
   - Need comprehensive security audit
   - Add integration tests for all role combinations

2. **[#20 Registration Role Assignment](https://github.com/mitetodb/test-purchase-application/issues/20)**
   - Potential privilege escalation during user registration
   - Role should be assigned by admin, not during self-registration

3. **[#7 Rate Limiting and Account Lockout](https://github.com/mitetodb/test-purchase-application/issues/7)**
   - No protection against brute force login attempts
   - No account lockout after failed attempts
   - No rate limiting on authentication endpoints

4. **[#8 Password Policy and TOTP 2FA](https://github.com/mitetodb/test-purchase-application/issues/8)**
   - No password complexity requirements
   - No password expiration
   - No two-factor authentication (TOTP)
   - Admins and Account Managers should require 2FA

5. **[#9 Upload Validation](https://github.com/mitetodb/test-purchase-application/issues/9)**
   - No MIME type validation (beyond file extension)
   - No file size limits
   - No antivirus scanning
   - No image dimension limits
   - Risk of malicious file uploads

6. **[#10 Upload Backups](https://github.com/mitetodb/test-purchase-application/issues/10)**
   - Database backups don't include uploaded files
   - No disaster recovery for user uploads
   - Files stored on local filesystem only

### Medium Priority

7. **[#14 Audit Logging](https://github.com/mitetodb/test-purchase-application/issues/14)**
   - No comprehensive audit trail
   - Cannot track who changed what and when
   - Needed for compliance and incident investigation

8. **[#15 API Rate Limiting per User](https://github.com/mitetodb/test-purchase-application/issues/15)**
   - No per-user API quotas
   - Risk of resource abuse

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
   - Plan for file upload backups (see [#10](https://github.com/mitetodb/test-purchase-application/issues/10))

6. **Limit Access**
   - Use SSH keys instead of passwords
   - Disable root SSH login
   - Use firewall (ufw) to restrict ports
   - Only expose ports 80 (HTTP) and 443 (HTTPS)

7. **TLS Configuration**
   - Let's Encrypt certificates auto-renew
   - Test with: `certbot renew --dry-run`
   - Ensure HSTS headers are set (nginx config)

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
   - Minimum 12 characters (no technical limit currently)
   - Mix uppercase, lowercase, numbers, symbols
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
- Right to deletion: Admin can deactivate (soft delete) or hard delete
- Right to rectification: Users can edit own profiles
- Right to portability: Manual export by admin

**Technical Measures:**
- Encryption in transit (HTTPS)
- Encryption at rest (MySQL data files)
- Access control (role-based)
- Audit logging (planned: [#14](https://github.com/mitetodb/test-purchase-application/issues/14))

### Data Retention

**Current Policy:**
- Soft deletes preserve data indefinitely
- Hard deletes (admin only) remove data permanently
- Database backups retained for 14 days
- Application logs rotate automatically (Docker default)

**Recommendations:**
- Define formal data retention policy
- Implement automated cleanup of old soft-deleted records
- Consider longer backup retention for compliance

## Security Roadmap

See [MASTERPLAN.md](MASTERPLAN.md) for the complete security improvement roadmap.

### Immediate (High Priority)

1. Implement rate limiting and account lockout - [#7](https://github.com/mitetodb/test-purchase-application/issues/7)
2. Add password complexity requirements and TOTP 2FA - [#8](https://github.com/mitetodb/test-purchase-application/issues/8)
3. Implement comprehensive upload validation - [#9](https://github.com/mitetodb/test-purchase-application/issues/9)
4. Set up automated upload backups - [#10](https://github.com/mitetodb/test-purchase-application/issues/10)
5. Complete RBAC security audit - [#19](https://github.com/mitetodb/test-purchase-application/issues/19)
6. Fix registration role assignment - [#20](https://github.com/mitetodb/test-purchase-application/issues/20)

### Soon (Medium Priority)

7. Add comprehensive audit logging - [#14](https://github.com/mitetodb/test-purchase-application/issues/14)
8. Implement per-user API rate limiting - [#15](https://github.com/mitetodb/test-purchase-application/issues/15)

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
