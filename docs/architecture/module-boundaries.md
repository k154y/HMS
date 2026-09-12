# Hotel Management System — Backend Module Boundaries

## 1. Purpose

The Hotel Management System backend is implemented as a modular monolith.

A modular monolith is one deployable application while maintaining clear business-domain boundaries.

The objective is to gain the operational simplicity of one application without creating tightly coupled and unmaintainable code.

## 2. Module Ownership

Each module should own its primary business rules and persistence logic.

Other modules should interact through defined services, application interfaces, or domain events rather than directly manipulating another module's internal state.

## 3. Platform Module

Responsibilities:

* Hotels
* Branches
* Hotel settings
* Subscription status
* Trial period
* Hotel activation and suspension

Other modules depend on trusted hotel and branch context.

## 4. Identity and Access Module

Responsibilities:

* Users
* Authentication
* Roles
* Permissions
* User branch access
* Token security

It determines who the current user is and what the user may do.

## 5. Customer Module

Responsibilities:

* Individual customers
* Companies
* Tour agencies
* Organizations
* Customer contact information
* Reusable customer history references

A customer represents the commercial party.

A guest represents a person staying or consuming hotel services.

These concepts must not be treated as identical.

## 6. Room Module

Responsibilities:

* Room types
* Rooms
* Bed configuration
* Capacity
* Operational room status
* Room blocks
* Availability-supporting room information

The room module does not own reservation financial settlement.

## 7. Reservation Module

Responsibilities:

* Reservation creation
* Reservation date validation
* Reservation rooms
* Reservation guests
* Availability confirmation
* Reservation status
* Cancellation
* No-show

One reservation may contain multiple rooms.

The reservation owner may be different from the staying guests.

## 8. Stay Module

Responsibilities:

* Check-in
* Active stay
* Room assignment during stay
* Room transfer
* Check-out process
* Actual arrival/departure information

Reservation describes planned accommodation.

Stay describes actual accommodation.

## 9. Folio Module

Responsibilities:

* Guest/customer account during service consumption
* Charges
* Credits
* Adjustments
* Balance
* Folio splitting
* Charge transfers

A folio should not implement payment-provider logic itself.

## 10. Order / POS Module

Responsibilities:

* Restaurant orders
* Bar orders
* Room-service orders
* Order items
* Tables where applicable
* Order preview
* Order confirmation
* Order status
* Kitchen/bar routing

Confirmed order data provides input into inventory and financial processing.

## 11. Product Module

Responsibilities:

* Sellable products
* Inventory-linked products
* Service items
* Categories
* Units
* Price configuration
* Product availability

Historical transaction prices must remain in transaction records rather than being derived later from the current product price.

## 12. Inventory Module

Responsibilities:

* Stock locations
* Stock balances
* Stock movements
* Transfers
* Counts
* Adjustments
* Waste
* Reorder information

Inventory changes should be represented through stock movements.

Other modules must not silently overwrite stock balances.

## 13. Purchasing Module

Responsibilities:

* Purchase requests
* Purchase orders
* Goods receipts
* Purchase items
* Supplier invoice relationships

Confirmed goods receipt communicates an inventory increase to the inventory domain.

## 14. Vendor Module

Responsibilities:

* Supplier identity
* Contact information
* Payment terms
* Supplier history references
* Vendor status

Vendor financial balances are coordinated with the financial/accounting domain.

## 15. Payment Module

Responsibilities:

* Payment transactions
* Payment methods
* Split payments
* Payment allocation
* Reversals
* Refund relationships

One bill may have multiple payment transactions.

Payments must be independently auditable.

## 16. Cashier Module

Responsibilities:

* Cashier shifts
* Opening float
* Collections
* Expected cash
* Counted cash
* Shift reconciliation
* Shortage and excess

Payment transactions reference the appropriate cashier context when required.

## 17. Credit / Receivables Module

Responsibilities:

* Credit accounts
* Credit limits
* Payment terms
* Customer outstanding balances
* Due dates
* Overdue status

Credit approval must interact with the authorization/approval system.

## 18. Accounting Module

Responsibilities:

* Financial classifications
* Revenue recognition inputs
* Receivables
* Payables
* Expense records
* Accounting-oriented reporting

Operational modules provide accounting events or records.

Accounting should not bypass operational transaction history.

## 19. Housekeeping Module

Responsibilities:

* Room cleaning tasks
* Cleaning state
* Inspections
* Lost and found

Cleaning status must remain distinct from future reservation availability.

## 20. Maintenance Module

Responsibilities:

* Maintenance requests
* Maintenance tasks
* Maintenance costs
* Room/service impact

Maintenance may cause a room to become blocked or out of service.

## 21. Notification Module

Responsibilities:

* In-application notifications
* Email dispatch coordination
* Future SMS
* Future WhatsApp integration
* Notification preferences

RabbitMQ may be used for asynchronous delivery.

A failed non-critical notification must not corrupt the original business transaction.

## 22. Audit Module

Responsibilities:

* Sensitive action records
* Approval history
* Before/after values where appropriate
* Actor information
* Correlation identifiers

Audit data must not be modifiable by ordinary operational users.

## 23. Reporting Module

Responsibilities:

* Operational reporting
* Financial reporting views
* Date-range reporting
* Dashboard aggregation

Reporting should read authoritative data from business modules.

It must not silently create independent competing transaction records.

## 24. File Module

Responsibilities:

* File metadata
* Storage provider abstraction
* Hotel ownership
* Access control
* Object-storage integration

The initial cloud implementation may use Cloudflare R2.

The design should remain compatible with S3-compatible storage.

## 25. Cross-Module Communication

Preferred patterns:

1. Direct application service call when immediate consistency is required.
2. Database transaction coordination for operations that must commit together.
3. Domain/application event for decoupled secondary processing.
4. RabbitMQ for asynchronous processing when eventual completion is acceptable.

## 26. Financial and Stock Consistency

Critical consistency must not depend only on an asynchronous message.

Examples requiring careful transactional handling:

* Payment posting
* Stock receipt
* Reservation confirmation
* Folio charge creation
* Stock deduction from confirmed sale

RabbitMQ is useful for follow-up activities but should not be the only mechanism protecting core financial correctness.

## 27. Module Independence Rule

A module must not directly modify tables belonging to another module merely because the database is shared.

Shared PostgreSQL deployment does not mean shared ownership of every table.

This principle keeps later extraction into separate services possible if scale eventually requires it.
