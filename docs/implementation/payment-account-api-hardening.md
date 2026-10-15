# Payment-account API hardening

This pass hardens the existing feature without changing its accounting model. No commit or deployment was performed. Full account identifiers remain stored in `payment_accounts`, frozen approval payloads, and immutable payment, expense, and supplier-payment snapshots.

## Inspection and security changes

`PaymentController.create` still delegates POST `/payments` to `PaymentWorkflow.requestSingle`. That creates the same workflow request, which validates the hotel account, method, currency, and active state. No alternative posting route was added. Null account IDs remain supported.

One response-only utility, `PaymentAccountMasking`, handles all identifier masking. Null stays null; identifiers up to four characters are completely masked; longer identifiers expose only their final four Unicode code points. Empty strings return a mask rather than an empty unmasked value. Map responses are copied, not mutated. The utility neither logs nor writes identifiers to storage.

Payment-account list/create/update APIs return an explicit representation containing only id, name, method, currency, identifierType, maskedIdentifier, and active. The internal `Account` record retains the full identifier for validation and storage, with a redacted `toString()`. `BRANCH_VIEW` cannot retrieve the raw identifier from the list response.

Account updates lock the existing row under hotel scope and update only name/active. The browser supplies those fields without resending any identity data. Optional identity fields from older clients are checked against the stored row; changes are rejected. Deactivating or renaming an account still works if its currency has since been disabled. Existing permissions and audit events remain in use.

`PaymentResponse.paymentAccountIdentifier` retains its existing JSON name for compatibility, but its value is masked. Expense and transaction-report responses likewise retain their existing snake-case field names with masked values. Supplier payment detail uses an explicit column list and masks its identifier. Original amounts, currencies, rates, actual cash amounts, and exchange differences are unchanged.

Approval listing sanitizes a copy of the stored JSON and replaces the nested account identifier with `maskedIdentifier`. It keeps the payload string shape and all other fields, preserving decimal precision. Approval posting and legacy payload decoding still use the original frozen data; they do not re-resolve an account after rename/deactivation. The idempotent retry responses for approvals, expenses, and supplier payments are sanitized as well. The cashier transaction report was also covered because it previously exposed payment snapshots through `p.*`.

Finance Settings, payment account selectors, and cashier approval displays consume masked identifiers. Only creation asks for the complete identifier. Rename and activation requests contain just name/active.

## Database constraints

As explicitly permitted for unapplied V31, its checks now additionally require bank accounts to use ACCOUNT_NUMBER and mobile money to use PHONE_NUMBER or MERCHANT_CODE. The non-NONE check now explicitly rejects SQL NULL; the previous comparison could evaluate to UNKNOWN and pass a CHECK. CREDIT was already excluded. NONE still requires NULL, and non-NONE requires a nonblank value. No new CARD restriction was added. V32 and all other migration files were unchanged in this pass.

Tests use a fresh disposable `hms_hardening` database so V31 can be validated without altering migration history in an existing database. Environments that already applied an earlier local V31 must not silently repair production migration checksums; coordinate the unapplied release migration before deployment.

## Validation

- Targeted integration tests passed: FinancialConfigurationTest (7), PasswordRecoveryTest (9), PaymentFxWorkflowTest (7), and ReservationAdvancePaymentTest (6).
- PaymentAccountMaskingTest: 2 passed after correcting the new phone-number test's expected star count. The initial targeted run had 30 passes and this one test expectation failure; no existing test was weakened and masking logic was unchanged.
- Complete backend suite: **62 tests passed, zero failures/errors/skips**, BUILD SUCCESS (3 October 2026, 13:54 UTC).
- Production frontend build passed with pinned Next.js 16.3.5, including TypeScript and all 33 routes. Build-generated config changes were restored.
- Translation validation passed: 668 keys in all three languages, 430 literal UI keys.
- `git diff --check` passed.

Backend tests run through the existing Compose app with Java 21, `HMS_TEST_DB_URL=jdbc:postgresql://test-postgres:5432/hms_hardening`, test-only credentials, and `HMS_TEST_REDIS_HOST=test-redis`. Targeted Maven selection was `-Dtest=FinancialConfigurationTest,PasswordRecoveryTest,PaymentFxWorkflowTest,ReservationAdvancePaymentTest,PaymentAccountMaskingTest`; the complete run omits that selector. Frontend build uses `NEXT_OUTPUT_DIR=tmp/account-hardening-build npm run build` in the existing app container. No live financial data was used.

## Changed files in this pass

Paths are relative to the repository root. Earlier feature changes already present in the workspace are outside this list.

