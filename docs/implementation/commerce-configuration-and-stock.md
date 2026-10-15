# Commerce configuration, stock controls, and superadmin consolidation

## Scope and repository state

Branch: `yfeature`. Baseline: `6e2d65c256699b9e9697b5701344914b4a9dac1b`.

The working tree already contained uncommitted HMS fixes, commerce foundation/catalog/opening stock, V33–V36, KBP provisioning, temporary-password controls, authentication changes, and landing-page work. These were preserved. No Git reset, commit, push, service restart, dependency installation, or persistent database operation was performed.

**This is a partial implementation of the attached complete-ISDMS request, not a completed commerce/accounting system.** The implemented slice is configuration, employee branch access, stock commands, and consolidation of duplicate superadmin entry pages. The remaining transactional modules are listed below.

## Superadmin duplication

`/platform` rendered the older hotel-only provisioning form (`PlatformPanel`), while `/platform/organizations` already provided Hotel/Commerce provisioning through `KbpProvisioningService`. Both could create hotel owners, causing the duplication reported by the user.

The authenticated superadmin route `/platform` now redirects to `/platform/organizations`. The latter has workspace navigation, language selection, and logout instead of a link back to the duplicate form. Existing role checks, backend provisioning APIs, and the old component source are preserved. No platform administrator receives operational commerce membership through this change.

## Existing features reused and inspected

- Existing NextAuth identity, safe login destinations, route proxy, shared LocaleProvider and translations.
- `KbpProvisioningService`, organization UI, hotel-only UI, platform page guards, and existing organization provisioning contracts.
- `CommerceFoundationService` / controller, `CommerceCatalogService` / controller, `CommerceCatalogAccess`, `CommerceOpeningStockService` / controller.
- V34 business/branch/location/membership schema, V35 catalog and immutable movement schema, V36 temporary-password controls, and V20 platform-administrator schema.
- `UserAccountService` password hashing/policy, `TemporaryPasswordGate`, existing API proxy/error handling.
- `scripts/test-hms-consistency.ps1`, `docker-compose.test.yml`, Maven configuration, `IntegrationServices`, existing consistency tests and frontend Node tests.
- The frontend package/build/type configuration and running container inventory. The existing app and unrelated test/database containers were left running untouched.

## Implemented behavior

### Configuration and employee access

- Categories: optional parent label, top-level creation, search, rename/reparent, deactivation; reject cycles and deactivation while active children or products depend on a category. Graph edits serialize per business to prevent concurrent cycles.
- Branches: owner can edit name/contact and deactivate only after all stock locations are inactive.
- Locations: owner can rename/deactivate only when stock is zero. Their branch is immutable, so editing cannot silently relocate inventory history. New locations remain attached to a validated branch.
- Employees: owner creates identities using existing password hashing/policy; the temporary-password gate is reused. Owner assigns Manager, Cashier, Storekeeper, Accountant or Viewer and explicit allowed branches, changes roles/branch assignments, and disables membership. Owner membership cannot be changed through employee administration. Existing global accounts are not implicitly attached to another business.
- Branch access is enforced in backend branch/location/stock queries. Owners retain all-branch access. V37 preserves existing non-owner access to existing branches by explicit assignment; newly created non-owner staff have only their assigned branches.
- Cashiers and storekeepers cannot mutate catalog configuration. Purchase costs are omitted from catalog responses unless the role is Owner, Manager or Accountant. UI controls mirror these checks but are not the security boundary.
- Owners can inspect immutable configuration audit entries. New code does not audit passwords.

### Currency foundation

- Owner-managed allowed currencies, ISO decimal precision, immutable effective-dated rates, source, creator, and audit entries.
- Rate convention: one transaction-currency unit multiplied by `rate_to_base` yields base-currency value.
- Base currency remains fixed from provisioning. There is no base-currency change endpoint; a future conversion requires an explicit controlled migration, not editing historical transactions.
- Quote API returns original amount/currency, chosen rate ID/rate, base currency, and rounded base amount. It rejects missing dated rates, inactive foreign currencies, and excess transaction precision. It uses Java `BigDecimal` and HALF_UP rounding to the base currency's ISO precision.
- **Quotes are not posted financial transactions.** Posting-time persistence, settlement FX gains/losses, and historical invoice/receipt guarantees remain unimplemented. Inserting a backdated rate can change a future quote for that date; posted transactions must eventually retain their own snapshots.

