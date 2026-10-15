# Payment accounts, currencies, and password recovery

Implementation record, 3 October 2026. Local changes only; no production deployment or production database migration was performed. The inspection report was delivered before implementation was authorized.

## Delivered behavior

- Settings lets authorized administrators register separate accounts such as BK RWF, BK USD, Equity RWF, and mobile-money phone numbers or merchant codes. Account identifiers remain strings, including leading zeroes. Accounts can be renamed or deactivated; changing their currency, method, or identifier requires a new account.
- Customer payments, reservation advances, supplier payments, and expenses can identify the receiving/paying account. Accounts must belong to the hotel and match the method and currency. Posted transactions retain account-name and identifier snapshots after account changes.
- Settings manages enabled currencies alongside the existing effective-dated exchange-rate editor. Rates mean **one unit of foreign currency equals this many units of the hotel's accounting currency**. For an RWF hotel, USD rate 1,500 means USD 2 = RWF 3,000. The accounting currency is not changed by this feature.
- Products have separate buying and selling currencies. Orders, purchase invoices, supplier payments, and expenses preserve original currency, original amount/price, conversion rate, and accounting amounts. Existing amounts remain unchanged. New sales require an applicable rate; products without a usable conversion remain editable but cannot be selected in POS.
- Supplier payments preserve the invoice liability at its historical conversion and separately record actual cash converted at payment time and the exchange difference. Cash reports use actual cash; payable balances use the amount applied against the invoice. Account cash movements are reported in hotel accounting currency, grouped by account and transaction currency. These movements are not bank balances or reconciliation.
- Login links to forgot-password. Token-based reset, authenticated password change, and staff recovery actions use the existing API proxy and authentication architecture. A separate `/security` page supports signed-in users without requiring hotel selection.

## Authentication and security decisions

Login still verifies the current password with the injected Spring Security encoder, issues the existing HS256 JWT, and creates a hashed refresh session. Refresh rotation and logout remain in the existing services. No JWT implementation, signing key, password encoding policy, or existing password was replaced.

The existing DelegatingPasswordEncoder continues encoding new passwords with PBKDF2 SHA-256 (600,000 iterations, 16-byte salt), retaining existing legacy password support. The existing password policy is reused. Change-password verifies the current password; identity comes from the authenticated principal. Optional confirmation preserves existing API clients, while the new UI supplies confirmation.

Reset tokens use existing cryptographically secure random-token generation. Only SHA-256 digests are stored. Tokens expire after 20 minutes; user locking and atomic consumption allow one successful reset. Successful change/reset invalidates outstanding reset tokens and revokes every refresh session for the user. Identity is global, so this affects all of that user's hotel memberships. **Already issued access JWTs remain valid until expiry:** the application default is 60 minutes, with deployment examples configured for 15 minutes. No blacklist or token-version scheme was introduced.

Forgot-password returns the same generic successful response for existing, missing, or ineligible accounts. Existing Redis IP limiting is extended to recovery routes, with an additional normalized-email digest cooldown of 60 seconds. Disabled and indefinitely locked accounts cannot recover through this route. Tenant administration uses existing `USER_MANAGE` authorization and checks membership in the specified hotel; administrators never receive the token or choose the new password.

Currency configuration uses existing `EXCHANGE_RATE_MANAGE`; account configuration uses `HOTEL_SETTINGS_MANAGE`; configuration reads use `BRANCH_VIEW`. Existing transaction and report permissions remain applicable. Composite foreign keys also prevent attaching another hotel's account. Account omission remains accepted for legacy API clients and historical transactions, which reports label as unspecified; the new UI requires a matching configured account when available.

Existing audit infrastructure records reset requests, admin requests, completion/failure, password change, and session revocation. Passwords, hashes, raw reset tokens, and session credentials are excluded. Recovery request DTOs and delivery events redact sensitive `toString()` output.

Recovery uses configurable SMTP because inspection found no existing delivery service. Messages are dispatched asynchronously after transaction commit. The configured frontend URL must be trusted HTTPS. Links carry tokens in the URL fragment, which the reset screen clears and holds only in component memory. Provider exception details are not logged. Recovery is disabled by default.

## Migrations

Only new forward migrations were added; no previously applied migration was edited:

- `V30__password_recovery.sql`: dedicated hashed, expiring, single-use reset-token records and indexes.
- `V31__hotel_currencies_and_payment_accounts.sql`: hotel currency catalog, account configuration, and account snapshots/tenant foreign keys on financial transactions. Seeds existing hotel base currencies and previously configured exchange currencies.
- `V32__sales_and_purchase_currency_snapshots.sql`: product buying/selling currencies, original currency/price/rate snapshots, and supplier payment cash/exchange-difference values. Existing product currency defaults to hotel base; historical monetary values are not revalued.

The isolated PostgreSQL test database successfully applied these migrations through Spring Boot Flyway startup, including default migration validation on subsequent startup. Review migration locking/backfill duration against a staging copy before production.

## API contract

Added backend endpoints:

| Method | Path | Purpose |
| --- | --- | --- |
| POST | `/api/v1/auth/forgot-password` | Generic recovery request |
| POST | `/api/v1/auth/reset-password` | Consume token and set password |
| POST | `/api/v1/hotels/{hotel}/memberships/{membership}/password-recovery` | Authorized staff recovery |
| GET / POST | `/api/v1/hotels/{hotel}/payment-accounts` | List/create hotel accounts |
| PUT | `/api/v1/hotels/{hotel}/payment-accounts/{id}` | Rename/activate/deactivate account |
| GET | `/api/v1/hotels/{hotel}/currencies` | List configured currencies |
| GET | `/api/v1/hotels/{hotel}/currencies/base` | Read accounting currency |
| PUT | `/api/v1/hotels/{hotel}/currencies/{code}` | Configure currency |

The existing authenticated `/api/v1/auth/password` endpoint gains optional confirmation. Existing product, purchase, expense, and payment requests gain optional currency/account fields; responses expose snapshots and converted values. Accounting reports add `paymentAccounts` and supplier exchange details. Existing login, refresh, logout, password, exchange-rate, and operational endpoint paths are preserved. Legacy constructors and omitted-field behavior remain supported.

The Next.js proxy exposes narrow POST `/api/account/forgot-password`, `/api/account/reset-password`, and `/api/account/password` routes. These avoid NextAuth's `/api/auth/*` namespace, verify request origin, and use the existing server-side authentication context. Hotel resources continue through the existing hotel-scoped proxy. Signing out also revokes the server-side refresh session.

## Verification

- Backend compilation and test compilation: passed with Java 21.
- Initial full backend suite: 55 tests; 53 passed, with one invalid new test fixture and one changed legacy exception contract failing. Both were corrected. The fixture now uses a valid nonresident restaurant bill; missing-rate validation preserves the established exception contract.
- Targeted regression rerun: **25 tests passed, zero failures/errors/skips** across `FinancialConfigurationTest`, `PasswordRecoveryTest`, `PaymentFxWorkflowTest`, and `OperationsTest`. Together with unaffected passing full-suite results, all 55 test cases have passing results. This was not a second full-suite run.
- Recovery coverage includes change-password success/wrong-current/policy/confirmation, generic existing-versus-missing HTTP responses, valid/invalid/expired/used tokens, concurrent consumption, old/new login behavior, refresh revocation, disabled-account handling, authorized/unauthorized/cross-hotel recovery, and audit secret exclusion.
- Financial coverage includes tenant/method/currency/account validation, mobile merchant identifiers, inactive accounts, frozen account/rate snapshots and idempotency, foreign-currency product sales, purchase liabilities versus actual settlement cash, and expense snapshots.
- Frontend production build: **passed using project-pinned Next.js 16.3.5 in Docker**, including TypeScript and all 33 routes. A separate ignored output directory protected the development build. Generated TypeScript configuration changes were restored.
- Translation check: **passed**, 668 keys across all three languages and 430 literal UI keys.
- No actual SMTP delivery or interactive browser journey was exercised. These remain staging acceptance checks. No production passwords or live hotel data were used in tests.

Reproducible backend check using the existing isolated Compose services:

```powershell
docker compose -f docker-compose.dev.yml -f docker-compose.test.yml up -d test-postgres test-redis
docker compose -f docker-compose.dev.yml exec -T -e JAVA_HOME=/usr/lib/jvm/msopenjdk-current -e HMS_TEST_DB_URL=jdbc:postgresql://test-postgres:5432/hms_test -e HMS_TEST_DB_USER=hms_test -e HMS_TEST_DB_PASSWORD=hms_test_only -e HMS_TEST_REDIS_HOST=test-redis app mvn -B -ntp -f apps/api/pom.xml test
node scripts/check-translations.cjs
```

