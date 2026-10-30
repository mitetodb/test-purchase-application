# Account Manager User Guide

Welcome to the Test Purchase Application! This guide is for **Account Managers** who manage customer relationships and test purchase assignments.

## Table of Contents

- [Getting Started](#getting-started)
- [Your Role](#your-role)
- [Dashboard](#dashboard)
- [Customer Management](#customer-management)
- [Test Purchase Management](#test-purchase-management)
- [Assigning Mystery Shoppers](#assigning-mystery-shoppers)
- [Shop Management](#shop-management)
- [Reports and Analytics](#reports-and-analytics)
- [Best Practices](#best-practices)

## Getting Started

### Logging In

1. Navigate to https://testkauf.getinweb.de
2. Enter your username and password
3. Click **Login**

### Your Profile

1. Click your username in the top right corner
2. Select **Profile**
3. Update your information:
   - Email
   - Country
   - Language preference
   - Avatar image
4. Change your password if needed
5. Click **Save**

### Changing Language

Click the globe icon in the navigation and select:
- **English** (en)
- **Bulgarian** (bg)
- **German** (de)

## Your Role

As an **Account Manager**, you:

✅ **Can do:**
- View and manage customers assigned to you
- Assign yourself or others as account managers to customers
- Create and manage test purchases for your customers
- **Assign mystery shoppers** to test purchases
- **See mystery shopper names** (unlike Sales Managers and Customers)
- Change test purchase status
- View all shops
- Create and edit shops
- Generate reports
- View status history and attachments

❌ **Cannot do:**
- Create or manage users (Admin only)
- Hard delete data (only deactivate)
- Access system administration features
- Change system settings

See the complete permission matrix in [ROLES.md](../../ROLES.md).

## Dashboard

Your dashboard shows:
- **Your Customers**: Customers assigned to you
- **Active Test Purchases**: Ongoing purchases you manage
- **Pending Assignments**: Purchases awaiting mystery shopper assignment
- **Recent Activity**: Latest status changes
- **Quick Stats**: Purchase counts by status

## Customer Management

### Viewing Customers

1. Click **Customers** in the navigation
2. See customers assigned to you (and others for coordination)
3. Search by name, email, or VAT number
4. Filter by account manager or status

### Creating a Customer

1. Go to **Customers** → **Create Customer**
2. Fill in the details:
   - Name (required)
   - Email
   - Phone
   - VAT number
   - Address
   - Country
3. Assign an Account Manager:
   - Select yourself
   - Or select another Account Manager
4. Click **Create**

### Editing a Customer

1. Find the customer → Click **Edit**
2. Update any field
3. Change the assigned Account Manager if needed
4. Click **Save**

**Note:** You can only edit customers you manage, unless you're reassigning them.

### Managing Your Customer Portfolio

**Best practice:** Focus on customers assigned to you. You can see other customers for coordination but can only edit your managed customers.

**To see your customers:**
1. Go to **Customers**
2. Filter by **Account Manager** → Select your name
3. These are your primary responsibility

### Deactivating a Customer

When a customer is no longer active:
1. Edit the customer
2. Uncheck **Active**
3. Click **Save**

The customer remains in the database but is marked inactive.

**Note:** You cannot permanently delete customers (Admin only).

## Test Purchase Management

Test purchases are mystery shopping orders you create and manage for your customers.

### Viewing Test Purchases

1. Click **Test Purchases** in the navigation
2. See all purchases (you have full access)
3. **You can see mystery shopper names** (important for assignment management)
4. Filter by:
   - Status (Draft, Submitted, Assigned, etc.)
   - Customer
   - Mystery Shopper
   - Date range

### Creating a Test Purchase

1. Go to **Test Purchases** → **Create**
2. Select the **Customer** (usually one of yours)
3. Select the **Shop** location
4. Add a **Description** of what needs to be evaluated
5. Set a **Due Date**
6. Add **Items** (products/services to evaluate):
   - Click **Add Item**
   - Enter item name, quantity, price
   - Repeat for all items
7. Click **Create**

The purchase is created in **DRAFT** status.

### Test Purchase Status Workflow

```
DRAFT → SUBMITTED → ASSIGNED → IN_PROGRESS → COMPLETED → REVIEWED
                        ↓
                   CANCELLED (any time)
```

**Your actions:**
- **DRAFT → SUBMITTED**: When purchase details are finalized
- **SUBMITTED → ASSIGNED**: When you assign a mystery shopper
- **ASSIGNED → CANCELLED**: If the purchase needs to be cancelled
- **COMPLETED → REVIEWED**: After you review the shopper's results

**Mystery Shopper actions:**
- **ASSIGNED → IN_PROGRESS**: When they start working
- **IN_PROGRESS → COMPLETED**: When they finish

### Changing Purchase Status

1. Open the test purchase
2. Click **Change Status**
3. Select the new status
4. Add a comment (optional but recommended)
5. Enable **Email Notification** to notify relevant parties
6. Click **Save**

Status history is tracked automatically.

## Assigning Mystery Shoppers

**This is a key responsibility!** Only Account Managers and Admins can assign mystery shoppers.

### Step-by-Step Assignment

1. Open a test purchase (should be in **SUBMITTED** status)
2. Click **Edit**
3. In the **Mystery Shopper** dropdown, select a shopper
4. Change status to **ASSIGNED**
5. Optional: Enable email notification to alert the shopper
6. Click **Save**

### Choosing the Right Shopper

Consider:
- **Availability**: Is the shopper available for the due date?
- **Experience**: Do they have relevant experience?
- **Location**: Are they near the shop location?
- **Workload**: Do they already have many assignments?
- **Language**: Do they speak the required language?

### Reassigning a Shopper

If you need to change the assigned shopper:
1. Edit the purchase
2. Select a different mystery shopper
3. Add a comment explaining the change
4. Enable notification to alert both shoppers
5. Click **Save**

### Important: Shopper Anonymity

- **Sales Managers** and **Customers** cannot see shopper names
- They see "Mystery Shopper" or "Assigned" instead
- **You** can see names because you manage assignments
- **Admins** can also see names
- This maintains the integrity of mystery shopping

## Shop Management

You can create and manage shop locations.

### Viewing Shops

1. Click **Shops** in the navigation
2. See all shops
3. Search by name, city, or country

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

1. Find the shop → Click **Edit**
2. Update any field
3. Click **Save**

### Deactivating a Shop

1. Edit the shop
2. Uncheck **Active**
3. Click **Save**

## Reports and Analytics

### Purchase Reports

1. Go to **Reports** in the navigation
2. Select date range
3. Filter by:
   - Customer
   - Mystery Shopper
   - Status
   - Shop
4. View statistics:
   - Total purchases
   - Average completion time
   - Status distribution
   - Top shoppers
5. Export to CSV or PDF (if available)

### Customer Performance

Track customer activity:
- Number of purchases
- Completion rates
- Average purchase value
- Trends over time

### Mystery Shopper Performance

Evaluate shopper effectiveness:
- Number of assignments
- Completion rate
- On-time delivery
- Quality ratings (if implemented)

## Attachments and Files

### Viewing Attachments

1. Open a test purchase
2. Scroll to **Attachments** section
3. View uploaded files:
   - Receipts (from shoppers)
   - Photos (evidence from shop visit)
   - Reports (detailed findings)
4. Download any attachment

### Uploading Attachments

1. Open a test purchase
2. Click **Upload Attachment**
3. Select a file (PDF or image)
4. Add a description
5. Click **Upload**

### Deleting Attachments

1. Find the attachment
2. Click **Delete**
3. Confirm

You can delete any attachment (not just your own).

## Status History

Every status change is tracked automatically.

### Viewing History

1. Open a test purchase
2. Scroll to **Status History** section
3. See:
   - Who changed the status
   - When it was changed
   - From what status to what status
   - Any comments added

This audit trail is important for:
- Accountability
- Tracking delays
- Understanding workflow
- Resolving disputes

## Email Notifications

When changing status, you can notify relevant parties:

- **Customer**: Notified of major status changes
- **Mystery Shopper**: Notified of assignments and status changes
- **Account Manager**: That's you! You'll be notified of updates

**To enable notifications:**
1. When changing status, check **Send Email Notification**
2. An email is sent to relevant parties

**Note:** Email must be enabled in system configuration.

## Multi-Language Support

The application supports three languages. Set your preference in your profile, or click the globe icon to switch anytime.

**Languages:**
- **English** (en)
- **Bulgarian** (bg)
- **German** (de)

Messages, labels, and emails will be in the selected language.

## Best Practices

### Customer Relationship Management

1. **Stay Organized**
   - Keep customer information up to date
   - Respond promptly to customer requests
   - Track your customer portfolio

2. **Communication**
   - Set clear expectations for test purchases
   - Notify customers of status changes
   - Be available for questions

3. **Quality Control**
   - Review completed purchases thoroughly
   - Provide feedback to mystery shoppers
   - Ensure accurate reporting

### Mystery Shopper Management

1. **Fair Assignment**
   - Distribute work evenly among shoppers
   - Match shoppers to appropriate tasks
   - Consider workload and availability

2. **Clear Instructions**
   - Provide detailed purchase descriptions
   - Set realistic due dates
   - Clarify expectations

3. **Follow-Up**
   - Monitor progress (ASSIGNED → IN_PROGRESS → COMPLETED)
   - Check in if deadlines are approaching
   - Review results promptly

### Workflow Efficiency

1. **Regular Reviews**
   - Check pending assignments daily
   - Review completed purchases weekly
   - Update purchase status promptly

2. **Proactive Management**
   - Anticipate customer needs
   - Plan assignments in advance
   - Address issues early

3. **Documentation**
   - Add comments to status changes
   - Keep notes on special requirements
   - Document shopper performance

## Troubleshooting

### Can't Assign a Mystery Shopper

- Verify the user has **Mystery Shopper** role
- Check that they are **Active**
- Ensure you're an Account Manager or Admin

### Email Notifications Not Sending

- Check system email configuration (Admin responsibility)
- Verify recipient email addresses are correct
- Check spam folders

### Can't See a Customer's Purchases

- Verify the customer is correct
- Check if purchases exist
- Use filters to search

### Status Change Not Allowed

- Check the current status
- Verify status workflow (can't skip steps)
- Ensure you have permission (Account Manager or Admin)

## Keyboard Shortcuts

- `Alt + H`: Home/Dashboard
- `Alt + C`: Customers
- `Alt + T`: Test Purchases
- `Alt + S`: Shops
- `Alt + R`: Reports
- `Alt + P`: Profile

## Support

For questions or issues:
- **User Guide**: This document
- **Permissions**: See [ROLES.md](../../ROLES.md)
- **System Issues**: Contact your Admin
- **Security**: See [SECURITY.md](../../SECURITY.md)

Thank you for managing our customer relationships and mystery shopping operations!
