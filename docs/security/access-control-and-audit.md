# Hotel Management System — Access Control and Audit

## 1. Security Principle

Every employee must have an individual user account.

Shared operational accounts are not permitted.

Every sensitive action must be attributable to one authenticated user.

## 2. Authentication

The backend will use:

* Spring Security
* JWT-based API authentication

Authentication and authorization are separate concerns.

Authentication answers:

Who is the user?

Authorization answers:

What is the user permitted to do?

## 3. Roles

Initial roles may include:

* System Super Admin
* Hotel Owner
* Hotel Manager
* Accountant
* Receptionist
* Cashier
* Waiter
* Bartender
* Kitchen Staff
* Storekeeper
* Housekeeper
* Maintenance Staff
* Auditor
* Supervisor

Roles must not be used as a substitute for individual permission checks where finer control is necessary.

## 4. Permissions

Examples of permissions include:

* Reservation create
* Reservation cancel
* Guest check-in
* Guest check-out
* Payment receive
* Refund request
* Refund approve
* Discount request
* Discount approve
* Stock receive
* Stock issue
* Stock adjust
* Stock adjustment approve
* Purchase create
* Purchase approve
* Credit approve
* Financial report view

## 5. Tenant Security

Every request accessing tenant-owned data must be restricted to the authenticated user's hotel.

Users must not be able to change a request field such as `hotel_id` to access another hotel's information.

Tenant context must be derived from trusted authentication/authorization context.

## 6. Branch Security

Users may have access to:

* One branch
* Multiple selected branches
* All branches of a hotel

Branch authorization must be enforced by the backend.

## 7. Sensitive Operations

Sensitive actions include:

* Refund
* Void
* Cancellation
* Discount
* Credit approval
* Stock adjustment
* Complimentary service
* Room rate override
* Checkout with outstanding balance
* Permission change
* User activation/deactivation

## 8. Approval Control

Approval-required operations must preserve:

* Requested by
* Requested at
* Reason
* Approved or rejected by
* Decision time
* Decision notes

Self-approval should be prevented when segregation of duties requires an independent approver.

## 9. Audit Trail

Audit records must capture important changes.

Typical audit information:

* Hotel
* Branch
* User
* Action
* Entity type
* Entity ID
* Date and time
* Previous value
* New value
* Reason
* Request or correlation identifier
* Device/network context where appropriate

## 10. Financial Records

Financial transactions must never disappear silently.

Corrections must use:

* Void
* Reversal
* Refund
* Adjustment

The system must retain the relationship between the original and corrective transactions.

## 11. Inventory Audit

Stock actions that require auditing include:

* Goods receipt
* Stock issue
* Transfer
* Sale consumption
* Recipe consumption
* Waste
* Adjustment
* Count variance

Each movement must identify its source transaction where applicable.

## 12. Password Security

Passwords must:

* Never be stored in plain text
* Use an approved password hashing algorithm
* Never appear in logs
* Never be returned by APIs

## 13. Tokens

JWT secrets and signing material must not be committed to Git.

Production secrets must be delivered through secure environment configuration or secret-management tooling.

Access tokens should be short-lived.

Refresh-token handling must support revocation.

## 14. Logging

Logs must not contain:

* Passwords
* JWT secrets
* Full payment card data
* Sensitive authentication credentials

Operational logs should contain enough context to investigate failures without exposing secrets.

## 15. Auditors

Auditor access should normally be read-only.

Auditors may view authorized:

* Financial transactions
* Payments
* Stock movements
* Approval history
* Audit trail
* Reports

Auditors should not modify operational records unless explicitly authorized.

## 16. Security Enforcement

Security rules must be enforced primarily in the backend.

Frontend restrictions improve user experience but are not security boundaries.
