# Account Manager user guide (English)

Guide for **Account Managers** — manage assigned customers, shops, test purchases, and mystery shopper invites.

**URL:** https://testkauf.getinweb.de

---

## Sign in

1. Username and password on `/login`.
2. **Password:** minimum **12 characters** for new/changed passwords.
3. **Two-factor authentication (TOTP)** is required only for **Admin** and **Account Manager**. As an Account Manager you must set up 2FA on **first login**:
   - After your password, redirect to **`/2fa/enroll`** — scan the QR code, confirm TOTP, then **save all recovery codes** on **`/2fa/recovery-codes`** (each code works once; re-issued sets **invalidate** older unused codes — rows stay in the database).
   - On later logins: password → **`/2fa/verify`** (TOTP or recovery code).
4. When login and 2FA succeed, you are redirected to **`/home`**.
5. If you lose your phone, use a **recovery code** on `/2fa/verify`. Login lockout is cleared by an **Admin** via **Unlock** on the Users list.

**Profile:** `/profile` — email, country, language, avatar (JPEG/PNG, magic-byte validated).

**Language:** globe or `?lang=en|bg|de`.

---

## What you can access

Under **Management**:

| Area | Scope |
|------|--------|
| Test purchases | Customers you **manage** only |
| Customers | Create (auto-assigned to you), edit **managed** customers |
| Shops | Full shop list — create and edit shops |
| Invites | Create mystery-shopper invite links |
| Invoices | List, statements, create/issue for **managed** customers |
| Score trends | `/score-trends` for managed customers |

You **cannot** open **Users** or **Audit log** (Admin only).

---

## Customers

- Add customers; they are linked to your **managed customers** set.
- Edit customer data and **base service fee** (pricing API sync when you save).
- You **cannot** deactivate customers from the UI (Admin-only **Deactivate** on customer list). Inactive customers cannot receive new test purchases.

---

## Shops

- Maintain shop master data used on test purchases.
- **Deactivate / reactivate shops:** Admin only in the UI.

---

## Test purchases

- **Create** for managed customers (pick active customer + active shop); optional **deadline** date.
- **Edit** purchases for managed customers.
- **Assign mystery shopper** to a purchase (you see shopper names; customers and sales managers do not).
- **Visit appointment:** schedule or confirm on purchase detail.
- **Questionnaire:** `/testpurchases/{id}/questionnaire` when allowed.
- **PDF report:** `/testpurchases/view/{id}/report.pdf` for purchases you can view.
- **Change status** on purchases you can view (same status enum as the rest of the app: INITIALISED, IN_PROGRESS, PRODUCT_ORDERED, …, CLOSED, CANCELLED).
- **Upload attachments** on purchase detail (allowlisted types, max size 10 MB default).
- **Lists:** filters, pagination, CSV/Excel export within export cap.
- **Cannot archive** purchases (Admin only). Archived purchases are read-only.

---

## Invoices (managed customers)

- Open **Invoices** from the navigation.
- Preview **monthly statement** per customer; **create draft** invoice from statement.
- **Issue**, **mark paid**, or **cancel** on invoice detail; download **PDF**.

Reminder and visit e-mails require operator configuration (`MAIL_ENABLED` and feature flags) — default is no outbound mail.

---

## Mystery shopper invites

1. Go to **Invites** (`/invites`).
2. Create invite — copy the link (`/register?token=…`) and send it securely to the new test buyer.
3. Token is single-use and expires after seven days.

---

## Privacy rules

- **Sales Manager** and **Customer** roles never see mystery shopper names in lists or detail views.
- Do not share invite links in public channels.

See [ROLES.md](../ROLES.md) for details.
