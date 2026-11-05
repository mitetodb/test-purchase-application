# Admin user guide (English)

This guide describes what an **Administrator** can do in the Test Purchase Application, based on the live UI and server-side security rules.

**URL:** https://testkauf.getinweb.de

---

## Sign in

1. Open the login page and enter your username and password.
2. **Password policy:** new passwords must be at least **12 characters** (configurable via `app.security.password.min-length`).
3. **Two-factor authentication (TOTP)** is required only for **Admin** and **Account Manager** (not for Sales Manager, Customer, or Mystery Shopper). As an Administrator you must complete 2FA after every successful password check:
   - **First login, not enrolled yet:** redirect to **`/2fa/enroll`** — scan the QR code with an authenticator app (TOTP, 6 digits, 30 s), confirm with one code, then **save all 10 recovery codes** on **`/2fa/recovery-codes`** before you continue.
   - **Already enrolled:** redirect to **`/2fa/verify`** — enter the current TOTP or a one-time recovery code.
4. When login (and 2FA, if applicable) succeeds, the application redirects you to **`/home`** (not `/dashboard`).

**Change password:** profile menu → **Profile** → update password (must meet minimum length).

**Language:** globe menu or `?lang=en|bg|de` (stored in a cookie).

---

## Navigation (Admin menu)

Under **Admin** you can open:

| Item | Path | Purpose |
|------|------|---------|
| Users | `/users` | Create, edit, deactivate/reactivate, unlock |
| Audit log | `/audit` | Filterable history of sensitive changes |
| Test purchases | `/testpurchases` | Full list, filters, export, archive |
| Customers | `/customers` | All customers, pricing, deactivate/reactivate |
| Shops | `/shops` | Create, edit, deactivate/reactivate |
| Invoices | `/invoices` | List, monthly statement, issue/mark paid/cancel |

Under **Management** (or reports menu): **Score trends** at `/score-trends` (aggregated questionnaire scores).

---

## User management

- **List:** pagination, filters, sort; export **CSV** or **Excel** (`/users/export.csv`, `/users/export.xlsx`) up to `app.list-export.max-rows` (default 10 000).
- **Create user:** `/users/add` — choose role, country, password.
- **Edit user:** role, active flag, managed customers (for Account Managers).
- **Deactivate / Reactivate:** soft flag only — **no hard delete**. Deactivation ends active sessions.
- **Unlock:** clears login lockout after too many failed attempts (`/users/unlock/{id}`).

Users receive sequential **entity numbers** (see audit log and lists).

---

## Customers and shops

- **Customers:** create/edit for any customer; set base service fee (synced to pricing service when allowed). **Deactivate** inactive companies; **Reactivate** from list with “show deactivated”. Only Admin can reactivate via UI.
- **Shops:** manage all shops; deactivate/reactivate. New test purchases cannot target inactive customers or shops.

---

## Test purchases

- View **all** purchases (filter by status, customer, dates, etc.).
- Create and edit purchases for any customer/shop; optional **deadline** date (overdue shown on detail when past due and not closed).
- **Assign mystery shopper:** action on purchase (`/testpurchases/{id}/assign-mystery-shopper`) — Admin and Account Manager only.
- **Visit appointment:** schedule or confirm a shop visit on purchase detail (optional e-mail only if operator enabled mail + `MAIL_VISIT_APPOINTMENT_ENABLED`).
- **Questionnaire:** open `/testpurchases/{id}/questionnaire` to fill or review scored responses (Admin, Account Manager, assigned shopper).
- **PDF report:** link or URL `/testpurchases/view/{id}/report.pdf` — download summary PDF for any purchase you can view.
- **Change status** along the workflow (e.g. INITIALISED → IN_PROGRESS → … → CLOSED / CANCELLED). Status history is recorded.
- **Archive / Unarchive:** Admin only — sets status **ARCHIVED**; archived purchases are **read-only** (no status changes, no uploads). Show archived via list filter.
- **No hard delete:** legacy delete URLs return **404**.
- **Attachments:** upload (PDF, images, Office Open XML per allowlist); **download/preview only** — delete endpoint removed (404). Files validated by **magic bytes**, size limit `spring.servlet.multipart.max-file-size` (default 10 MB).
- **Export:** `/testpurchases/export.csv` or `.xlsx` with current filters.

---

## Invoices

- **List** (`/invoices`): filter by customer, status, period; pagination.
- **Monthly statement** (`/invoices/statement`): pick customer + year/month to preview billable test purchases before invoicing.
- **Create draft** from statement (Admin and Account Manager for customers they manage).
- On invoice detail: **Issue**, **Mark paid**, **Cancel** (audit log records lifecycle); **Download PDF** (`/invoices/{id}/pdf`).
- Status flow: **DRAFT** → **ISSUED** → **PAID**, or **CANCELLED**.

---

## Invites (mystery shoppers)

- **Invites** (`/invites`): create one-time invite link (7-day expiry, hashed in DB).
- Registration **`/register?token=…`** always creates **MYSTERY_SHOPPER** — no public self-signup without token.

---

## Audit log

**Audit log** (`/audit`): filter by action, entity type, actor, date range; paginated (25 rows). Covers user/customer/shop lifecycle, test purchase status, **invoices**, profile/password/TOTP events, pricing-related events, and similar actions recorded by the application.

---

## System notes

- **Swagger / OpenAPI:** only if `SWAGGER_ENABLED=true` (default off).
- **Health:** `/actuator/health` (public); optional `backup` / `serverErrors` components — see [MONITORING.md](../MONITORING.md).
- **E-mail defaults:** deadline reminders, visit mail, and monitoring alerts are **off** until env flags are set on the server — see [STATUS.md](../STATUS.md).
- **Operations:** deployment, backup, rollback, 2FA break-glass — see [ADMIN-OPS.md](../ADMIN-OPS.md).

For the full permission matrix see [ROLES.md](../ROLES.md).
