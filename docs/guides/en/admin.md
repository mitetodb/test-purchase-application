# Admin User Guide

Welcome to the Test Purchase Application! This guide is for **Administrators** who have full system access.

## Table of Contents

- [Getting Started](#getting-started)
- [Dashboard](#dashboard)
- [User Management](#user-management)
- [Customer Management](#customer-management)
- [Shop Management](#shop-management)
- [Test Purchase Management](#test-purchase-management)
- [System Administration](#system-administration)
- [Security Best Practices](#security-best-practices)

## Getting Started

### Logging In

1. Navigate to https://testkauf.getinweb.de
2. Enter your username: `admin`
3. Enter your password (set during deployment)
4. Click **Login**

**Important:** Change your password immediately after first login!

### Changing Your Password

1. Click your username in the top right corner
2. Select **Profile**
3. Click **Change Password**
4. Enter your current password
5. Enter your new password (use a strong password!)
6. Confirm your new password
7. Click **Save**

### Your Profile

Update your profile information:
1. Click your username → **Profile**
2. Update your email, country, or language
3. Upload an avatar image (optional)
4. Click **Save**

## Dashboard

After logging in, you'll see the main dashboard with:

- **Statistics**: Total users, customers, shops, test purchases
- **Recent Activity**: Latest test purchases and status changes
- **Quick Actions**: Common tasks you can perform
- **Notifications**: Important system alerts

## User Management

As an Admin, you have full control over all users in the system.

### Viewing Users

1. Click **Users** in the navigation menu
2. See the list of all users with their roles and status
3. Use the search bar to find specific users
4. Filter by role or status

### Creating a New User

1. Go to **Users** → **Create User**
2. Fill in the required fields:
   - Username (unique)
   - Email (unique)
   - Password (strong password required)
   - Role: Admin, Account Manager, Sales Manager, Customer, or Mystery Shopper
   - Country
   - Language preference
3. Click **Create**

**Roles:**
- **Admin**: Full system access (like you!)
- **Account Manager**: Manage assigned customers and test purchases, assign mystery shoppers
- **Sales Manager**: View owned customers' purchases and generate reports (cannot see shopper names)
- **Customer**: Create and track their company's test purchases
- **Mystery Shopper**: Execute assigned test purchases

**Inviting Mystery Shoppers:**
Mystery Shoppers cannot self-register. Admins and Account Managers invite them via one-time invite links that expire. Only Admins can create users with Admin, Account Manager, Sales Manager, or Customer roles.

### Editing a User

1. Go to **Users** → Find the user → Click **Edit**
2. Update any field except username
3. You can change their role
4. Click **Save**

### Deactivating a User

Deactivated users cannot log in but their data is preserved.

1. Go to **Users** → Find the user → Click **Edit**
2. Uncheck **Active**
3. Click **Save**

To reactivate: Check **Active** and save.

**Note:** Users are never permanently deleted—only deactivated. This preserves audit trails and data integrity.

### Inviting Mystery Shoppers

Mystery Shoppers join by invitation only:

1. Go to **Users** → **Invite Mystery Shopper**
2. Enter the shopper's email address
3. Click **Send Invitation**
4. A one-time invite link is emailed to them
5. The link expires after a set time (typically 48 hours)
6. The shopper clicks the link to set up their account

Only Admins and Account Managers can send mystery shopper invitations.

### Resetting a User's Password

1. Go to **Users** → Find the user → Click **Edit**
2. Click **Reset Password**
3. Enter a new temporary password
4. Click **Save**
5. Notify the user to change their password on next login

## Customer Management

Customers are organizations or individuals who request test purchases.

### Viewing Customers

1. Click **Customers** in the navigation menu
2. View all customers with their details
3. Search by name, email, or VAT number

### Creating a Customer

1. Go to **Customers** → **Create Customer**
2. Fill in the details:
   - Name (required)
   - Email
   - Phone
   - VAT number
   - Address
   - Country
3. Assign an Account Manager (optional)
4. Click **Create**

### Editing a Customer

1. Go to **Customers** → Find customer → Click **Edit**
2. Update any field
3. Change the assigned Account Manager if needed
4. Click **Save**

### Assigning Account Managers

Account Managers are responsible for specific customers:

1. Edit the customer
2. Select an Account Manager from the dropdown
3. Click **Save**

Account Managers can now manage test purchases for this customer.

### Deactivating/Deleting Customers

- **Deactivate**: Uncheck **Active** when editing (preferred method)
- **Delete (permanent)**: Click **Delete** button and confirm (removes customer record permanently)

**Note:** Only customers, shops, and test purchases can be hard deleted. Users are never deleted—only deactivated.

## Shop Management

Shops are the locations where mystery shopping occurs.

### Viewing Shops

1. Click **Shops** in the navigation menu
2. View all shops with their locations

### Creating a Shop

1. Go to **Shops** → **Create Shop**
2. Fill in:
   - Name (required)
   - Address
   - City
   - Country
   - Additional notes
3. Click **Create**

### Editing a Shop

1. Go to **Shops** → Find shop → Click **Edit**
2. Update fields as needed
3. Click **Save**

### Deactivating/Deleting Shops

- **Deactivate**: Uncheck **Active** (preferred method)
- **Delete**: Click **Delete** (permanently removes shop record)

## Test Purchase Management

Test purchases are mystery shopping orders assigned to mystery shoppers.

### Viewing Test Purchases

1. Click **Test Purchases** in the navigation
2. See all purchases with their status
3. Filter by status, customer, or date
4. **You can see mystery shopper names** (unlike Sales Managers or Customers)

### Creating a Test Purchase

1. Go to **Test Purchases** → **Create**
2. Fill in basic information:
   - Customer (select from dropdown)
   - Shop
   - Description
   - Due date
3. Add items (products/services to evaluate):
   - Item name
   - Quantity
   - Price per unit
4. Click **Create**

The purchase is created in **DRAFT** status.

### Assigning a Mystery Shopper

**Only Admins and Account Managers can assign shoppers.**

1. Open the test purchase → Click **Edit**
2. Select a Mystery Shopper from the dropdown
3. Change status to **ASSIGNED**
4. Click **Save**
5. Optional: Enable email notification to notify the shopper

### Test Purchase Status Workflow

```
DRAFT → SUBMITTED → ASSIGNED → IN_PROGRESS → COMPLETED → REVIEWED
                        ↓
                   CANCELLED (any time)
```

**Status meanings:**
- **DRAFT**: Being created, not yet ready
- **SUBMITTED**: Ready for assignment
- **ASSIGNED**: Assigned to a mystery shopper
- **IN_PROGRESS**: Shopper is working on it
- **COMPLETED**: Shopper has finished
- **REVIEWED**: Admin/Account Manager has reviewed results
- **CANCELLED**: Purchase was cancelled

### Changing Purchase Status

1. Open the test purchase
2. Click **Change Status**
3. Select new status
4. Add a comment (optional)
5. Enable email notification (optional)
6. Click **Save**

### Viewing Purchase Details

1. Open any test purchase to see:
   - Customer and shop information
   - Mystery shopper (name visible to you)
   - Items and calculated prices
   - Attachments (photos, receipts, reports)
   - Status history (who changed what and when)

### Attachments

Mystery shoppers and customers can upload files:

1. Open the purchase
2. Scroll to **Attachments** section
3. View or download any attachment
4. Delete attachments if needed

Allowed file types: PDF, images (JPG, PNG)

### Pricing Calculation

Prices are calculated automatically via the pricing microservice:
- Service fee based on purchase value
- Postage costs
- Total amount

These are computed when items are added or updated.

## System Administration

### API Documentation

1. Navigate to: https://testkauf.getinweb.de/swagger-ui.html
2. Explore all available API endpoints
3. Test endpoints directly from the Swagger UI

**Note:** API docs are only visible to Admins.

### System Health

Check application health:
1. Navigate to: https://testkauf.getinweb.de/actuator/health
2. Verify all services are "UP"

### Viewing Logs

Logs are available on the server:
```bash
# SSH to the server as root
ssh root@testkauf.getinweb.de

# View application logs
cd /opt/testkauf/test-purchase-application
docker compose -f deploy/docker-compose.prod.yml logs -f app
```

### Database Backups

Backups run automatically every night at 3:15 AM:
- Location: `/root/testkauf-backups/`
- Retention: 14 days
- Format: Gzipped SQL dumps

To manually trigger a backup:
```bash
/opt/testkauf/test-purchase-application/deploy/backup-mysql.sh
```

### System Settings

Environment configuration is in `deploy/.env` on the server:
- Database credentials
- Email settings
- Feature flags (demo data, Swagger)
- API keys

**Important:** Never commit `.env` to Git!

## Security Best Practices

### Password Policy

- Use strong passwords (12+ characters)
- Mix uppercase, lowercase, numbers, symbols
- Don't reuse passwords
- Use a password manager

### Account Management

- Deactivate users when they leave
- Review user roles regularly
- Limit Admin role to necessary personnel
- Change default passwords immediately

### Monitoring

- Review access logs periodically
- Watch for suspicious activity
- Monitor failed login attempts
- Check for unusual data changes

### Data Protection

- Hard-delete customers, shops, or purchases only when absolutely necessary
- **Users are never deleted**—only deactivated
- Use deactivation (soft delete) by default for all entities
- Keep backups secure
- Limit access to sensitive data

### Two-Factor Authentication (Planned)

Coming soon: TOTP-based 2FA for Admin and Account Manager roles.
See [SECURITY.md](../../SECURITY.md) for details.

## Multi-Language Support

The application supports three languages:
- **Bulgarian** (bg)
- **English** (en)
- **German** (de)

### Changing Language

1. Click the globe icon in the navigation bar
2. Select your preferred language
3. The page will reload in that language

Or add `?lang=bg` to any URL.

## Troubleshooting

### Can't Log In

- Verify username and password
- Check Caps Lock
- Try password reset (if email is configured)
- Contact system administrator

### User Can't Access Features

- Check their role and permissions (see [ROLES.md](../../ROLES.md))
- Verify their account is active
- Ensure they're logged in

### Email Not Sending

- Check email configuration in `deploy/.env`
- Verify SMTP credentials
- Check application logs for errors
- Email might be disabled (`MAIL_ENABLED=false`)

### File Upload Fails

- Check file size (large files may fail)
- Verify file type (only PDF and images)
- Check disk space on server
- Review application logs

### System Performance Issues

- Check server resources (CPU, memory, disk)
- Review database performance
- Check for long-running queries
- Monitor Docker container health

## Support

For issues or questions:
- **Technical issues**: Check [DEPLOYMENT.md](../DEPLOYMENT.md)
- **Security concerns**: See [SECURITY.md](../SECURITY.md)
- **Development**: See [CONTRIBUTING.md](../CONTRIBUTING.md)
- **GitHub Issues**: [test-purchase-application/issues](https://github.com/mitetodb/test-purchase-application/issues)

## Keyboard Shortcuts

- `Alt + H`: Home/Dashboard
- `Alt + U`: Users
- `Alt + C`: Customers
- `Alt + S`: Shops
- `Alt + T`: Test Purchases
- `Alt + P`: Profile

## Best Practices

1. **Regular Maintenance**
   - Review user accounts monthly
   - Check backup logs weekly
   - Monitor system health daily
   - Update system regularly

2. **Data Management**
   - Keep customer data up to date
   - Archive old test purchases
   - Clean up inactive users
   - Maintain shop information

3. **Security**
   - Change passwords regularly
   - Review access logs
   - Limit Admin role
   - Enable 2FA when available

4. **Communication**
   - Notify users of system changes
   - Document configuration changes
   - Keep contact information current
   - Respond to user issues promptly

Thank you for maintaining the Test Purchase Application!
