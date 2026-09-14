# HMS production system design

Updated 14 September 2026. This is the implementation blueprint and boundary of the current product, not a production certification. The repository contains real Spring controllers and a Next.js frontend. Proposed tables and workflows below are explicitly marked as planned; they are not available endpoints.

## 1. Feature modules and implementation status

| Module | Working foundation in this repository | Remaining production work |
|---|---|---|
| Platform administration | Separate platform administrators, hotel/owner onboarding and trial metadata | Subscription billing, grace-period policy and comprehensive write restrictions |
| Tenant identity | Hotels, branches, memberships, multiple roles, combined permissions, refresh sessions | Departments, complete cross-tenant negative test matrix, production recovery/MFA policy |
| Reception | Customers, guests, room types, room edit/archive, bed details, availability, reservations, stays | Approved rate overrides and unpaid-checkout decision workflow |
| Customer billing | Resident and roomless customer folios, immutable charges/payment entries, customer portfolio grouping | Formal table/open-bill context, events/day-use scheduling, deposits and credit terms |
| Restaurant/bar/kitchen | Dedicated menu editor using products, POS item confirmation and preview, preparation routing, stock deduction | Recipes and ingredient consumption, table seating, modifiers and printer integration |
| Inventory/purchasing | Product costs/prices/units, stock movements, purchase approval/receipt, supplier payments and balances | Separate invoices and goods-received notes, partial receipts, returns, cost layers and invoice matching |
| Payments/cashier | Split payment requests, approval, full refunds, shift reconciliation | Controlled partial refunds, cash handovers, complete reason/approval coverage and bank reconciliation |
| Credit | Approved credit folios, customer ledger and settlement through payment approval | Credit application decisions, enforced limits, terms, due dates and aging |
| Finance | Expense register, date-filtered billing/cash reports, customer and supplier balances, CSV and print export | General ledger, tax configuration, COGS, financial statements and period closing |
| Staff operations | Housekeeping assignments, maintenance records, audit events | Department scheduling, escalation and full before/after audit coverage |
| Infrastructure | PostgreSQL, Redis and RabbitMQ configuration; Docker development and isolated integration services | Durable business-event outbox, tenant-bound real-time notifications, backups, monitoring and deployment hardening |

Keep the Spring application a modular monolith. Each module owns its writes, validation and repositories. Cross-module changes use application services within one database transaction when atomicity matters. Do not split into microservices, adopt Kafka or move to Kubernetes until measured capacity or operational requirements justify it.

## 2. Folder structure

```text
HMS/
  apps/api/
    src/main/java/com/hotelmanagement/hms/
      platform/ identity/ customer/ room/ reservation/ stay/
      folio/ order/ product/ inventory/ vendor/ purchase/
      payment/ credit/ housekeeping/ maintenance/ report/ audit/ shared/
    src/main/resources/db/migration/  Versioned Flyway migrations
    src/test/java/                   Unit, contract and integration tests
  apps/web/
    app/(dashboard)/                 Operational pages
    app/api/                         NextAuth and authenticated Spring proxy
    components/                      Shared UI and business forms
    lib/                             API client, authentication, translations, exports
  docs/architecture/                 Design and implementation boundaries
  docs/testing/                     Verification results
  docs/maintenance/                 Cleanup/recovery inventory
  scripts/                          Reproducible local checks
  infra/postgres/init/               First-start database initialization
  docker-compose.dev.yml
  docker-compose.test.yml
```

New modules should follow the existing `model`, `repository`, `service`, `web` organization. Add subscription, accounting, department and messaging packages when implementing their workflows, rather than creating empty placeholder applications. Generated files, credentials, local logs and the reversible cleanup archive remain ignored. The old Prisma application is outside the active application tree.

## 3. Database design

### Existing tables

| Area | Tables |
|---|---|
| Tenant configuration | `hotels`, `branches`, `hotel_exchange_rates` |
| Identity | `users`, `hotel_memberships`, `membership_branch_access`, `roles`, `permissions`, `role_permissions`, `membership_roles`, `refresh_sessions` |
| Platform | `platform_administrators`, `platform_audit_events` |
| Reception | `customers`, `guests`, `room_types`, `rooms`, `reservations`, `reservation_rooms`, `reservation_guests`, `stays` |
| Billing | `folios`, `folio_entries`, `orders`, `order_items` |
| Stock and suppliers | `products`, `stock_movements`, `vendors`, `purchase_orders`, `purchase_order_items`, `vendor_payments` |
| Collections | `payments`, `payment_approvals`, `cashier_shifts`, `expenses` |
| Credit | `credit_accounts`, `credit_ledger` |
| Operations | `housekeeping_tasks`, `maintenance_tickets`, `audit_events` |

