# Commerce trading and accounting implementation

Date: 2026-10-15. This extends the configuration/stock implementation documented in `commerce-configuration-and-stock.md`. It is an implementation and verification report, not a claim that the entire ISDMS roadmap is finished.

## Repository and environment

- Branch: `yfeature`.
- Baseline: `6e2d65c256699b9e9697b5701344914b4a9dac1b`.
- The working tree already contained hotel payment/authentication/transfer/credit changes, Commerce foundations, shared authentication, provisioning, and local backups. Those were preserved.
- No commits, pushes, deployments, backend restarts, dependency installation, or writes to the existing application database were performed.
- New migrations were exercised only in uniquely named disposable PostgreSQL instances with tmpfs storage. Tests explicitly verified database/user `hms_test`, used isolated Redis, existing cached images/dependencies, and removed only their own Compose project.
- The running application has **not** been upgraded to this backend. New screens require the corresponding backend/migrations before operational use. Production rollout and real-account acceptance testing remain separate work.

## Existing functionality reused

The implementation reuses Commerce business/branch/location identifiers, module entitlements, explicit branch grants, global identity/password onboarding, catalog units, dated currency quotations, immutable stock movements, authenticated JSON proxies, translation infrastructure, and the isolated Maven runner. It does not route Commerce transactions through hotel folios, hotel inventory, or hotel payment services.

Super Admin has one entry: `/platform` redirects to `/platform/organizations`. Provisioning remains separate from tenant operations. An explicit module editor can enable ACCOUNTING/RETAIL for an organization; it creates no operational membership. CORE and INVENTORY remain required, and RETAIL requires ACCOUNTING. WHOLESALE/DISTRIBUTION orchestration is not advertised as enabled functionality.

## Implemented behavior

- Owner-approved opening stock valuation, immutable product/location quantity/cost/value snapshots, fixed base currency after activation, and balanced immutable branch journals.
- Customer/supplier records, audited credit-limit/contact/status changes, and payment destinations with separate currency, bank/mobile/card/cash method, and account/phone/merchant reference.
- Product descriptions/category/status updates, optional wholesale price, minimum price, dated branch prices, tax-inclusive/exclusive configuration, recoverable/nonrecoverable purchase tax, stock thresholds and extra package units. Product updates preserve before/after audit snapshots. Cashiers cannot edit catalog prices or override checkout prices.
- Purchase invoices with supplier/reference/due date, independent ordered and received quantities, partial physical receipt, and separate partial payments. Storekeepers can receive stock without seeing purchase costs or financial totals.
- Immediate fulfilled sales with selected-location deduction, server-priced preview, product scanning/search, retail/wholesale/branch prices, authorized reasoned overrides, credit limits, cash or split tenders, and atomic stock/financial posting.
- Immutable invoice product/SKU/unit/price/tax/currency/rate/COGS snapshots and business/branch/location/cashier header names. Receipt lines cannot be appended after the posting transaction.
- Parent-command replay checks compare actor, kind and complete JSON payload before recalculation. Changed requests are rejected; retries do not allocate fresh payments, deduct stock again, or repost revenue. Writes serialize on the business row.
- Separate customer and supplier settlements in configured account currencies; historical carrying value and current settlement rates are both retained, with FX gain/loss postings.
- Manager-controlled partial/full goods returns, original-value revenue/tax reversal, saleable-stock restoration, supplier-stock reversal, and separate cash/bank refunds. Originals are never deleted.
- Cashier shift opening/count/close, manager cash custody movements with reasons, expected/count variance, independent manager approval, and database protection for closed shifts. Cash tenders require the actor's open matching branch/account shift.
- Noncash operating expenses, separate from inventory receipts.
- Branch-scoped reports: journal accounts, current inventory valuation/low-stock flags, sales and purchases by branch/cashier/party/currency, collections, current receivables/payables by document, product/category sales, returns, revenue/COGS/gross profit/margin.
- Searchable recent document history, payment/return history, audited print requests, and browser printing to 58 mm, 80 mm or A4/PDF. Receipts explicitly do not claim Rwanda EBM compliance.
- Browser requests retain pending command payloads and UUIDs in session storage scoped to user/business. Unknown outcomes retry the same command. Decimal inputs remain strings, and trading responses serialize BigDecimal values as strings.

## Accounting rules

All posted journal entries balance within each branch; deferred database triggers reject unbalanced commits, updates/deletes, and late line insertion.

