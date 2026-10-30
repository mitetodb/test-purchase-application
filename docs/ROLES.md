# Roles and Permissions

This document defines the five roles in the Test Purchase Application and their permissions.

## Roles Overview

| Role | Purpose | Key Responsibilities |
|------|---------|---------------------|
| **Admin** | System administrator | Full system access, user management, hard deletes |
| **Account Manager** | Customer relationship management | Manage customers, assign mystery shoppers |
| **Sales Manager** | Sales oversight | View test purchases, generate reports (no shopper names) |
| **Customer** | Request test purchases | Create and track their own test purchases (no shopper names) |
| **Mystery Shopper** | Execute test purchases | View assigned purchases, upload results |

## Permission Matrix

### Users and Authentication

| Permission | Admin | Account Manager | Sales Manager | Customer | Mystery Shopper |
|-----------|:-----:|:---------------:|:-------------:|:--------:|:---------------:|
| View all users | ✅ | ❌ | ❌ | ❌ | ❌ |
| Create users | ✅ | ❌ | ❌ | ❌ | ❌ |
| Edit any user | ✅ | ❌ | ❌ | ❌ | ❌ |
| Deactivate user | ✅ | ❌ | ❌ | ❌ | ❌ |
| Hard delete user | ✅ | ❌ | ❌ | ❌ | ❌ |
| Edit own profile | ✅ | ✅ | ✅ | ✅ | ✅ |
| Upload avatar | ✅ | ✅ | ✅ | ✅ | ✅ |
| Change own password | ✅ | ✅ | ✅ | ✅ | ✅ |
| Reset password (any user) | ✅ | ❌ | ❌ | ❌ | ❌ |

### Customers

| Permission | Admin | Account Manager | Sales Manager | Customer | Mystery Shopper |
|-----------|:-----:|:---------------:|:-------------:|:--------:|:---------------:|
| View all customers | ✅ | ✅ | ✅ | ❌ | ❌ |
| View own customer profile | ❌ | ❌ | ❌ | ✅ | ❌ |
| Create customer | ✅ | ✅ | ❌ | ❌ | ❌ |
| Edit any customer | ✅ | ✅ | ❌ | ❌ | ❌ |
| Deactivate customer | ✅ | ✅ | ❌ | ❌ | ❌ |
| Hard delete customer | ✅ | ❌ | ❌ | ❌ | ❌ |
| Assign account manager | ✅ | ✅ | ❌ | ❌ | ❌ |

### Shops

| Permission | Admin | Account Manager | Sales Manager | Customer | Mystery Shopper |
|-----------|:-----:|:---------------:|:-------------:|:--------:|:---------------:|
| View all shops | ✅ | ✅ | ✅ | ✅ | ✅ |
| Create shop | ✅ | ✅ | ❌ | ❌ | ❌ |
| Edit any shop | ✅ | ✅ | ❌ | ❌ | ❌ |
| Deactivate shop | ✅ | ✅ | ❌ | ❌ | ❌ |
| Hard delete shop | ✅ | ❌ | ❌ | ❌ | ❌ |

### Test Purchases

| Permission | Admin | Account Manager | Sales Manager | Customer | Mystery Shopper |
|-----------|:-----:|:---------------:|:-------------:|:--------:|:---------------:|
| View all purchases | ✅ | ✅ | ✅ | ❌ | ❌ |
| View own purchases | ❌ | ❌ | ❌ | ✅ | ❌ |
| View assigned purchases | ❌ | ❌ | ❌ | ❌ | ✅ |
| **See mystery shopper name** | ✅ | ✅ | ❌ | ❌ | ✅ |
| Create purchase | ✅ | ✅ | ❌ | ✅ | ❌ |
| Edit any purchase | ✅ | ✅ | ❌ | ❌ | ❌ |
| Edit own purchase (if Customer) | ❌ | ❌ | ❌ | ✅* | ❌ |
| **Assign mystery shopper** | ✅ | ✅ | ❌ | ❌ | ❌ |
| Change status (any) | ✅ | ✅ | ❌ | ❌ | ❌ |
| Update assigned purchase | ❌ | ❌ | ❌ | ❌ | ✅* |
| Deactivate purchase | ✅ | ✅ | ❌ | ❌ | ❌ |
| Hard delete purchase | ✅ | ❌ | ❌ | ❌ | ❌ |

\* *With status restrictions*

### Items (Purchase Line Items)

