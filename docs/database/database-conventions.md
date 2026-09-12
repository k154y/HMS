# Hotel Management System — Database Conventions

## 1. Database

Primary database:

PostgreSQL

Schema changes are managed with Flyway.

## 2. Naming

Table names use lowercase plural snake_case.

Examples:

* `hotels`
* `branches`
* `users`
* `reservations`
* `reservation_rooms`
* `payments`
* `stock_movements`

Column names use lowercase snake_case.

Examples:

* `hotel_id`
* `created_at`
* `payment_method`
* `trial_ends_at`

## 3. Primary Keys

Primary business tables use UUID primary keys.

Example:

```sql
id UUID PRIMARY KEY
```

Application code should generate UUIDs unless a database-specific strategy is explicitly chosen.

## 4. Tenant Ownership

Tenant-owned tables must contain:

```text
hotel_id
```

Branch-owned records should contain:

```text
branch_id
```

when relevant.

Tenant ownership must never be inferred only from frontend input.

## 5. Foreign Keys

Foreign key constraints should normally be enforced by PostgreSQL.

Example:

```sql
FOREIGN KEY (hotel_id)
REFERENCES hotels(id)
```

## 6. Timestamps

Transactional timestamps should use:

```text
TIMESTAMPTZ
```

The system stores timestamps consistently and converts them for hotel/branch display time zones.

Common fields:

```text
created_at
updated_at
```

## 7. Monetary Amounts

Financial amounts must never use floating-point database types.

Use fixed precision numeric values.

Preferred pattern:

```text
NUMERIC(19, 4)
```

Currency code must be known from the transaction or hotel context.

## 8. Quantities

Inventory quantities may require decimal values.

Examples:

* 1 bottle
* 2.5 kg
* 750 ml

Stock quantity types must therefore support decimal precision where applicable.

## 9. Immutable Financial History

Historical transactions must retain the values that applied when the transaction occurred.

Examples:

A purchase record preserves:

* Purchase price
* Quantity
* Tax
* Total

A sale item preserves:

* Selling price
* Quantity
* Discount
* Tax
* Line total

Changing the current product price must never rewrite previous transactions.

## 10. No Hard Delete for Financial Records

Financially relevant records should normally use:

* Status
* Void
* Reversal
* Cancellation

rather than physical deletion.

## 11. Indexing

Indexes should be created based on real access patterns.

Important recurring index candidates include:

* `hotel_id`
* `branch_id`
* Reservation dates
* Room IDs
* Customer IDs
* Payment dates
* Order dates
* Vendor IDs
* Status fields where selective

Indexes must not be added blindly to every column.

## 12. Uniqueness

Uniqueness should generally be scoped to the hotel or branch where appropriate.

Example:

Room number may be unique within a branch rather than globally.

Possible constraint:

```text
UNIQUE(branch_id, room_number)
```

Vendor codes may be unique within a hotel.

## 13. Status Values

Important state machines should use controlled status values.

Examples:

Reservation:

* PENDING
* CONFIRMED
* CHECKED_IN
* CHECKED_OUT
* CANCELLED
* NO_SHOW

Payment:

* PENDING
* COMPLETED
* FAILED
* REVERSED
* VOIDED

Status transitions must be validated in application services.

## 14. Migrations

Flyway migrations must be:

* Ordered
* Immutable after deployment
* Reviewed before production
* Backward-aware where possible

Example:

```text
V1__create_platform_foundation.sql
V2__create_users_and_security.sql
V3__create_customer_foundation.sql
```

A migration already applied to shared environments must not normally be edited.

A new migration should correct or extend it.

## 15. Database Source of Truth

PostgreSQL is the source of truth for:

* Reservations
* Stays
* Folios
* Payments
* Orders
* Inventory
* Purchases
* Accounting
* Audit records

Redis and RabbitMQ must not replace the authoritative transactional records.