### Inventory foundation

- Authorized owners/managers/storekeepers can post quantity adjustments and source-to-destination transfers in assigned branches/locations. Cashiers cannot do so.
- An immutable parent command records request ID, actor, business, product, quantity, source/destination and reason. Exact retries return the same command; altered request payloads are rejected.
- Source deduction and destination addition share one database transaction. Stock cannot become negative; failed commands and forced transaction failures roll back both legs and the parent.
- Transfers serialize per business for predictable lock ordering. This favors correctness over high-volume throughput; finer-grained locking can be introduced with equivalent concurrency tests later.
- The UI retains the exact pending payload for retries after an uncertain response. This state is component-local; leaving/reloading the page requires checking the ledger before initiating a new request.
- A location movement ledger displays up to the latest 500 entries. Full export/pagination remains future work.

## Accounting behavior

No purchasing, sales, payment, expense, tax, COGS, or general-ledger posting was added. A transfer or quantity adjustment creates stock records only. Consequently, this implementation must not be used as inventory valuation or financial accounting. Adding adjustment valuation and balancing journal entries is still necessary for an integrated accounting workflow.

## Files created in this request

- `apps/api/src/main/java/com/hotelmanagement/hms/commerce/foundation/CommerceConfigurationService.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/commerce/foundation/CommerceConfigurationController.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/commerce/inventory/CommerceStockService.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/commerce/inventory/CommerceStockController.java`
- `apps/api/src/main/resources/db/migration/V37__commerce_configuration_and_branch_access.sql`
- `apps/api/src/main/resources/db/migration/V38__commerce_stock_commands.sql`
- `apps/api/src/test/java/com/hotelmanagement/hms/CommerceConfigurationTest.java`
- `apps/web/components/commerce/CommerceConfiguration.tsx`
- `apps/web/components/commerce/CommerceStockOperations.tsx`
- `docs/implementation/commerce-configuration-and-stock.md`

## Existing files modified in this request

Several of these were already untracked before this request.

- `apps/api/src/main/java/com/hotelmanagement/hms/commerce/catalog/CommerceCatalogAccess.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/commerce/catalog/CommerceCatalogService.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/commerce/foundation/CommerceFoundationService.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/commerce/inventory/CommerceOpeningStockService.java`
- `apps/web/app/platform/page.tsx`
- `apps/web/app/platform/organizations/OrganizationPanel.tsx`
- `apps/web/components/commerce/CommercePanel.tsx`
- `apps/web/lib/i18n.ts`
- `scripts/test-workspace-navigation.cjs`
- `scripts/test-hms-consistency.ps1` (optional `-ContainerBuild` copies source/POM into the disposable Maven container to avoid slow Windows bind-mount classloading; test selection and database isolation stay unchanged)

## Verification

Commands:

```powershell
./scripts/test-hms-consistency.ps1 -Tests 'CommerceConfigurationTest,CommandConsistencyTest,FinancialConfigurationTest,OperationsTest'
./scripts/test-hms-consistency.ps1 -ContainerBuild -Tests 'CommerceConfigurationTest,CommandConsistencyTest,FinancialConfigurationTest,OperationsTest'
./scripts/test-hms-consistency.ps1 -ContainerBuild -Tests 'CommerceConfigurationTest'
docker exec -w /workspace/apps/web hotel_hms_app sh -c 'npm run check:translations && npm run typecheck && cd /workspace && node --test scripts/test-workspace-navigation.cjs scripts/test-web-errors.cjs && cd /workspace/apps/web && NEXT_OUTPUT_DIR=.next-build npm run build'
git -c core.safecrlf=false diff --check
```