The existence of `credit_accounts` does not mean its limits and terms are fully enforced. Likewise, a purchase-order balance is not a matched supplier-invoice accounts-payable ledger.

### Planned tables, added only with tested workflows

| Area | Proposed tables and purpose |
|---|---|
| Departments | `departments`, `membership_department_access` for operational assignment and authorization |
| Non-resident service context | `restaurant_tables`, `service_resources`, `service_bookings`, `bill_contexts` for table bills, conferences, events and day use |
| Credit control | `credit_requests`, `credit_decisions`, `customer_credit_terms` with limit, due date, reason and approver |
| Supplier documents | `supplier_invoices`, `supplier_invoice_lines`, `goods_receipts`, `goods_receipt_lines`, `invoice_matches`, `purchase_returns` |
| Inventory costing | `recipes`, `recipe_ingredients`, `inventory_cost_layers` and consumption links |
| Accounting | `chart_of_accounts`, `journal_entries`, `journal_lines`, `accounting_periods`, `tax_codes`, `bank_reconciliations` |
| SaaS billing | `subscription_plans`, `hotel_subscriptions`, `subscription_invoices`, `subscription_payments`, `billing_webhook_events` |
| Reliable jobs | `transactional_outbox`, `processed_messages`, `export_jobs` |

Every tenant-owned record carries `hotel_id`; branch records also carry `branch_id`. Validate related objects belong to the same tenant and branch, reinforced with composite foreign keys where practical. Never accept tenant identity solely from a request body. Use UUID identifiers, decimal money, explicit currency, UTC timestamps and hotel-time-zone reporting boundaries. Do not aggregate mixed currencies without an explicit conversion rate and valuation policy.

Keep immutable transaction entries; correct them with linked reversals. Use row locks and unique idempotency keys for approval, posting, stock receipt and settlement. Keep PostgreSQL's room-booking exclusion constraint as the final concurrency guard. Add indexes beginning with tenant/branch and report date; paginate registers and queue large exports.

## 4. Backend boundaries

Controllers authenticate, bind requests and call application services. Services enforce tenant access, permissions, subscription entitlement, state transitions, money rules and transaction boundaries. Repositories never serve as a permission bypass.

The report module reads operational ledgers; it must not modify transactions. The future accounting module posts balanced journals from verified business events. Identity owns roles and active memberships. Platform administrators manage SaaS accounts; platform access does not implicitly grant access to a hotel's financial operations.

The current finance endpoint is `GET /api/v1/hotels/{hotel}/branches/{branch}/reports/accounting?from=YYYY-MM-DD&to=YYYY-MM-DD`. It uses a repeatable-read snapshot and hotel-local date boundaries. CSV export uses these actual returned records. Future modules need contracts and migrations before adding UI links; do not create success-only mock endpoints.

## 5. Frontend pages

Existing operational pages cover dashboard, rooms, room types, reservations, stays, customers, folios, POS/orders, menu, preparation, products/stock, vendors, purchases, payments/cashier, shifts, expenses, credit, reports, housekeeping, maintenance, staff and audit. Platform onboarding is separate.

The new menu page edits the same product catalog consumed by POS. A kitchen menu item can be sold without tracking finished-item stock. Ingredient deduction requires a recipe; it must not silently pretend that one prepared meal equals one ingredient unit.

Finance uses summary cards, date selection, payment-method filtering, detailed registers, customer/supplier balances and CSV/print export. Retain visible reasons, approval status, validation feedback and current-balance timestamps. Add document-level drill-down as invoice and general-ledger workflows are implemented.

Planned pages: subscription/billing, departments, table floor plan, service/event bookings, credit requests and aging, supplier invoices/receipts/matching, recipes, journal inquiry, bank reconciliation and period closing. English, French and Kinyarwanda must cover labels, errors, statuses and exports. Automated key coverage is useful but does not replace a fluent review of translations.

