# Account Manager user guide (English)

Guide for **Account Managers** — manage assigned customers, shops, test purchases, and mystery shopper invites.

**URL:** https://testkauf.getinweb.de

---

## Sign in

1. Username and password on `/login`.
2. **Password:** minimum **12 characters** for new/changed passwords.
3. **Two-factor authentication (required):** same flow as Admin — enroll at `/2fa/enroll`, verify at `/2fa/verify`, store recovery codes from `/2fa/recovery-codes`. Recovery codes are one-time; re-issued codes **invalidate** older unused codes (old rows kept in the database).
4. If you lose your phone, use a **recovery code** on the verify page. Login lockout is cleared by an **Admin** via **Unlock** on the Users list.

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

- **Create** for managed customers (pick active customer + active shop).
- **Edit** purchases for managed customers.
- **Assign mystery shopper** to a purchase (you see shopper names; customers and sales managers do not).
- **Change status** on purchases you can view (same status enum as the rest of the app: INITIALISED, IN_PROGRESS, PRODUCT_ORDERED, …, CLOSED, CANCELLED).
- **Upload attachments** on purchase detail (allowlisted types, max size 10 MB default).
- **Lists:** filters, pagination, CSV/Excel export within export cap.
- **Cannot archive** purchases (Admin only). Archived purchases are read-only.

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