The existing test runner creates a unique Docker project, verifies tmpfs PostgreSQL storage and the `hms_test` database/user, supplies test-only connection settings, runs Maven offline, then removes only its disposable services. V37/V38 are applied only there during verification, not to the running application database. The frontend build uses separate `.next-build` output.

Frontend results: PASS, 782 translation keys across all three languages and 490 literal UI keys checked; TypeScript passed; 14 Node regression tests passed (zero failures/skips); production build passed including generation of 36 static-page build entries. Automatic build edits to generated type references and the TypeScript include list were restored. Git diff whitespace check passed.

The first backend attempt compiled but was explicitly stopped during Spring context initialization (exit 143), after filesystem diagnostics showed slow classloading from the Windows bind mount and source review introduced final access checks. This interrupted attempt is not a passing test run. Only its named disposable Maven container was stopped, and its unique PostgreSQL/Redis project was removed by the runner. The replacement command recompiles the final source on the container filesystem and executes the same selected suites; it does not suppress or skip tests.

Backend consolidated results: PASS, 52 tests, zero failures/errors/skips: CommerceConfigurationTest 14, CommandConsistencyTest 23, FinancialConfigurationTest 8, OperationsTest 7. Both new migrations applied successfully in the disposable database.

A final review added a narrow fix allowing employee deactivation when previously assigned branches are already closed. The focused Commerce suite was rerun for that change: PASS, all 14 tests, zero failures/errors/skips. Frontend source was unchanged after its successful consolidated verification.

HTTP checks: anonymous `/platform` and `/platform/organizations` return 307 to login with their respective destinations; `/` returns 200. The Node test exercises the authenticated superadmin redirect and rejects non-superadmin access. No real-account browser sign-in, staff onboarding/password change, or authenticated UI workflow was performed.

## Browser verification instructions

Use an isolated environment with V37/V38 and the rebuilt backend after verifying its exact datasource. Do not point migration or testing commands at production or persistent development data.

1. Sign in as a test superadmin at `http://localhost:3001/login?next=/platform`. Confirm the resulting single administration page is `/platform/organizations`, with both product choices and logout. A hotel owner must not access it.
2. Sign in as a test Commerce owner at `http://localhost:3001/login?next=/commerce`. Select the business, branch and location.
3. Create/edit/search categories, test a circular parent and an in-use deactivation rejection. Rename a branch and stock location.
4. Open Business settings. Configure USD, add a dated rate with a source, then check rate/audit history. The base currency cannot be disabled.
5. Create a cashier and storekeeper assigned to one branch. Complete their first-login password change using the existing security flow. Check that another branch and forbidden write APIs remain inaccessible.
6. As authorized warehouse staff, post an adjustment and transfer; verify both locations and their ledger. Retry the same command after an uncertain response and verify no duplicate movement.

The currently running backend has deliberately not been restarted, so new backend endpoints/migrations are not assumed available at the local URL yet.

## Remaining functionality from the full request

- Product editing/history, description, minimum/branch/customer-group/effective prices, discount overrides, tax configuration, expiry/batches, optional wholesale pricing.
- Supplier management, purchase orders/invoices, partial receipts, GRNI, recoverable/nonrecoverable tax, landed costs, supplier balances/payments/returns.
- POS, customer/credit-limit management, sales and payment snapshots, split/cross-currency payments, receipts/PDF/thermal printing, sales history, cancellations/refunds/returns.
- Double-entry accounting, inventory costing/valuation, COGS, settlement FX, expenses and financial reports.
- Cashier shifts, cash movements, reconciliation and approval.
- Employee invitation delivery and profile-name/email editing, richer permission configuration, product-level low-stock thresholds/alerts, ledger pagination/export.
- Real-account end-to-end tests for the new UI and temporary-password flow. Existing HMS regression tests do not establish full UI coverage.

The next implementation should introduce immutable financial documents, valuation and balanced journal posting with isolated transaction tests, then build receiving, POS and settlement interfaces on those contracts. Do not represent the configuration/stock foundation as a completed sales or accounting product.