## 6. Roles and permissions

| Role | Scope |
|---|---|
| Platform administrator | Create/manage hotel subscriptions and owners; no automatic hotel-operator access |
| Owner | Every hotel permission, limited to the owner's tenant and authorized branch access |
| Manager | Operations, staff, approvals and oversight according to assigned permissions |
| Accountant | Finance, supplier/customer settlement, reporting and authorized stock corrections |
| Auditor | Read/export and audit review; no posting rights from this role alone |
| Cashier | Payment decisions, own shifts, reconciliation and permitted finance views |
| Receptionist | Reservations, stays, folios and room-payment requests |
| Waiter | Food/service orders and permitted collection requests; no room collections |
| Bartender / kitchen | Assigned preparation queues and item status transitions |
| Storekeeper | Stock and purchasing duties within assigned permissions |
| Housekeeper | Assigned housekeeping tasks |

Multiple roles combine permissions. An accountant with auditor access retains accountant write permissions; adding a read-only role does not revoke another role's rights. Owner permission does not bypass tenant isolation or ledger integrity.

Current payment policy permits an owner to decide their own payment request only with a recorded reason. Other approvers cannot approve their own request. Every approval that posts a receipt requires the approving user's open cashier shift. A waiter recording a pending request has not yet posted a receipt. If staff physically collect money before approval, implement explicit handover and custody records before using this process for that cash.

## 7. Main workflows

### Rooms and residents

Choose dates first, then search available rooms. Capture person/company booking contact, occupants, adults and children, allowing multiple rooms and guests. Room creation inherits type occupancy, bed information and nightly rate, with an explicit room price override. A production override should require permission and a reason. Check-in posts charges to the stay folio using the agreed price. Bar, food and other services reference the same customer portfolio. Partial/split approved receipts reduce its balance. Checkout requires settlement; a planned authorized unpaid-checkout decision transfers the debt without erasing it.

### Non-resident customers

Customer legal type (person, company, NGO, government, agency) is distinct from whether a visit is resident or non-resident. Reuse one customer record and portfolio over time; preserve separate historical bills. Roomless orders already work. Table, event, pool, transport, laundry and outside-catering contexts should reference that customer and a bill without a required room. Sending posts the charge. Immediate or partial approved payment posts receipts. Credit transfer requires an explicit authorized decision, then due-date tracking. Reports must expose draft/open/unpaid/partial/credit/voided bills and settlements without treating all bill totals as collected sales.

### Supplier purchasing and expenses

Create and approve a purchase order; record received quantities against a goods-received note; match the supplier invoice and record the liability. Stock receipts increase inventory, not operating expense. A service invoice can debit the relevant expense and credit accounts payable. Paying an invoice debits accounts payable and credits the selected cash/bank/mobile-money account. It must not create the expense twice. A stock sale recognizes inventory cost as cost of sales under the chosen costing policy. The current purchase/payment workflow is a foundation; separate receipt/invoice matching and double-entry posting remain planned.