## Manual staging and deployment steps

1. Review the diff and new migrations. Back up the target database and verify restoration procedures. First use a staging copy with representative existing hotel data, keeping production credentials out of local tests.
2. Build the API and frontend using the repository's existing pinned dependencies and deployment workflow. Do not change JWT secrets, password configuration, refresh-session settings, or hotel accounting currency.
3. In staging, run the new API with its normal Flyway startup. Confirm successful validation and V30-V32 migration history before routing traffic. Compare existing invoices, payments, expenses, and balances before/after migration.
4. Configure `HMS_FRONTEND_URL` to the public HTTPS frontend origin, `HMS_MAIL_FROM`, and `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`. The example files include authenticated STARTTLS and connection/read/write timeouts. Configure the provider and sender appropriately. Set `HMS_PASSWORD_RECOVERY_ENABLED=true` only when ready to verify delivery.
5. Verify forgot/reset and staff recovery using controlled test mailboxes. Check link origin, expiration, single-use behavior, old-password rejection, new-password login, and revoked refresh sessions. Confirm a normal existing account can log in, refresh, and log out, and that hotel roles and boundaries still apply. Check reverse-proxy/application logging excludes request bodies and credentials.
6. Configure separate bank/mobile accounts and currencies in Settings. Set effective exchange rates. Exercise a foreign-currency sale, receipt approval, reservation advance, purchase, supplier payment after a rate change, and expense. Check the account and original currency on the transaction and reconcile report cash movements. Verify disabled accounts cannot receive new payments.
7. After staging approval, deploy backend/migrations before the new frontend using the established release procedure. Monitor Flyway status, authentication failures, email delivery failures, and financial reports. Do not drop the additive columns/tables as an application rollback; restore or roll forward using the approved operational procedure.

Remaining operational limits: SMTP delivery is asynchronous and has no durable queue/retry mechanism; failures emit a fixed log message and users can request a new link after cooldown. Disabling recovery prevents new recovery requests; no immediate JWT revocation was added. Legacy clients may omit accounts. Exchange rates are administrator supplied, not fetched from a live provider. Browser and real-mail acceptance are still required before production release.

## File inventory

All paths below are relative to the repository root. The exact modified/created file inventory follows, together with the principal inspected dependencies that were left unchanged.

### Modified files (also inspected)

- `.env.example`
- `apps/api/pom.xml`
- `apps/api/src/main/java/com/hotelmanagement/hms/identity/authentication/config/SecurityConfiguration.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/identity/authentication/dto/ChangePasswordRequest.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/identity/authentication/service/PasswordChangeService.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/identity/authentication/web/AuthenticationRateLimitFilter.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/order/model/OrderItem.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/order/service/OrderService.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/payment/dto/PaymentRequest.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/payment/dto/PaymentResponse.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/payment/model/Payment.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/payment/service/PaymentWorkflow.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/platform/currency/service/ExchangeRateService.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/product/dto/ProductRequest.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/product/dto/ProductResponse.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/product/model/Product.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/product/service/ProductService.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/purchase/dto/PurchaseOrderRequest.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/purchase/model/PurchaseOrder.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/purchase/model/PurchaseOrderItem.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/purchase/service/PurchaseOrderService.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/report/web/AccountingReportController.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/report/web/ExpenseController.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/report/web/ReportController.java`
- `apps/api/src/main/resources/application.yml`
- `apps/web/app/(auth)/login/page.tsx`
- `apps/web/app/(dashboard)/Sidebar.tsx`
- `apps/web/app/(dashboard)/cashier/page.tsx`
- `apps/web/app/(dashboard)/expenses/page.tsx`
- `apps/web/app/(dashboard)/folios/[id]/page.tsx`
- `apps/web/app/(dashboard)/menu/page.tsx`
- `apps/web/app/(dashboard)/pos/page.tsx`
- `apps/web/app/(dashboard)/purchasing/page.tsx`
- `apps/web/app/(dashboard)/reports/page.tsx`
- `apps/web/app/(dashboard)/reservations/page.tsx`
- `apps/web/app/(dashboard)/settings/page.tsx`
- `apps/web/app/(dashboard)/staff/page.tsx`
- `apps/web/app/(dashboard)/stock/page.tsx`
- `apps/web/app/api/[...path]/route.ts`
- `apps/web/components/operations/MasterData.tsx`
- `apps/web/components/operations/ProductEditor.tsx`
- `apps/web/components/operations/fields.ts`
- `apps/web/lib/auth.ts`
- `apps/web/lib/i18n.ts`
- `deployment/production/env.example`
- `deployment/staging/env.example`

