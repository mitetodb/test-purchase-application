# Master Plan

This document outlines the development roadmap for the Test Purchase Application, organized by priority.

See [ORIGINAL_VISION.md](ORIGINAL_VISION.md) for the original project goals and feature implementation status.

## High Priority

Critical security and operational improvements that should be implemented first:

- [#19 Role-based access control enforced on the server](https://github.com/mitetodb/test-purchase-application/issues/19) - Enforce granular permissions for all roles
- [#20 Public /register allows role=ADMIN](https://github.com/mitetodb/test-purchase-application/issues/20) - Prevent privilege escalation during user registration
- [#7 Login rate limiting and account lockout](https://github.com/mitetodb/test-purchase-application/issues/7) - Protect against brute force attacks
- [#8 Password policy (min 12) and TOTP 2FA for Admin and Account Manager](https://github.com/mitetodb/test-purchase-application/issues/8) - Strengthen authentication security
- [#9 Upload validation by magic bytes with an allowlist](https://github.com/mitetodb/test-purchase-application/issues/9) - Validate file types, sizes, and content
- [#10 Backups of uploaded files plus an off-server copy](https://github.com/mitetodb/test-purchase-application/issues/10) - Protect user-uploaded files with automated backups

## Medium Priority

Important features and improvements for better usability and monitoring:

- [#11 PDF report per test purchase and Excel export](https://github.com/mitetodb/test-purchase-application/issues/11) - Generate reports and exports
- [#12 Invoices and monthly statements per client](https://github.com/mitetodb/test-purchase-application/issues/12) - Financial reporting per client
- [#13 Full audit log for clients, prices and users](https://github.com/mitetodb/test-purchase-application/issues/13) - Track all user actions and data changes
- [#14 CSP and security header cleanup](https://github.com/mitetodb/test-purchase-application/issues/14) - Security headers and content security policy
- [#15 Pagination and filters](https://github.com/mitetodb/test-purchase-application/issues/15) - Improve data navigation and filtering
- [#23 Concurrency-safe sequential numbers for test purchases, customers and shops](https://github.com/mitetodb/test-purchase-application/issues/23) - Prevent race conditions in sequential numbering

## Later

Future enhancements that can be scheduled after core improvements:

- [#16 Deadlines and email reminders](https://github.com/mitetodb/test-purchase-application/issues/16) - Deadline management and automated reminders
- [#17 Scored questionnaire and client trends dashboard](https://github.com/mitetodb/test-purchase-application/issues/17) - Enhanced client analytics and questionnaires
- [#18 Alerts on health.getinweb.de for missing backup or 5xx errors](https://github.com/mitetodb/test-purchase-application/issues/18) - Health monitoring and alerting
- [#24 Appointment/visit booking for mystery shoppers](https://github.com/mitetodb/test-purchase-application/issues/24) - Schedule and manage shop visit appointments

## Implementation Notes

- Each phase should be completed and tested before moving to the next
- Security issues in the High Priority phase are blockers for production deployment
- Medium Priority features enhance usability but are not security-critical
- Later phase items can be implemented based on user feedback and business needs