Inventory cost is normally recognized as expense when the related inventory is sold; see [IAS 2 Inventories](https://www.ifrs.org/issued-standards/list-of-standards/ias-2-inventories/). Configure local accounting/tax policy with the hotel's accountant before production statements are issued.

### Payment and cashier

Open a personal shift with opening float. Record a split request whose parts equal the requested payment and do not exceed the collectible balance. An authorized decision locks the request and shift, then posts once. Refunds create reversal entries rather than delete receipts. Close the shift with an actual count; expected cash equals opening cash plus recorded cash receipts minus cash refunds and relevant cash outflows. Report shortages/excess; never overwrite the expected value to match the count. Refund authorization, outflow shift association and cash handovers need further hardening before production cash-control acceptance.

### SaaS subscription

Start the free trial at successful hotel activation for three calendar months, using an explicit stored start/end time. Plan the statuses `TRIAL`, `ACTIVE`, `EXPIRED`, `SUSPENDED`, `CANCELLED`, with a configurable grace end separate from status. During an expired trial's grace period, policy may permit operations with a visible notice. After grace, deny new business transactions centrally while retaining authenticated historical viewing/export. Security actions such as password changes and revoking access remain available.

Enforce this server-side across every mutation, jobs and WebSocket commands; hiding buttons is insufficient. A verified, idempotent billing-provider webhook can activate a paid subscription. Do not trust a browser success URL. Existing trial metadata is not complete enforcement; billing provider, billing currency and grace duration remain deployment decisions.

## 8. Reports and accounting meaning

Current finance output separates billed amounts, customer receipts, refunds, supplier payments, operating expenses and net cash movement. Receipts/refunds use immutable folio entries, so a later refund does not erase an earlier period's receipt. Current debtor balances and outstanding purchases are a present-time snapshot, not an aged or historical closing balance.

**Net cash movement = receipts − refunds − supplier payments − expense payments.** It is neither profit nor a bank balance. Outstanding approved/received purchase orders are not formal invoice-based accounts payable. The interface must state these limits. Cash flow and profit answer different questions; see [IAS 7 Statement of Cash Flows](https://www.ifrs.org/issued-standards/list-of-standards/ias-7-statement-of-cash-flows/).

Production reports should include:

- Sales by hotel-local date, branch, department, item, user and resident/non-resident context, with tax and reversals separated.
- Every bill status, credit approvals, debtor aging, settlement history and customer statements.
- Purchase commitments, receipts, invoice matching, supplier aging, supplier statements and payment history.
- Inventory movements, valuation, recipe consumption, wastage and stock-adjustment exceptions.
- Receipts/refunds by method, cashier reconciliation, cash custody and bank reconciliation.
- Authorized journal inquiry, trial balance, income statement, balance sheet and cash-flow statement after a tested general ledger exists.
- Voids, refunds, discounts, overrides, unpaid checkout decisions and audit exception reports.

Exports must preserve filters, tenant/branch, currency, timezone, generation time and stable document references. Escape spreadsheet formulas in untrusted text. Large datasets need streamed/queued exports with access-controlled expiry rather than loading years of records into the browser.

## 9. Anti-cheating and reliability controls

Use individual accounts and revoke inactive membership access. Enforce permissions in Spring, not only navigation. Financial and stock records are append-only; archive master data with references intact. Sensitive actions need permission, reason, approval decision and traceable reversals. Payment decisions now record before/after status, reason, approver, timestamp and request context; complete before/after coverage for other operations remains work.

Capture device/user-agent and address as supporting context, not proof of identity. Avoid storing secrets in audit payloads. Restrict audit modification/export and define retention. Use a separate approver where practical; flag owner self-decisions for review. Enforce shift ownership and single posting under concurrent requests.

For RabbitMQ, save an outbox event in the business transaction, publish after commit, deduplicate consumers and use bounded retries/dead-letter handling. WebSocket subscriptions must authenticate and authorize tenant/branch channels; send notifications that cause clients to reload authoritative data. Redis must not be the source of financial truth. These business-event reliability mechanisms are planned, not implied by running the containers.

## 10. Development steps in dependency order

1. Finish tenant isolation, subscription write policy, branch/department access and identity recovery. Verify expired hotels retain read/export access.
2. Stabilize customer identity, room pricing, reservations and stay/folio ownership; test overlap and cross-tenant attacks.
3. Add supplier documents, units, stock costing and recipes; agree tax and accounting policies before financial-statement work.
4. Implement balanced journal posting and idempotent links from operational transactions, then supplier receipt/invoice matching and accounts payable.
5. Complete non-resident table/service contexts, approved discounts/voids and bill state transitions.
6. Finish payment custody, controlled refunds, shift outflows, bank reconciliation and unpaid-checkout approvals.
7. Add credit decisions, limits, terms, aging and statements; test concurrent settlement and refunds.
8. Complete accounting reports, pagination/queued exports, all language review and accessible responsive UI acceptance.
9. Add transactional outbox, reliable RabbitMQ consumers, tenant-bound WebSockets and operational monitoring.
10. Run production acceptance: PostgreSQL/Redis integration, concurrency, role matrix, billing webhook replay, backup restore, migration rollback strategy, load tests, security review and financial reconciliation. Deploy Docker with managed secrets/TLS/backups first. Consider Kubernetes or service extraction only after demonstrated need.

Current automated tests establish useful correctness evidence, not all these acceptance gates. Keep the latest actual results in `docs/testing` and update this document when planned modules become implemented and verified.
