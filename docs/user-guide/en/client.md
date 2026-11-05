# Client (customer) user guide (English)

Guide for **Customer** users — companies that request and track **test purchases** for their own organization.

**URL:** https://testkauf.getinweb.de

---

## Sign in

1. Use the username and password provided by your administrator (customers are created by Admin, not via public registration).
2. **Password policy:** at least **12 characters** when setting or changing password.
3. **Two-factor authentication** is **not** required for Customer (only **Admin** and **Account Manager** must use TOTP).
4. After a successful password login you are redirected to **`/home`**.
5. **Profile:** `/profile` for email, country, language, and avatar (JPEG/PNG).

**Language:** globe or `?lang=en|bg|de`.

---

## Your scope

You see **only test purchases for your company** (linked to your user account).

You **can**:

- Open **Test purchases** from the navigation
- Open **Invoices** (your company only) and **Score trends** (`/score-trends`) for your company’s aggregated scores
- **Create** new test purchases for your company (active customer + active shop); optional **deadline**
- Add or edit **line items** on your purchases while they are editable
- View **status** and **status history**
- **Preview price** (service fee/postage via pricing service)

You **cannot**:

- See other companies’ data
- Assign mystery shoppers or change most workflow statuses (account managers/admins handle that)
- See the mystery shopper’s **name** (hidden in UI and API for your role)
- Open **Customers**, **Users**, **Shops** management (`/shops` returns forbidden for Customer)
- **Upload purchase attachments** — upload button is shown only for Admin, Account Manager, and Mystery Shopper; you can **download** attachments others uploaded

---

## Creating a test purchase

1. **Test purchases** → create.
2. Select your company (fixed to your account) and an **active** shop.
3. Add items, description, and required fields.
4. Save — initial status is typically **INITIALISED** (exact transitions are managed by your account manager).

---

## Tracking progress

- Open a purchase to see current **status**, history, attachments, **deadline** (if set), and questionnaire **results** (read-only).
- **PDF report:** download from purchase detail or `/testpurchases/view/{id}/report.pdf`.
- **Invoices:** view your company’s invoices and download PDFs; you cannot create or issue invoices.
- Email notifications may appear if the operator enabled `MAIL_ENABLED` on the server (deadline reminders are a separate flag — default off).
- Purchases in **ARCHIVED** status are read-only.

---

## Privacy

Mystery shopper identities are confidential. You will see that a shopper is assigned without seeing their personal name.

For permissions see [ROLES.md](../ROLES.md). For support, contact your account manager or system admin.
