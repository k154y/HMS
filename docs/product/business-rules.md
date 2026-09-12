# Hotel Management System — Business Rules

## 1. Purpose

This document defines the mandatory business rules of the Hotel Management System.

These rules apply to backend services, database transactions, APIs, dashboards, reports, and frontend workflows.

Business logic must not be implemented only in the frontend. Critical rules must always be enforced by the backend.

## 2. Hotel Data Ownership

Every operational record must belong to a hotel.

Branch-specific records must also belong to a branch where applicable.

A hotel must never access data belonging to another hotel.

Multi-tenant isolation must apply to:

* Users
* Customers
* Companies
* Guests
* Rooms
* Reservations
* Stays
* Orders
* Folios
* Payments
* Inventory
* Vendors
* Purchases
* Accounting records
* Reports
* Audit records

## 3. Reservation Rules

### 3.1 Reservation Search Order

Reservation creation must begin with:

1. Arrival date
2. Departure date
3. Number of adults
4. Number of children
5. Required room capacity or room type
6. Available room search
7. Room selection
8. Customer information
9. Guest information
10. Reservation preview
11. Final confirmation

Rooms that are not available for the complete requested period must not be offered as available.

### 3.2 Date Rules

The departure date must be after the arrival date.

A reservation must have at least one room.

The backend must verify availability again during final confirmation.

Frontend availability results alone must never be considered sufficient to prevent overbooking.

### 3.3 Multiple Rooms

One reservation may contain:

* One room
* Multiple rooms

A reservation therefore must not be designed as one reservation equal to one room.

### 3.4 Reservation Customer

A reservation may be made by:

* Individual person
* Company
* Tour agency
* Organization
* Other approved corporate customer

The reservation owner and the people staying in the rooms are not necessarily the same people.

### 3.5 Reservation Guests

One reservation may contain multiple guests.

Different rooms inside the same reservation may contain different guests.

Adults and children must be recorded separately.

## 4. Room Rules

Every room must belong to one branch.

Room information may include:

* Room number
* Room type
* Floor
* Number of beds
* Bed type
* Bed size
* Maximum adults
* Maximum children
* Standard rate
* Current operational status

Bed type and bed size are separate values.

Example:

* Bed type: King
* Bed size: 180 × 200 cm

## 5. Room Availability Rules

Availability must consider:

* Confirmed reservations
* Checked-in stays
* Rooms blocked by management
* Rooms under maintenance
* Rooms out of service

A room must not be double-booked for overlapping stay dates.

Room cleaning status and booking availability must remain separate concepts.

For example, a room may be available for tomorrow even if it is dirty today.

## 6. Customer Rules

The system must maintain reusable customer records.

Supported customer categories include:

* Individual
* Company
* Tour agency
* NGO
* Government institution
* Regular customer

A returning customer should reuse the existing customer record where safely identifiable instead of creating unnecessary duplicates.

Customer history may contain:

* Reservations
* Stays
* Orders
* Folios
* Invoices
* Payments
* Credit transactions
* Outstanding balances

## 7. Guest Folio Rules

A checked-in guest must have an active folio or be linked to an appropriate folio.

A folio may contain:

* Accommodation charges
* Food
* Beverages
* Room service
* Laundry
* Transport
* Minibar
* Damage charges
* Other services

A folio must track:

* Charges
* Payments
* Credits
* Adjustments
* Remaining balance

## 8. Order Rules

An order may originate from:

* Restaurant
* Bar
* Room service
* Takeaway
* Event
* Other hotel service

Items added to an order must first display:

* Quantity
* Unit price
* Line total

The user must confirm the addition.

Recently added items should appear prominently in the interface.

Before an order is finally submitted, the system must provide an order preview.

The preview must allow the user to verify:

* Customer or room
* Items
* Quantities
* Unit prices
* Totals
* Notes
* Destination such as kitchen or bar

## 9. Payment Rules

The system must support:

* Cash
* Mobile money
* Card
* Bank transfer
* Credit
* Split payment

One bill may contain multiple payment transactions.