- `apps/api/src/main/java/com/hotelmanagement/hms/payment/service/PaymentAccountMasking.java` (new): centralized masking and copied map responses.
- `apps/api/src/main/java/com/hotelmanagement/hms/payment/service/PaymentAccountService.java`: safe public account representation, locked partial updates, redacted diagnostic representation.
- `apps/api/src/main/java/com/hotelmanagement/hms/payment/web/PaymentAccountController.java`: safe list/create/update responses.
- `apps/api/src/main/java/com/hotelmanagement/hms/payment/dto/PaymentResponse.java`: masked snapshot value under the existing field name.
- `apps/api/src/main/java/com/hotelmanagement/hms/payment/service/PaymentWorkflow.java`: sanitized approval list/retry payload copies.
- `apps/api/src/main/java/com/hotelmanagement/hms/purchase/service/PurchaseOrderService.java`: explicit vendor-payment fields and sanitized detail/retry output.
- `apps/api/src/main/java/com/hotelmanagement/hms/report/web/ExpenseController.java`: sanitized list/retry output.
- `apps/api/src/main/java/com/hotelmanagement/hms/report/web/ReportController.java`: sanitized cashier transaction output.
- `apps/api/src/main/resources/db/migration/V31__hotel_currencies_and_payment_accounts.sql`: checks matching existing account business rules.
- `apps/api/src/test/java/com/hotelmanagement/hms/FinancialConfigurationTest.java`: disclosure, snapshot preservation, API routing, update, compatibility, and constraint regressions.
- `apps/api/src/test/java/com/hotelmanagement/hms/PaymentAccountMaskingTest.java` (new): null, short, standard identifiers, and non-mutating responses.
- `apps/web/components/operations/FinanceSettings.tsx`: masked display and name/active-only updates.
- `apps/web/components/operations/MoneyFields.tsx`: safe account response type and masked selectors.
- `apps/web/app/(dashboard)/cashier/page.tsx`: sanitized approval account display.
- `docs/implementation/payment-account-api-hardening.md` (new): this report.

Additional requested files inspected but unchanged in this pass: `PaymentController.java`, `PaymentApprovalController.java`, `PaymentService.java`, `PaymentRequest.java`, and `Payment.java` in their existing payment packages. Their route, posting, validation, and full snapshot storage responsibilities remain intact.

## Requested git diff --stat

This is the workspace-wide tracked diff, including the earlier uncommitted feature implementation. Git excludes untracked new files from this output; the complete 15-file inventory for this hardening pass is listed above.

```text
 .env.example                                       | 14 ++++
 apps/api/pom.xml                                   |  1 +
 .../config/SecurityConfiguration.java              |  2 +
 .../authentication/dto/ChangePasswordRequest.java  |  3 +-
 .../service/PasswordChangeService.java             | 10 ++-
 .../web/AuthenticationRateLimitFilter.java         |  2 +-
 .../hotelmanagement/hms/order/model/OrderItem.java |  2 +-
 .../hms/order/service/OrderService.java            | 12 +--
 .../hms/payment/dto/PaymentRequest.java            |  2 +-
 .../hms/payment/dto/PaymentResponse.java           |  2 +-
 .../hotelmanagement/hms/payment/model/Payment.java |  2 +
 .../hms/payment/service/PaymentWorkflow.java       | 60 +++++++++++----
 .../currency/service/ExchangeRateService.java      |  5 +-
 .../hms/product/dto/ProductRequest.java            |  4 +-
 .../hms/product/dto/ProductResponse.java           |  5 +-
 .../hotelmanagement/hms/product/model/Product.java |  5 ++
 .../hms/product/service/ProductService.java        | 14 ++--
 .../hms/purchase/dto/PurchaseOrderRequest.java     |  2 +-
 .../hms/purchase/model/PurchaseOrder.java          |  2 +-
 .../hms/purchase/model/PurchaseOrderItem.java      |  2 +-
 .../hms/purchase/service/PurchaseOrderService.java | 26 +++++--
 .../hms/report/web/AccountingReportController.java | 20 ++++-
 .../hms/report/web/ExpenseController.java          | 47 ++++++------
 .../hms/report/web/ReportController.java           |  4 +-
 apps/api/src/main/resources/application.yml        |  4 +
 apps/web/app/(auth)/login/page.tsx                 |  2 +-
 apps/web/app/(dashboard)/Sidebar.tsx               |  1 +
 apps/web/app/(dashboard)/cashier/page.tsx          |  9 ++-
 apps/web/app/(dashboard)/expenses/page.tsx         |  7 +-
 apps/web/app/(dashboard)/folios/[id]/page.tsx      |  5 ++
 apps/web/app/(dashboard)/menu/page.tsx             |  7 +-
 apps/web/app/(dashboard)/pos/page.tsx              | 11 ++-
 apps/web/app/(dashboard)/purchasing/page.tsx       | 11 +--
 apps/web/app/(dashboard)/reports/page.tsx          |  6 +-
 apps/web/app/(dashboard)/reservations/page.tsx     |  5 +-
 apps/web/app/(dashboard)/settings/page.tsx         |  4 +
 apps/web/app/(dashboard)/staff/page.tsx            |  2 +-
 apps/web/app/(dashboard)/stock/page.tsx            |  1 +
 apps/web/app/api/[...path]/route.ts                | 16 ++++
 apps/web/components/operations/MasterData.tsx      |  3 +-
 apps/web/components/operations/ProductEditor.tsx   |  5 +-
 apps/web/components/operations/fields.ts           |  2 +-
 apps/web/lib/auth.ts                               |  1 +
 apps/web/lib/i18n.ts                               | 86 ++++++++++++++++++++++
 deployment/production/env.example                  | 17 ++++-
 deployment/staging/env.example                     | 17 ++++-
 46 files changed, 369 insertions(+), 101 deletions(-)
```