| Permission | Admin | Account Manager | Sales Manager | Customer | Mystery Shopper |
|-----------|:-----:|:---------------:|:-------------:|:--------:|:---------------:|
| View items (any purchase) | ✅ | ✅ | ✅ | ❌ | ❌ |
| View items (own/assigned) | ❌ | ❌ | ❌ | ✅ | ✅ |
| Add/edit/delete items | ✅ | ✅ | ❌ | ✅* | ❌ |

\* *Customers: only on their own purchases*

### Attachments

| Permission | Admin | Account Manager | Sales Manager | Customer | Mystery Shopper |
|-----------|:-----:|:---------------:|:-------------:|:--------:|:---------------:|
| View attachments (any) | ✅ | ✅ | ✅ | ❌ | ❌ |
| View attachments (own/assigned) | ❌ | ❌ | ❌ | ✅ | ✅ |
| Upload attachment | ✅ | ✅ | ❌ | ✅ | ✅ |
| Delete attachment (any) | ✅ | ✅ | ❌ | ❌ | ❌ |
| Delete attachment (own) | ❌ | ❌ | ❌ | ✅ | ✅ |

### Status History

| Permission | Admin | Account Manager | Sales Manager | Customer | Mystery Shopper |
|-----------|:-----:|:---------------:|:-------------:|:--------:|:---------------:|
| View history (any purchase) | ✅ | ✅ | ✅ | ❌ | ❌ |
| View history (own/assigned) | ❌ | ❌ | ❌ | ✅ | ✅ |

### Reports and Analytics

| Permission | Admin | Account Manager | Sales Manager | Customer | Mystery Shopper |
|-----------|:-----:|:---------------:|:-------------:|:--------:|:---------------:|
| Generate system reports | ✅ | ✅ | ✅ | ❌ | ❌ |
| View customer analytics | ✅ | ✅ | ✅ | ❌ | ❌ |
| View own statistics | ❌ | ❌ | ❌ | ✅ | ✅ |

### System Administration

| Permission | Admin | Account Manager | Sales Manager | Customer | Mystery Shopper |
|-----------|:-----:|:---------------:|:-------------:|:--------:|:---------------:|
| Access API documentation | ✅ | ❌ | ❌ | ❌ | ❌ |
| View system health | ✅ | ❌ | ❌ | ❌ | ❌ |
| Manage system settings | ✅ | ❌ | ❌ | ❌ | ❌ |
| Access audit logs | ✅ | ❌ | ❌ | ❌ | ❌ |

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

### Delete vs. Deactivate

**Hard Delete (Physical Delete)**
- **Only Admin** can hard delete any entity
- Permanently removes data from the database
- Used sparingly, typically for data cleanup or compliance

**Deactivate (Soft Delete)**
- **Admin, Account Manager** can deactivate users, customers, shops, test purchases
- Records remain in the database with `active = false` or `status = 'INACTIVE'`
- Preferred method for "deleting" in normal operations
- Allows data recovery and maintains audit trails

**For all other roles:**
- "Delete" operations are actually deactivations
- They cannot permanently remove data
- Ensures data integrity and compliance

### Customer Editing Restrictions

Customers can edit their own test purchases **only under certain conditions**:
- Before the purchase is assigned to a mystery shopper
- Before the purchase status is finalized
- Cannot change assigned mystery shopper (they don't see this field)
- Cannot change certain status transitions

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
  └─ Customer relationship management
     └─ Assign mystery shoppers
     └─ Manage test purchases
     
Sales Manager
  └─ Read-only oversight
     └─ View purchases (no shopper names)
     └─ Generate reports
     
Customer
  └─ Self-service test purchases
     └─ Create and track own purchases
     └─ Cannot see shopper assignments
     
Mystery Shopper
  └─ Execute assigned work
     └─ View and update assigned purchases
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

@PreAuthorize("hasRole('CUSTOMER')")
public void createTestPurchase(...) { ... }
```

### Database-Level Security

- User roles are stored in the `role` column of the `users` table (enum)
- No user can change their own role through the application
- Only Admin can modify role assignments

### UI-Level Security

- Navigation menus and buttons are conditionally rendered based on role
- Thymeleaf security expressions: `sec:authorize="hasRole('ADMIN')"`
- Direct URL access is still blocked by Spring Security filters

## Future Enhancements

See [MASTERPLAN.md](MASTERPLAN.md) for planned improvements:

- [#19 RBAC Refinements](https://github.com/mitetodb/test-purchase-application/issues/19) - More granular permissions
- [#14 Audit Logging](https://github.com/mitetodb/test-purchase-application/issues/14) - Track all permission-based actions
- [#8 TOTP 2FA](https://github.com/mitetodb/test-purchase-application/issues/8) - Additional authentication for sensitive roles