Example:

Bill total: 300,000 RWF

* Cash: 100,000 RWF
* Mobile money: 120,000 RWF
* Card: 80,000 RWF

Each payment component must remain independently auditable.

## 10. Partial Payment Rules

A customer may pay only part of an outstanding balance.

The system must maintain:

* Original amount
* Amount paid
* Remaining amount

Payment status may include:

* OPEN
* PARTIALLY_PAID
* PAID
* CREDIT
* OVERDUE
* VOIDED
* CANCELLED

## 11. Payment Responsibility Rules

Restaurant, bar, and other consumption payments may be received by authorized users such as:

* Waiter
* Receptionist
* Cashier

Room accommodation settlement must follow reception/cashier permissions.

Financial permissions must be controlled by role.

## 12. Cashier Rules

Cashiers must work through cashier shifts.

A cashier shift must contain:

* Opening time
* Closing time
* Opening float
* Expected cash
* Counted cash
* Difference
* Other payment totals

Transactions must be filterable by payment method.

Examples:

* Cash transactions
* Mobile money transactions
* Card transactions
* Bank transfer transactions

## 13. Credit Rules

Approved customers may purchase on credit.

Credit accounts may belong to:

* Individual
* Company
* Tour agency
* NGO
* Government institution

Credit management must support:

* Credit limit
* Current outstanding balance
* Payment terms
* Due dates
* Payments against credit
* Overdue balances

Exceeding a credit limit must require appropriate authorization.

## 14. Product and Inventory Rules

A product or inventory item may contain:

* Name
* SKU
* Category
* Unit of measure
* Selling price
* Purchase cost
* Opening quantity
* Minimum stock
* Reorder level
* Stock tracking status

Opening quantity is used only when introducing an existing stock item into the system.

Stock balances must later be driven by stock movements, not repeatedly overwritten manually.

## 15. Sales and Stock

Where applicable, confirmed sales must affect inventory.

Examples:

* Bottled drink sale reduces drink stock.
* Restaurant recipe consumption reduces ingredients.
* Minibar sale reduces minibar stock.

Stock deduction must happen through controlled stock movements.

## 16. Purchasing Rules

Purchase records must contain the actual purchase price.

A confirmed goods receipt must increase inventory.

Purchasing must retain historical purchase prices.

An old purchase must not change because the item's current cost changes later.

Purchases may be:

* Fully paid
* Partially paid
* Credit

Supplier obligations must be reflected in financial records.

## 17. Vendor Rules

Vendors must have reusable profiles.

Vendor history must support:

* Purchases
* Purchase invoices
* Payments
* Credit purchases
* Outstanding balances

## 18. Financial Integrity Rules

Financial transactions must not be permanently deleted.

Corrections must use controlled actions such as:

* Void
* Reverse
* Cancel
* Adjustment

The original transaction must remain auditable.

## 19. Approval Rules

Sensitive actions may require approval, including:

* Discount
* Refund
* Void
* Cancellation
* Credit override
* Stock adjustment
* Complimentary item
* Room rate override
* Checkout with unpaid balance

A user must not approve a sensitive action when segregation-of-duty rules prohibit self-approval.

## 20. Reporting Rules

Reports must support date filtering.

Common filters include:

* Today
* Yesterday
* This week
* This month
* Custom period
* Branch
* Department
* Cashier
* Payment method
* Customer
* Product
* Sale type

Sales reporting must distinguish:

* Cash sales
* Credit sales
* Partially paid sales
* Fully paid sales
* Outstanding balances

## 21. Languages

The application must support:

* English
* French
* Kinyarwanda

Language codes:

* `en`
* `fr`
* `rw`

Business data must remain language-neutral where possible.

User interface translations should be handled independently from transactional data.

## 22. Audit Requirement

Every sensitive action must be attributable to a specific authenticated user.

Shared staff accounts must not be used.

Important operations must preserve:

* User
* Date and time
* Action
* Record affected
* Previous value where applicable
* New value where applicable
* Reason where required
* Approval information where required