### Created files

- `apps/api/src/main/java/com/hotelmanagement/hms/identity/authentication/config/PasswordRecoveryProperties.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/identity/authentication/password/PasswordRecoveryNotifier.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/identity/authentication/service/PasswordRecoveryService.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/identity/authentication/web/PasswordRecoveryController.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/payment/service/PaymentAccountService.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/payment/web/PaymentAccountController.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/platform/currency/service/HotelCurrencyService.java`
- `apps/api/src/main/resources/db/migration/V30__password_recovery.sql`
- `apps/api/src/main/resources/db/migration/V31__hotel_currencies_and_payment_accounts.sql`
- `apps/api/src/main/resources/db/migration/V32__sales_and_purchase_currency_snapshots.sql`
- `apps/api/src/test/java/com/hotelmanagement/hms/FinancialConfigurationTest.java`
- `apps/api/src/test/java/com/hotelmanagement/hms/PasswordRecoveryTest.java`
- `apps/web/app/(auth)/forgot-password/page.tsx`
- `apps/web/app/(auth)/reset-password/page.tsx`
- `apps/web/app/security/page.tsx`
- `apps/web/components/operations/FinanceSettings.tsx`
- `apps/web/components/operations/MoneyFields.tsx`
- `apps/web/components/operations/PasswordForm.tsx`
- `docs/implementation/payment-accounts-currencies-password-recovery.md`

### Additional inspected dependencies

- `apps/api/src/main/java/com/hotelmanagement/hms/identity/repository/UserRepository.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/identity/model/UserAccount.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/identity/model/UserStatus.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/identity/authentication/config/PasswordConfiguration.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/identity/authentication/config/JwtConfiguration.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/identity/authentication/config/AuthenticationProperties.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/identity/authentication/password/PasswordPolicy.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/identity/authentication/jwt/JwtTokenService.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/identity/authentication/jwt/JwtProperties.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/identity/authentication/service/AuthenticationService.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/identity/authentication/web/AuthenticationController.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/identity/authentication/web/PasswordController.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/identity/authentication/session/model/RefreshSession.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/identity/authentication/session/repository/RefreshSessionRepository.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/identity/authentication/session/service/RefreshSessionService.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/identity/authentication/session/security/RefreshTokenGenerator.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/identity/authorization/service/AuthorizationGuard.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/identity/authorization/service/TenantAuthorizationService.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/identity/authorization/model/PermissionCode.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/identity/membership/repository/HotelMembershipRepository.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/identity/membership/model/HotelMembership.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/audit/service/AuditService.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/audit/model/AuditEvent.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/audit/repository/AuditEventRepository.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/shared/service/OperationScope.java`
- `apps/web/lib/hms-api.ts`
- `docker-compose.dev.yml`
- `docker-compose.test.yml`
- `deployment/production/docker-compose.yml`
- `deployment/staging/docker-compose.yml`
- `.github/workflows/backend-image.yml`
- `apps/api/src/test/java/com/hotelmanagement/hms/BackendFoundationTest.java`
- `apps/api/src/test/java/com/hotelmanagement/hms/IntegrationServices.java`
- `apps/api/src/test/java/com/hotelmanagement/hms/JwtValidationTest.java`
- `apps/api/src/test/java/com/hotelmanagement/hms/OperationsTest.java`
- `apps/api/src/test/java/com/hotelmanagement/hms/PasswordPolicyTest.java`
- `apps/api/src/test/java/com/hotelmanagement/hms/PaymentFxWorkflowTest.java`
- `apps/api/src/test/java/com/hotelmanagement/hms/ReservationAdvancePaymentTest.java`
- `apps/api/src/test/java/com/hotelmanagement/hms/RoomRequestContractTest.java`
