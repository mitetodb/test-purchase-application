# Testkauf – Original Vision

Source: 8 early developer-support chats (ChatGPT). They are about debugging and deployment, not product vision, and several are incomplete (earliest messages not loaded). Goals below are partly inferred from code shown there. No secrets or real data.

## Goal
Web app for mystery-shopping "test purchases": a customer orders a test purchase of products from a shop in a country; staff process it through statuses; a separate pricing service calculates the fees.

## Roles
| Role | Original intent | State |
|---|---|---|
| Admin | full rights, only one who deletes | built; tightened in #19 / PR #21 |
| Account Manager | manages own customers, sees their purchases | built (basic); planned #19 |
| Mystery Shopper | executes assigned purchases | built (basic); planned #19 |
| Customer / user | sees own purchases | planned #19 |
| Public registration | self sign-up | fix planned #20 |

## Workflow & features
| Item | State |
|---|---|
| Create purchase (customer, shop, country, category, type, items) | built |
| Price preview/calculation via pricing microservice (service + postage fee) | built |
| Sequential number TP-xxxx | built; concurrency-safe numbering: new gap #23 |
| Status changes with comment + status history | built |
| Email on status change, password-reset email | built |
| Attachments (receipts/photos), avatars | built; validation planned #9, backups #10 |
| Lists with filter/sort, Excel/CSV export | built (client-side); server pagination #15, PDF/Excel #11 |
| User management, profile, admin dashboard with charts | built |
| Multilingual UI | built (BG/EN/DE) |
| Audit/action logging | basic AOP logging built; full audit log #13 |
| Booking/appointment service | new gap #24 |
| Invoicing | not in original chats; planned #12 |
| HTTPS, closed ports, secrets out of git, backups | built (Hetzner); hardening #7, #8, #14, #18 |
| Cloud DB (managed RDS) | superseded by self-hosted move |