| Event | Debit | Credit |
|---|---|---|
| Opening valuation | Inventory | Opening equity |
| Purchase invoice | Goods clearing; recoverable input tax | Payables |
| Physical purchase receipt | Inventory | Goods clearing |
| Supplier settlement | Historical payable; FX loss if applicable | Cash/bank; FX gain if applicable |
| Sale invoice | Receivables | Sales; output tax |
| Sale fulfillment | COGS | Inventory |
| Customer settlement | Cash/bank; FX loss if applicable | Historical receivable; FX gain if applicable |
| Sale return | Sales; output tax | Receivables |
| Saleable return | Inventory at original COGS | COGS |
| Supplier return | Payables; variance if applicable | Inventory at carrying value; input tax; variance if applicable |
| Noncash expense | Expense | Cash/bank |

Nonrecoverable purchase tax is included in inventory acquisition value. Partial receipts allocate invoice value proportionally and the final receipt absorbs the residual. Sales remove weighted-average carrying value; taking the final quantity removes the full remaining value. Transfers retain that carrying value and use offsetting interbranch accounts, never sales or expense. Manual positive/negative adjustments use stock-gain/loss accounts. Opening-stock additions use opening equity.

Currency rounding follows ISO currency precision. Invoice base totals sum rounded line net/tax values; they may differ slightly from independently converting the grand total. Settlements allocate document currency at the settlement-time rate, settle original carrying value, and post the difference as FX. Previously posted invoices are never revalued using a later rate. Payments larger than the remaining document balance are rejected; there is no automatic cash-change/overpayment workflow.

Cash drawer starting funds and deposits/withdrawals represent custody within a cash account; they are **not** new income/expenses. They do not substitute for an opening cash/bank ledger balance or a transfer between financial accounts. Report date filters use UTC calendar days. Inventory and open customer/supplier balances are current; journal/collection/return totals use the selected date range. Product sales are explicitly before returns; overall gross profit includes return postings.

## Files created or modified in this continuation

New backend files:

- `apps/api/src/main/java/com/hotelmanagement/hms/commerce/accounting/CommerceLedgerService.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/commerce/accounting/CommerceTradingService.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/commerce/accounting/CommerceFinanceService.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/commerce/accounting/CommerceTradingController.java`
- `apps/api/src/test/java/com/hotelmanagement/hms/CommerceTradingTest.java`

New Flyway migrations:

- `V39__commerce_financial_documents.sql`: financial schema, product pricing/tax fields, immutable events/journals, shifts, expenses, financial movement types.
- `V40__commerce_journal_trigger_record_shape.sql`: additive correction to the deferred trigger's handling of distinct PostgreSQL record shapes.
- `V41__commerce_receipt_snapshots.sql`: receipt header/category snapshots, prevention of late document lines, supporting indexes.
- `V42__commerce_opening_valuation_snapshots.sql`: immutable approval-time inventory valuation details.

Modified backend files:

- `commerce/catalog/CommerceCatalogService.java`
- `commerce/catalog/CommerceCatalogController.java`
- `commerce/inventory/CommerceOpeningStockService.java`
- `commerce/inventory/CommerceStockService.java`
- `platform/service/KbpProvisioningService.java`
- `platform/web/KbpProvisioningController.java`

These paths are under `apps/api/src/main/java/com/hotelmanagement/hms/`. Earlier foundation changes and V37/V38 are listed in the companion report.

Frontend files:

- New `apps/web/components/commerce/CommerceTrading.tsx`.
- New `apps/web/components/commerce/CommerceProductEditor.tsx`.
- New `apps/web/app/api/platform/organizations/modules/route.ts`.
- Modified `apps/web/components/commerce/CommercePanel.tsx`.
- Modified `apps/web/app/platform/organizations/OrganizationPanel.tsx`.
- Modified `apps/web/lib/i18n.ts`.
- This report. Build-generated Next type-path changes are restored after verification.

## Verification record

First backend command:

```powershell
./scripts/test-hms-consistency.ps1 -ContainerBuild -Tests 'CommerceTradingTest,CommerceConfigurationTest,CommandConsistencyTest,FinancialConfigurationTest,OperationsTest'
```

Result before V40: 64 tests, 52 passing, 12 errors. All 12 new trading tests stopped during accounting activation: `record "new" has no field "journal_id"`. PostgreSQL resolves both field references in the original CASE expression; V40 uses separate IF statements. The failing report is retained locally at `apps/api/target/commerce-verification/first-run/CommerceTradingTest.txt`. No test was suppressed.

Focused correction check:

```powershell
./scripts/test-hms-consistency.ps1 -ContainerBuild -Tests 'CommerceTradingTest'
```

Result: 12 tests passed, no failures/errors/skips.

Expanded suite: the same five-class command passed **70 tests: 18 trading, 14 Commerce configuration, 23 command consistency, 8 financial configuration, 7 hotel operations; zero failures/errors/skips**. Runtime 7:09. After final opening-valuation snapshot, cash-withdrawal guard, movement trace and derived-status changes, a focused final rerun uses:

```powershell
./scripts/test-hms-consistency.ps1 -ContainerBuild -Tests 'CommerceTradingTest,CommerceConfigurationTest'
```

The focused rerun passed **32 tests, zero failures/errors/skips**, runtime 3:12. A subsequent review tightened foreign-currency minimum prices to round upward rather than weaken the base-currency floor. A nineteenth trading regression covers rejection below the floor and safe standard-price rounding. The final `CommerceTradingTest` result follows after completion.

Frontend command:

```powershell
docker exec -w /workspace/apps/web hotel_hms_app sh -c 'npm run check:translations && npm run typecheck && cd /workspace && node --test scripts/test-workspace-navigation.cjs scripts/test-web-errors.cjs && cd /workspace/apps/web && NEXT_OUTPUT_DIR=.next-build npm run build'
```

First attempt stopped at a literal `A4 / PDF` translation-check finding. It was corrected without skipping the check. The rerun passed translations (**893 keys, 538 literal UI keys**), TypeScript, **14 Node tests**, and the **Next.js production build (37 generated entries)**. The generated `.next-build/dev/types` references were restored to the previous development configuration afterward.

Additional check: `git -c core.safecrlf=false diff --check`.

## Browser acceptance instructions

Use an isolated acceptance deployment with these migrations/backend; do not run this walkthrough against live customer data.

1. Sign in as Super Admin. Confirm `/platform` opens the shared organization page. Enable ACCOUNTING/RETAIL for a test Commerce organization. Confirm its operations remain inaccessible to Super Admin without tenant membership.
2. Sign in as its owner. Configure currencies and effective rates, verify products/costs/opening quantities, then approve opening valuation under **Sales and purchasing**.
3. Create customer/supplier records and distinct bank RWF, bank USD, mobile number/code and cash accounts. Assign test cashiers/storekeepers to a single branch.
4. Post an unpaid purchase invoice. Verify stock is unchanged. Receive part, inspect stock/clearing/payables, receive the rest, and record separate supplier payments.
5. Open a cashier shift. Select an assigned branch/location, scan a SKU/barcode, review a sale, then pay using cash or split tenders. Verify one stock deduction and the retained receipt. Test partial customer credit and later collection.
6. Change catalog prices, branch names and exchange rates; reopen the original receipt. Prices/header/rate snapshots must remain unchanged. Settle a foreign-currency balance and inspect FX entries.
7. As manager, return one saleable unit and issue its separate refund. Test an unsaleable return and supplier return. Verify originals remain visible.
8. Close/count the cash shift and approve as another manager. Confirm late cash postings, closed-count editing, cashier price changes, unauthorized branches and cross-tenant IDs are rejected.
9. Print 58/80 mm and A4, inspect actual printer margins/page breaks and browser Save as PDF. Verify a print event is recorded. Test network loss/reload and retry with the retained request ID; do not generate a fresh request for an unknown outcome.

Actual authenticated browser and physical-printer acceptance testing has not been performed in this run.

## Remaining scope and rollout limits

- Optional batch/expiry tracking, product variant attributes and customer-group price lists are not implemented.
- Wholesale price selection works for immediate fulfilled sales; reserved wholesale delivery orders, dispatch orchestration and distribution routing remain unimplemented.
- Purchasing currently uses invoice-before-receipt clearing. Purchase-order approval, receipt-before-invoice matching, landed-freight allocation and cancelling an unreceived remainder need separate workflows.
- Payments allocate to one selected document; there is no automatic allocation across several invoices, customer advances, overpayment/change, or payment-account reconciliation/import.
- Opening cash/bank/receivable/payable balances and general ledger adjusting/revaluation entries are not provided. Expense entry currently accepts noncash accounts only. This is not a complete general-purpose accounting package.
- Payment accounts are create/list only; account deactivation and beneficiary-change workflows are not implemented. Posted payment history retains immutable account IDs/currencies.
- Receipt PDF output uses the browser print dialog. There is no server PDF generator, printer-driver integration, or Rwanda EBM integration.
- History is capped at the most recent 200 documents per branch; large-history pagination/export and richer date/cashier filters are not yet available. Reports need production-volume performance testing.
- Business-wide serialization favors correctness over checkout throughput. Assess contention before large-scale rollout.
- Source/build/test verification does not replace isolated browser acceptance and a separately reviewed production migration/restart plan.

Recommended next work: validate these workflows in an isolated acceptance environment, then implement batch/expiry and reserved-delivery workflows with the same immutable command/ledger model. Do not enable unimplemented modules or describe the complete ISDMS roadmap as finished.
