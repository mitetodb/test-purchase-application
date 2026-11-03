# Test buyer (mystery shopper) user guide (English)

Guide for **Mystery Shoppers** (test buyers) who execute assigned test purchases.

**URL:** https://testkauf.getinweb.de

---

## How you join

1. An **Admin** or **Account Manager** sends you an **invite link** (`/register?token=…`).
2. Complete registration (password at least **12 characters**). Your role is always **Mystery Shopper** — you cannot choose another role.
3. Sign in at `/login`.

**No TOTP:** two-factor authentication is required only for Admin and Account Manager, not for mystery shoppers.

**Profile:** `/profile` — update email, country, language, avatar.

**Language:** globe or `?lang=en|bg|de`.

---

## What you see

- **Test purchases** menu: only purchases **assigned to you**.
- **Dashboard:** summary of your assignments.
- You may open **shop** information as shown on purchase detail (you do not manage the shop list — `/shops` is not available to your role).

You **cannot**:

- Create test purchases or assign yourself
- See other shoppers’ assignments or unassigned work
- View the customer directory or user list
- See purchases for companies you are not assigned to

Customers and sales managers **do not see your name** on their views; admins and account managers do.

---

## Working an assignment

1. Open the purchase from **Test purchases**.
2. Read shop details, line items, instructions, and any existing attachments.
3. **Change status** as work progresses (you are allowed to update status on purchases you can view). Typical flow uses statuses such as:
   - **INITIALISED** / **IN_PROGRESS**
   - **PRODUCT_ORDERED**, **PRODUCT_RECEIVED**
   - Forward/return steps (**PRODUCT_FORWARDED_TO_CLIENT**, **PRODUCT_SHIPPED_BACK_TO_SELLER**)
   - **CLOSED** or **CANCELLED** when instructed
4. Add an optional **comment** when changing status (stored in status history).
5. **Upload attachments** (receipts, photos, PDF/Office documents per server allowlist) on the purchase page — button visible for your role. Files are checked by content (magic bytes), not only file extension.
6. **Download** attachments; there is **no delete** action in the UI.

If a purchase is **ARCHIVED**, it is read-only (no uploads or status changes).

---

## Tips

- Keep your password unique and at least 12 characters.
- Log out on shared devices (**Logout** → `/logout`).
- Contact your account manager if an assignment looks wrong or you need a new invite after expiry.

See [ROLES.md](../ROLES.md).
