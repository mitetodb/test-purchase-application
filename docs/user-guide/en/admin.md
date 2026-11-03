# Admin user guide (English)

This guide describes what an **Administrator** can do in the Test Purchase Application, based on the live UI and server-side security rules.

**URL:** https://testkauf.getinweb.de

---

## Sign in

1. Open the login page and enter your username and password.
2. **Password policy:** new passwords must be at least **12 characters** (configurable via `app.security.password.min-length`).
3. **Two-factor authentication (required for Admin):**
   - First login after deployment: you are redirected to **`/2fa/enroll`** — scan the QR code with an authenticator app (TOTP, 6 digits, 30 s).
   - Confirm with one code, then **save the 10 recovery codes** shown on **`/2fa/recovery-codes`** (each code works once).
   - Later logins: password → **`/2fa/verify`** (TOTP or recovery code).
4. After successful 2FA, you reach the dashboard (`/dashboard`).

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
- Create and edit purchases for any customer/shop.
- **Assign mystery shopper:** action on purchase (`/testpurchases/{id}/assign-mystery-shopper`) — Admin and Account Manager only.
- **Change status** along the workflow (e.g. INITIALISED → IN_PROGRESS → … → CLOSED / CANCELLED). Status history is recorded.
- **Archive / Unarchive:** Admin only — sets status **ARCHIVED**; archived purchases are **read-only** (no status changes, no uploads). Show archived via list filter.
- **No hard delete:** legacy delete URLs return **404**.
- **Attachments:** upload (PDF, images, Office Open XML per allowlist); **download/preview only** — delete endpoint removed (404). Files validated by **magic bytes**, size limit `spring.servlet.multipart.max-file-size` (default 10 MB).
- **Export:** `/testpurchases/export.csv` or `.xlsx` with current filters.

---

## Invites (mystery shoppers)

- **Invites** (`/invites`): create one-time invite link (7-day expiry, hashed in DB).
- Registration **`/register?token=…`** always creates **MYSTERY_SHOPPER** — no public self-signup without token.

---

## Audit log

**Audit log** (`/audit`): filter by action, entity type, actor, date range; paginated (25 rows). Covers user/customer/shop lifecycle, test purchase status, pricing-related events, and similar actions recorded by the application.

---

## System notes

- **Swagger / OpenAPI:** only if `SWAGGER_ENABLED=true` (default off).
- **Health:** `/actuator/health` (public); use for monitoring behind nginx.
- **Operations:** deployment, backup, rollback, 2FA break-glass — see [ADMIN-OPS.md](../ADMIN-OPS.md).

For the full permission matrix see [ROLES.md](../ROLES.md).
