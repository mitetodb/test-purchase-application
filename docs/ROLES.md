# Roles and Permissions

This document defines the five roles in the Test Purchase Application and their permissions, as enforced by server-side access control (see PR #21).

## Roles Overview

| Role | Purpose | Key Responsibilities |
|------|---------|---------------------|
| **Admin** | System administrator | Full system access, user management; deactivates or archives records (never hard-deletes) |
| **Account Manager** | Customer relationship management | Manage assigned customers, assign mystery shoppers |
| **Sales Manager** | Sales oversight | View assigned customers' purchases, generate reports (no shopper names) |
| **Customer** | Request test purchases | Create and track company's test purchases (no shopper names) |
| **Mystery Shopper** | Execute test purchases | View and update assigned purchases, upload results |

## Permission Matrix

Server-side role-based access control is enforced in controllers, services, and repository queries.

### Test Purchases

| Permission | Admin | Account Manager | Sales Manager | Customer | Mystery Shopper |
|-----------|:-----:|:---------------:|:-------------:|:--------:|:---------------:|
| View test purchases | ✅ All | ✅ Managed customers' | ✅ Owned customers' | ✅ Own company's | ✅ Assigned only |
| Create test purchase | ✅ | ✅ | ❌ | ✅ (company only) | ❌ |
| Edit test purchase | ✅ | ✅ | ❌ | ❌ | ❌ |
| Delete / archive test purchase | ✅ Archive | ❌ | ❌ | ❌ | ❌ |
| **Change status** | ✅ | ✅ | ❌ | ❌ | ✅ **Own assignments** |
| **Assign mystery shopper** | ✅ | ✅ | ❌ | ❌ | ❌ |
| **See mystery shopper name** | ✅ | ✅ | ❌ | ❌ | ✅ (own) |

### Customers

| Permission | Admin | Account Manager | Sales Manager | Customer | Mystery Shopper |
|-----------|:-----:|:---------------:|:-------------:|:--------:|:---------------:|
| View customers | ✅ All | ✅ Managed only | ✅ Owned only | ❌ | ❌ |
| Create customer | ✅ | ✅ (auto-managed) | ✅ (auto-owned) | ❌ | ❌ |
| Edit customer | ✅ | ✅ **Managed only** | ✅ **Owned only** | ❌ | ❌ |
| Delete customer | ❌ Deactivate only (Admin UI) | ❌ | ❌ | ❌ | ❌ |
| Deactivate customer | ✅ | ❌ (Admin UI) | ❌ | ❌ | ❌ |
| Reactivate customer | ✅ | ❌ | ❌ | ❌ | ❌ |
| Set pricing | ✅ | ✅ | ✅ | ❌ | ❌ |

### Users and System

| Permission | Admin | Account Manager | Sales Manager | Customer | Mystery Shopper |
|-----------|:-----:|:---------------:|:-------------:|:--------:|:---------------:|
| View all users | ✅ | ❌ | ❌ | ❌ | ❌ |
| Create users | ✅ | ❌ | ❌ | ❌ | ❌ |
| Invite mystery shoppers | ✅ | ✅ | ❌ | ❌ | ❌ |
| Edit any user | ✅ | ❌ | ❌ | ❌ | ❌ |
| Deactivate user | ✅ | ❌ | ❌ | ❌ | ❌ |
| Edit own profile | ✅ | ✅ | ✅ | ✅ | ✅ |
| Upload avatar | ✅ | ✅ | ✅ | ✅ | ✅ |
| Change own password | ✅ | ✅ | ✅ | ✅ | ✅ |
| Reset password (any user) | ✅ | ❌ | ❌ | ❌ | ❌ |

### Shops

| Permission | Admin | Account Manager | Sales Manager | Customer | Mystery Shopper |
|-----------|:-----:|:---------------:|:-------------:|:--------:|:---------------:|
| View all shops | ✅ | ✅ | ❌ (no `/shops` access) | ❌ | ❌ |
| Create shop | ✅ | ✅ | ❌ | ❌ | ❌ |
| Edit any shop | ✅ | ✅ | ❌ | ❌ | ❌ |
| Deactivate shop | ✅ | ❌ (Admin UI) | ❌ | ❌ | ❌ |
| Reactivate shop | ✅ | ❌ | ❌ | ❌ | ❌ |

### Items (Purchase Line Items)

| Permission | Admin | Account Manager | Sales Manager | Customer | Mystery Shopper |
|-----------|:-----:|:---------------:|:-------------:|:--------:|:---------------:|
| View items (any purchase) | ✅ | ✅ Managed | ✅ Owned | ❌ | ❌ |
| View items (own/assigned) | ✅ | ✅ | ✅ | ✅ | ✅ |
| Add/edit/delete items | ✅ | ✅ | ❌ | ✅* | ❌ |

\* *Customers: only on their company's purchases*

### Attachments

| Permission | Admin | Account Manager | Sales Manager | Customer | Mystery Shopper |
|-----------|:-----:|:---------------:|:-------------:|:--------:|:---------------:|
| View attachments (any) | ✅ | ✅ Managed | ✅ Owned | ❌ | ❌ |
| View attachments (own/assigned) | ✅ | ✅ | ✅ | ✅ | ✅ |
| Upload attachment | ✅ | ✅ | ❌ | ❌ | ✅ |
| Delete attachment (any) | ❌ Retained | ❌ | ❌ | ❌ | ❌ |
| Delete attachment (own) | ❌ Retained | ❌ | ❌ | ❌ | ❌ |

### Status History

| Permission | Admin | Account Manager | Sales Manager | Customer | Mystery Shopper |
|-----------|:-----:|:---------------:|:-------------:|:--------:|:---------------:|
| View history (any purchase) | ✅ | ✅ Managed | ✅ Owned | ❌ | ❌ |
| View history (own/assigned) | ✅ | ✅ | ✅ | ✅ | ✅ |

### Reports and Analytics

| Permission | Admin | Account Manager | Sales Manager | Customer | Mystery Shopper |
|-----------|:-----:|:---------------:|:-------------:|:--------:|:---------------:|
| Generate system reports | ✅ | ✅ | ✅ | ❌ | ❌ |
| View customer analytics | ✅ | ✅ Managed | ✅ Owned | ❌ | ❌ |
| View own statistics | ✅ | ✅ | ✅ | ✅ | ✅ |
| **Download test purchase PDF report** | ✅ Scoped | ✅ Managed | ✅ Owned | ✅ Own company | ✅ Assigned |
| **View score trends** (`/score-trends`) | ✅ All scoped | ✅ Managed | ✅ Owned | ✅ Own company | ❌ |

PDF report URL: `/testpurchases/view/{id}/report.pdf` — same visibility rules as viewing the purchase.

### Deadlines

| Permission | Admin | Account Manager | Sales Manager | Customer | Mystery Shopper |
|-----------|:-----:|:---------------:|:-------------:|:--------:|:---------------:|
| Set/edit deadline on purchase | ✅ | ✅ Managed | ❌ | ✅ Own company (create/edit) | ❌ |
| See deadline / overdue indicator | ✅ | ✅ Managed | ✅ Owned | ✅ Own company | ✅ Assigned |

**Deadline reminder e-mails** are sent only when the operator enables `MAIL_ENABLED` and `DEADLINE_REMINDER_ENABLED` on the server (default off).

### Visit appointments

| Permission | Admin | Account Manager | Sales Manager | Customer | Mystery Shopper |
|-----------|:-----:|:---------------:|:-------------:|:--------:|:---------------:|
| Schedule or update visit slot | ✅ | ✅ Managed | ❌ | ❌ | ✅ Assigned |
| Confirm visit | ✅ | ✅ Managed | ❌ | ❌ | ✅ Assigned |
| View appointment on purchase | ✅ | ✅ Managed | ✅ Owned | ✅ Own company | ✅ Assigned |

Optional notification e-mail when `MAIL_ENABLED` and `MAIL_VISIT_APPOINTMENT_ENABLED` are true (default off).

### Questionnaire (scored)

| Permission | Admin | Account Manager | Sales Manager | Customer | Mystery Shopper |
|-----------|:-----:|:---------------:|:-------------:|:--------:|:---------------:|
| Fill questionnaire | ✅ | ✅ Managed | ❌ | ❌ | ✅ Assigned |
| View questionnaire result on purchase | ✅ | ✅ Managed | ✅ Owned | ✅ Own company | ✅ Assigned |

Form: `/testpurchases/{id}/questionnaire`. Sales Manager and Customer see aggregated scores on trends, not the fill form.

### Invoices and monthly statements

| Permission | Admin | Account Manager | Sales Manager | Customer | Mystery Shopper |
|-----------|:-----:|:---------------:|:-------------:|:--------:|:---------------:|
| List/view invoices (scoped) | ✅ | ✅ Managed | ✅ Owned | ✅ Own company | ❌ |
| Monthly statement preview | ✅ | ✅ Managed | ✅ Owned | ✅ Own company | ❌ |
| Create draft from statement | ✅ | ✅ Managed | ❌ | ❌ | ❌ |
| Issue / mark paid / cancel | ✅ | ✅ Managed | ❌ | ❌ | ❌ |
| Download invoice PDF | ✅ | ✅ Managed | ✅ Owned | ✅ Own company | ❌ |

Invoice statuses: **DRAFT**, **ISSUED**, **PAID**, **CANCELLED**. Mystery shoppers have no invoice menu access.

### System Administration

| Permission | Admin | Account Manager | Sales Manager | Customer | Mystery Shopper |
|-----------|:-----:|:---------------:|:-------------:|:--------:|:---------------:|
| Access API documentation | ✅ | ❌ | ❌ | ❌ | ❌ |
| View system health | ✅ | ❌ | ❌ | ❌ | ❌ |
| View aggregate `/actuator/health` (nginx) | Public | Public | Public | Public | Public |
| Manage system settings | ✅ | ❌ | ❌ | ❌ | ❌ |
| Access audit logs | ✅ | ❌ | ❌ | ❌ | ❌ |

Audit entries are written for deactivation, status changes, pricing updates, login unlock, **invoice lifecycle**, profile/password/TOTP flows, and other sensitive actions (see `AuditAction` enum).

## Key Security Features

### IDOR Protection
Each role can only access their authorized resources, tested with comprehensive IDOR (Insecure Direct Object Reference) tests.

### Deactivate and archive (no hard delete)

Users, customers, and shops are **never hard-deleted**—only deactivated via an `active` flag. Test purchases are **never hard-deleted**—Admins **archive** them (`ARCHIVED` status with status history). Attachments are retained with their test purchase.

Inactive or archived records are hidden from default lists; Admins can use list filters to show deactivated customers/shops or archived test purchases. Existing test purchases remain readable; new test purchases cannot be created for inactive customers or shops. Archived test purchases are read-only.

**GDPR Art. 17 erasure** is handled manually by the owner via anonymization of deactivated accounts—not via application delete buttons.

### Registration Security
Mystery Shoppers join by invitation only. Admins and Account Managers send one-time invite links that expire after 7 days. There is no public self-registration endpoint.

### Mystery Shopper Privacy
`SALES_MANAGER` and `CUSTOMER` roles never see mystery shopper names in views or API responses. This preserves the integrity of mystery shopping.

### Ownership Links

The system tracks ownership relationships:
- `User.customer` links CUSTOMER role users to their company
- `Customer.salesManager` links customers to their sales manager  
- `User.managedCustomers` links ACCOUNT_MANAGER to their managed customers

## Special Rules

### Mystery Shopper Assignment

**Only Admin and Account Manager** can assign mystery shoppers to test purchases.

**Why this matters:**
- Maintains confidentiality of shopper identity
- Prevents conflict of interest
- Ensures proper oversight of assignments

### Mystery Shopper Anonymity

**Sales Manager and Customer** cannot see the mystery shopper's name on test purchases.

**Implementation:**
- The shopper's name is hidden in the UI for these roles
- They see "Mystery Shopper" or "Assigned" instead of the actual name
- This preserves the integrity of the mystery shopping process

**Who can see shopper names:**
- **Admin**: Full system access requires this visibility
- **Account Manager**: Needs to manage assignments and relationships
- **Mystery Shopper**: Can see their own name and assignments

### Mystery Shopper Can Change Status

**Mystery Shoppers can change status on purchases they are allowed to view** (typically their assignments), for example moving from INITIALISED or IN_PROGRESS through product-order/receive steps toward CLOSED or CANCELLED as instructed by the account manager.

### Account Manager Scope

**Account Managers can only:**
- View test purchases for customers they manage
- Edit customers they manage
- Create new customers (automatically become managed by them)

They cannot access other Account Managers' customers or purchases.

### Sales Manager Scope

**Sales Managers can only:**
- View test purchases for customers they own
- Edit customers they own
- Create new customers (automatically become owned by them)

They cannot change status, assign shoppers, or edit purchases.

### Customer Scope

**Customers can only:**
- View test purchases for their own company
- Create test purchases for their company
- Cannot see mystery shopper names
- Cannot change status or edit purchases after creation

The `User.customer` link determines which company they belong to.

### Delete vs. deactivate vs. archive

The application **does not expose hard delete** for users, customers, shops, test purchases, or attachments.

- **Users, customers, shops:** Admin **Deactivate** / **Reactivate** (`active` flag). Rows and relationships stay in the database.
- **Test purchases:** Admin **Archive** / **Unarchive** (`ARCHIVED` status + status history). Rows, items, attachments, and history stay in the database; archived purchases are read-only.
- **Attachments:** Always kept with the test purchase (download only; no delete action).
- **GDPR Art. 17:** Formal erasure is handled manually by the owner via anonymization—not via in-app delete buttons.

### Status workflow permissions

Status values are defined in `TestPurchaseStatus` (e.g. INITIALISED, IN_PROGRESS, PRODUCT_ORDERED, PRODUCT_RECEIVED, PRODUCT_FORWARDED_TO_CLIENT, PRODUCT_SHIPPED_BACK_TO_SELLER, CLOSED, CANCELLED, ARCHIVED).

| Action | Admin | Account Manager | Sales Manager | Customer | Mystery Shopper |
|--------|:-----:|:---------------:|:-------------:|:--------:|:---------------:|
| Change status (non-archived) | ✅ | ✅ | ❌ | ❌ | ✅ (assigned purchases) |
| Archive / unarchive | ✅ | ❌ | ❌ | ❌ | ❌ |

**ARCHIVED** is set only via Admin archive/unarchive; archived purchases are read-only.

## Role Hierarchy

```
Admin
  └─ Full system access, can perform any action
  
Account Manager
  └─ Manage assigned customers
     └─ Assign mystery shoppers
     └─ Manage test purchases for managed customers
     
Sales Manager
  └─ Read-mostly oversight
     └─ View purchases for owned customers (no shopper names)
     └─ Generate reports
     
Customer
  └─ Self-service test purchases
     └─ Create and track company purchases
     └─ Cannot see shopper assignments
     
Mystery Shopper
  └─ Execute assigned work
     └─ View and update assigned purchases
     └─ Change status on own assignments
     └─ Upload results and attachments
```

## Implementation Notes

### Spring Security Configuration

Roles are implemented using Spring Security's `@PreAuthorize` annotations and role-based access control:

```java
@PreAuthorize("hasRole('ADMIN')")
public void deactivateUser(UUID id) { ... }

@PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNT_MANAGER')")
public void assignMysteryShopperToPurchase(...) { ... }

@PreAuthorize("hasRole('MYSTERY_SHOPPER')")
public void updateAssignedPurchaseStatus(...) { ... }
```

### Database-Level Security

- User roles are stored in the `role` column of the `users` table (enum)
- No user can change their own role through the application
- Only Admin can modify role assignments

### Repository Query Filtering

Repositories filter results based on the current user's role and relationships:
- Account Managers see only managed customers' data
- Sales Managers see only owned customers' data
- Customers see only their company's data
- Mystery Shoppers see only assigned purchases

### UI-Level Security

- Navigation menus and buttons are conditionally rendered based on role
- Thymeleaf security expressions: `sec:authorize="hasRole('ADMIN')"`
- Direct URL access is still blocked by Spring Security filters
- Mystery shopper names are hidden in templates for restricted roles

## Future Enhancements

See [MASTERPLAN.md](MASTERPLAN.md) for remaining roadmap items. Implemented since the original RBAC issue: TOTP 2FA, login lockout/rate limits, audit log, pagination/export, upload magic-byte validation, entity numbers, deactivation/archive-only data lifecycle, PDF purchase reports ([#11](https://github.com/mitetodb/test-purchase-application/issues/11)), deadlines ([#16](https://github.com/mitetodb/test-purchase-application/issues/16)), questionnaire and score trends ([#17](https://github.com/mitetodb/test-purchase-application/issues/17)), monitoring health components ([#18](https://github.com/mitetodb/test-purchase-application/issues/18)), visit appointments ([#24](https://github.com/mitetodb/test-purchase-application/issues/24)), invoices ([#12](https://github.com/mitetodb/test-purchase-application/issues/12)).
