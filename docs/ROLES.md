# Roles and Permissions

This document defines the five roles in the Test Purchase Application and their permissions, as enforced by server-side access control (see PR #21).

## Roles Overview

| Role | Purpose | Key Responsibilities |
|------|---------|---------------------|
| **Admin** | System administrator | Full system access, user management, can hard delete entities |
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
| Delete test purchase | ✅ Hard delete | ⚠️ Soft delete | ⚠️ Soft delete | ⚠️ Soft delete | ⚠️ Soft delete |
| **Change status** | ✅ | ✅ | ❌ | ❌ | ✅ **Own assignments** |
| **Assign mystery shopper** | ✅ | ✅ | ❌ | ❌ | ❌ |
| **See mystery shopper name** | ✅ | ✅ | ❌ | ❌ | ✅ (own) |

### Customers

| Permission | Admin | Account Manager | Sales Manager | Customer | Mystery Shopper |
|-----------|:-----:|:---------------:|:-------------:|:--------:|:---------------:|
| View customers | ✅ All | ✅ Managed only | ✅ Owned only | ❌ | ❌ |
| Create customer | ✅ | ✅ (auto-managed) | ✅ (auto-owned) | ❌ | ❌ |
| Edit customer | ✅ | ✅ **Managed only** | ✅ **Owned only** | ❌ | ❌ |
| Delete customer | ✅ Hard delete | ⚠️ Soft delete | ⚠️ Soft delete | ❌ | ❌ |
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
| View all shops | ✅ | ✅ | ✅ | ✅ | ✅ |
| Create shop | ✅ | ✅ | ❌ | ❌ | ❌ |
| Edit any shop | ✅ | ✅ | ❌ | ❌ | ❌ |
| Deactivate shop | ✅ | ✅ | ❌ | ❌ | ❌ |
| Hard delete shop | ✅ | ❌ | ❌ | ❌ | ❌ |

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
| Upload attachment | ✅ | ✅ | ❌ | ✅ | ✅ |
| Delete attachment (any) | ✅ | ✅ | ❌ | ❌ | ❌ |
| Delete attachment (own) | ✅ | ✅ | ❌ | ✅ | ✅ |

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

### System Administration

| Permission | Admin | Account Manager | Sales Manager | Customer | Mystery Shopper |
|-----------|:-----:|:---------------:|:-------------:|:--------:|:---------------:|
| Access API documentation | ✅ | ❌ | ❌ | ❌ | ❌ |
| View system health | ✅ | ❌ | ❌ | ❌ | ❌ |
| Manage system settings | ✅ | ❌ | ❌ | ❌ | ❌ |
| Access audit logs | ✅ | ❌ | ❌ | ❌ | ❌ |

## Key Security Features

### IDOR Protection
Each role can only access their authorized resources, tested with comprehensive IDOR (Insecure Direct Object Reference) tests.

### Soft Delete
Non-admin roles deactivate records via `active` flags; inactive records are filtered from queries. Only Admin can hard delete (permanently remove from database).

**Users are never deleted**, only deactivated. Even Admins cannot delete user records—they can only set `active = false`.

### Registration Security
Mystery Shoppers join by invitation only. Admins and Account Managers send one-time invite links that expire. There is no public self-registration endpoint.

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

**Mystery Shoppers can change status on their own assignments**, typically:
- ASSIGNED → IN_PROGRESS (when starting work)
- IN_PROGRESS → COMPLETED (when finishing work)

This allows them to update progress without Account Manager intervention.

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

### Delete vs. Deactivate

**Hard Delete (Physical Delete)**
- **Only Admin** can hard delete entities (customers, test purchases, shops)
- Permanently removes data from the database
- **Users are never deleted**—only deactivated (`active = false`)
- Used sparingly for data cleanup

**Soft Delete (Deactivate)**
- **Admin, Account Manager, Sales Manager** can soft delete records they can access
- Records remain in database with `active = false`
- Preferred method for "deleting" in normal operations
- Allows data recovery and maintains audit trails

**For other roles:**
- They cannot delete at all, or soft deletes are scoped to their access

### Status Workflow Permissions

Status changes follow a workflow, and permissions vary:

| Status Transition | Admin | Account Manager | Sales Manager | Customer | Mystery Shopper |
|------------------|:-----:|:---------------:|:-------------:|:--------:|:---------------:|
| Create (DRAFT) | ✅ | ✅ | ❌ | ✅ | ❌ |
| DRAFT → SUBMITTED | ✅ | ✅ | ❌ | ✅ | ❌ |
| SUBMITTED → ASSIGNED | ✅ | ✅ | ❌ | ❌ | ❌ |
| ASSIGNED → IN_PROGRESS | ✅ | ✅ | ❌ | ❌ | ✅ |
| IN_PROGRESS → COMPLETED | ✅ | ✅ | ❌ | ❌ | ✅ |
| COMPLETED → REVIEWED | ✅ | ✅ | ❌ | ❌ | ❌ |
| Any → CANCELLED | ✅ | ✅ | ❌ | ❌ | ❌ |

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
public void deleteUser(UUID id) { ... }

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

See [MASTERPLAN.md](MASTERPLAN.md) for planned improvements:

- [#19 Role-based access control enforced on the server](https://github.com/mitetodb/test-purchase-application/issues/19) - More granular permissions
- [#13 Full audit log for clients, prices and users](https://github.com/mitetodb/test-purchase-application/issues/13) - Track all permission-based actions
- [#8 Password policy (min 12) and TOTP 2FA for Admin and Account Manager](https://github.com/mitetodb/test-purchase-application/issues/8) - Additional authentication for sensitive roles
