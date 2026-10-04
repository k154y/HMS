# Reservation details, safe errors and production automation

Verification completed 2026-10-04 (Africa/Johannesburg). Implementation is local and uncommitted. No push, deployment, production SQL or GitHub/VPS configuration was performed.

## 1. Inspection

Read the complete reservation calendar page before editing, its controller, response DTO and service; the payment-summary contract; central frontend API client; Panel/form error display; existing room/product drawers and operational UI conventions. There was no reusable general-purpose modal/dialog component, so the new detail component uses the native accessible dialog and existing drawer styles.

Inspected `GlobalApiExceptionHandler`, `ApiError`, `ApiException`, correlation filter, authentication advice, security entry/denied handlers, reservation/payment validation DTOs and the business exception sites listed in the modified-file inventory below. Also inspected authentication/tenant scoping in the proxy, financial configuration and payment tests, and the existing error documentation.

Inspected all existing workflows (the backend-image workflow), production Compose and environment example, backend Dockerfile and inherited healthcheck, application production/default configuration, development Compose/test infrastructure, README/deployment documentation, frontend package/build configuration, and current V1–V32 migrations. No real environment secrets were copied into deliverables.

## 2–3. Modified and created files

The exact inventory is appended below. Broad backend file coverage is limited to replacing reviewed business-rule exception constructors with an explicitly safe exception type. It does not redesign those services or their calculations. Existing password-recovery/payment-account/currency work was preserved. Applied migrations were not edited; no new schema migration is needed for this request.

## 4. Reservation calendar

Reserved cells with actual reservations are underlined, keyboard-operable buttons with reservation/room/date accessible names. Each opening fetches the existing reservation-by-ID and payment-summary endpoints. Every occupied day uses the same reservation ID; no creation endpoint is called. Available links and operationally unavailable cells retain their existing behavior.

The right drawer displays reference, customer, assigned rooms, dates, adults/children, status, notes, server-calculated totals/advance/remaining/payment status, and a folio link. Loading and error states are accessible. Native modal focus handling, autofocus Close, Escape and focus restoration were verified. A stale request cannot update a closed/replaced drawer. Payment-summary failure can display separately from successfully fetched reservation details.

Customer and room display names come from the page's existing lookups, with identifier fallback; the actual reservation and financial summary are freshly fetched. The browser formats money but does not recompute payment balances.

## 5. Error contract

The backend continues to use `ApiError`: `timestamp`, `status`, `code`, `message`, `path`, `requestId`, `fieldErrors`. Successful API response shapes remain unchanged. Existing client-supplied safe `X-Request-ID` values are reused; missing/invalid values receive a generated ID.

| Situation | HTTP/code | User behavior |
| --- | --- | --- |
| Bean validation | 400 / VALIDATION_FAILED | “Please correct the highlighted fields.” plus safe field messages |
| Unclassified input/parsing error | 400 / VALIDATION_FAILED | Correct entered information; never raw exception text |
| Explicit safe domain error | Existing appropriate 4xx / domain code | Reviewed corrective message |
| Overpayment | 409 / PAYMENT_EXCEEDS_BALANCE | “This payment exceeds the outstanding balance.” |
| Inactive account | 409 / PAYMENT_ACCOUNT_INACTIVE | “This account is inactive. Choose another payment account.” |
| Authentication | 401 / authentication code | Session expired/invalid; existing sign-in redirect retained |
| Permission | 403 / ACCESS_DENIED | “You don't have permission to perform this action.” |
| Missing resource, including NoResourceFoundException | 404 / RESOURCE_NOT_FOUND | Record unavailable message |
| Concurrency/data conflict | 409 / STATE_CONFLICT | Refresh and review changes; no SQL/constraint disclosure |
| Booking exclusion constraint | 409 / ROOM_UNAVAILABLE | Room no longer available for selected dates |
| Unexpected failure | 500 / INTERNAL_ERROR | System problem; contact administrator with reference ID |

Example validation fields: `customerId: Customer is required.`, `checkIn: Check-in date is required.`, `checkOut: Check-out date is required.` Payment amount Bean Validation says “Amount must be greater than zero.” Date-order validation uses the explicit `INVALID_STAY_DATE_RANGE` code and “Check-out must be after check-in.”

`BusinessRuleException` explicitly marks reviewed messages safe and extends `IllegalStateException`, preserving existing internal caller/test compatibility. Unclassified illegal-state failures are technical faults, not automatically exposed as business messages. No arbitrary IllegalArgumentException/SQL exception message is returned.

Unexpected responses use: “We couldn't complete this action because of a system problem. Please contact the system administrator and provide reference ID: <id>.” The same ID is logged with the exception type and bounded stack locations. Arbitrary exception messages, causes, request bodies and throwable dumps are deliberately excluded from this handler's log event to avoid secrets. Technical 503 domain statuses retain their status with a generic system message; they are not mislabeled as client errors.

The frontend `HmsApiError` preserves status, code, `safeMessage`, `fieldErrors` and `requestId`; its conventional `message` also includes field messages so existing Panel/forms display useful corrections. Proxies propagate request IDs and redact upstream 5xx payloads. Network failures get a safe connection message and reference. Existing authentication and hotel/branch routing remain in place.

## 6–8. Tests and results

| Check | Result |
| --- | --- |
| Targeted backend tests | 24 passed; 0 failures/errors/skips |
| Complete Maven backend suite | 66 passed; 0 failures/errors/skips; BUILD SUCCESS |
| Flyway validation | Explicit `Flyway.validate()` passed against disposable `hms_hardening`; V1–V32 available |
| Migration file integrity | All 32 files byte-for-byte unchanged from task-start snapshot; normalized checksums also pass offline guard |
| Frontend production build | Next 16.3.5 build passed, TypeScript passed, 33/33 pages generated; existing routes build |
| Frontend error client tests | 5 passed |
| Translation check | 673 keys in all three languages; 434 literal UI keys checked |
| Offline deployment tests | 6 passed, including YAML parse and Bash syntax checks |
| Browser acceptance | Fresh details/summary, loading, Enter, Escape/focus restoration, Close, multiple covered days, error state, available link and noninteractive unavailable cells verified |
| Repository whitespace check | `git diff --check` passed |

Backend verification used the existing development container only after verifying Docker and disposable PostgreSQL/Redis access. No production database was used. The final full run completed at 2026-10-03T22:14:07Z. It took about 66 minutes on this machine. Build-generated TypeScript configuration changes were restored to their original bytes; generated output stays ignored under `apps/web/tmp/production-improvements-build`.

Added `ApiErrorContractTest` exercises required fields, explicit business errors, 401/403/404/resource-404/conflict/500, response/log reference matching and secret-marker suppression. Financial configuration tests now explicitly validate Flyway and the inactive-account code. Payment workflow tests assert the safe overpayment message/code while retaining existing behavior assertions.

`scripts/test-web-errors.cjs` runs the real API client through Node's test runner with mocked fetch. `scripts/test-production-deployment.py` checks actual YAML and executes the real deployment script with fake Docker/curl/flock commands in temporary directories. It verifies exact SHA, only-tag environment updates, private backup mode, API-only recreation, rejection of wrong image revision before update, lock failures, container/public health failures, safe logs and absence of automatic rollback. It never connects to the VPS.

`scripts/preview-reservation-calendar.cjs` bundles the real page/drawer against mock API/session/locale dependencies and serves it on an ephemeral loopback port. It is a repeatable manual acceptance fixture, not a production route or a CI browser test. Browser verification used fake room/customer/payment data, without live hotel records. Screenshot is saved locally at `tmp/reservation-calendar-detail.png` (ignored).

## 9–12. Deployment architecture and setup

See [production deployment runbook](../../deployment/production/README.md) for exact secret names, Environment settings, one-time VPS requirements, release sequence and recovery. The backend test job runs deployment safety checks. The image job requires both backend and frontend verification and publishes the exact full Git SHA tag/revision. The deployment job requires test/image success, uses Environment `production`, and is gated by exactly `github.event_name == 'push' && github.ref == 'refs/heads/main'`.

PR/yfeature/devs/manual-dispatch deployment is excluded and tested. GitHub concurrency plus remote flock prevent overlap. Strict SSH host verification is required. The remote script backs up `.env`, pulls/verifies the revision, updates only the image tag, recreates only API, then verifies container health, running revision and public health. Flyway runs through normal startup. No migration SQL or repair command is added.

Required secrets: `HMS_PROD_HOST`, `HMS_PROD_USER`, `HMS_PROD_SSH_PRIVATE_KEY`, `HMS_PROD_KNOWN_HOSTS`, optional `HMS_PROD_PORT`. Required Environment variable: `HMS_PROD_HEALTH_URL`. No current VPS username/domain is assumed. After one-time setup and your reviewed merge to main, ordinary backend releases require no manual SSH commands.

## 13. Remaining risks and limits

Actual GitHub Actions execution, GHCR permissions, SSH credentials, VPS permissions, public health routing and production startup have not been exercised because deployment is explicitly out of scope. The first configured release needs operator observation. One API instance is recreated, so short downtime is possible. Startup/migrations exceeding the health bound fail visibly. No automatic rollback is attempted after potentially irreversible migrations.

This workflow deploys the backend; the calendar/client frontend changes still need the existing frontend hosting release. The safe-error tests verify representative central-handler paths and existing integration suites, not every operational UI form. Production disables Hibernate's raw JDBC error loggers because database rejection details can contain full account identifiers or other submitted values; the offline checks verify those settings. Access to server logs must remain restricted. The handler and deployment log filter do not dump secrets or raw exception/SQL text.

Future schema migrations must be sequential V33+, immutable after application, forward-only, tested and preferably additive/backward-compatible. The applied-migration manifest is a review guard, not protection against an authorized contributor deliberately altering both SQL and manifest. Environment backups contain secrets and require restricted retention; they do not replace database backups.

## 14. Git status

The final snapshot is appended below. All changes remain unstaged/uncommitted by this task. Earlier implementation documents and unrelated existing files were preserved. Real `.env`, generated `.next` output and `.prod-repair-src` are excluded from release candidates; private scratch/build outputs are also excluded from Docker context. No commit, push or deployment command was run.

## File inventory

Modified (50):

- `.dockerignore`
- `.github/workflows/backend-image.yml`
- `.gitignore`
- `apps/api/src/main/java/com/hotelmanagement/hms/credit/service/CreditService.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/folio/model/Folio.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/folio/service/FolioService.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/housekeeping/service/HousekeepingTaskService.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/identity/authentication/web/AuthenticationApiExceptionHandler.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/identity/authentication/web/RestAccessDeniedHandler.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/identity/authentication/web/RestAuthenticationEntryPoint.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/identity/authorization/service/RoleAuthorizationService.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/identity/membership/service/HotelMembershipService.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/identity/service/UserAccountService.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/inventory/service/InventoryService.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/maintenance/model/MaintenanceTicket.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/order/model/Order.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/order/service/OrderService.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/payment/dto/PaymentRequest.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/payment/model/CashierShift.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/payment/model/Payment.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/payment/service/PaymentAccountService.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/payment/service/PaymentService.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/payment/service/PaymentWorkflow.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/platform/currency/service/ExchangeRateService.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/platform/service/BranchService.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/platform/service/PlatformService.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/product/service/ProductService.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/purchase/model/PurchaseOrder.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/purchase/service/PurchaseOrderService.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/report/web/ExpenseController.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/reservation/dto/ReservationRequest.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/reservation/dto/ReservationUpdateRequest.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/reservation/model/Reservation.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/reservation/model/ReservationRoom.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/reservation/service/ReservationService.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/room/service/RoomService.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/shared/web/ApiError.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/shared/web/ApiException.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/shared/web/GlobalApiExceptionHandler.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/shared/web/RequestCorrelationFilter.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/stay/model/Stay.java`
- `apps/api/src/main/java/com/hotelmanagement/hms/stay/service/StayService.java`
- `apps/api/src/main/resources/application-prod.yml`
- `apps/api/src/test/java/com/hotelmanagement/hms/FinancialConfigurationTest.java`
- `apps/api/src/test/java/com/hotelmanagement/hms/PaymentFxWorkflowTest.java`
- `apps/web/app/(dashboard)/reservations/page.tsx`
- `apps/web/app/api/[...path]/route.ts`
- `apps/web/app/api/platform/hotels/route.ts`
- `apps/web/lib/hms-api.ts`
- `apps/web/lib/i18n.ts`

Created (11):

- `apps/api/src/main/java/com/hotelmanagement/hms/shared/web/BusinessRuleException.java`
- `apps/api/src/test/java/com/hotelmanagement/hms/ApiErrorContractTest.java`
- `apps/web/components/operations/ReservationDetails.tsx`
- `apps/web/lib/api-proxy-errors.ts`
- `deployment/migrations-v1-v32.sha256`
- `deployment/production/deploy.sh`
- `deployment/production/README.md`
- `docs/implementation/reservation-errors-production-deployment.md`
- `scripts/preview-reservation-calendar.cjs`
- `scripts/test-production-deployment.py`
- `scripts/test-web-errors.cjs`

Final working-tree snapshot:

```text
 M .dockerignore
 M .github/workflows/backend-image.yml
 M .gitignore
 M apps/api/src/main/java/com/hotelmanagement/hms/credit/service/CreditService.java
 M apps/api/src/main/java/com/hotelmanagement/hms/folio/model/Folio.java
 M apps/api/src/main/java/com/hotelmanagement/hms/folio/service/FolioService.java
 M apps/api/src/main/java/com/hotelmanagement/hms/housekeeping/service/HousekeepingTaskService.java
 M apps/api/src/main/java/com/hotelmanagement/hms/identity/authentication/web/AuthenticationApiExceptionHandler.java
 M apps/api/src/main/java/com/hotelmanagement/hms/identity/authentication/web/RestAccessDeniedHandler.java
 M apps/api/src/main/java/com/hotelmanagement/hms/identity/authentication/web/RestAuthenticationEntryPoint.java
 M apps/api/src/main/java/com/hotelmanagement/hms/identity/authorization/service/RoleAuthorizationService.java
 M apps/api/src/main/java/com/hotelmanagement/hms/identity/membership/service/HotelMembershipService.java
 M apps/api/src/main/java/com/hotelmanagement/hms/identity/service/UserAccountService.java
 M apps/api/src/main/java/com/hotelmanagement/hms/inventory/service/InventoryService.java
 M apps/api/src/main/java/com/hotelmanagement/hms/maintenance/model/MaintenanceTicket.java
 M apps/api/src/main/java/com/hotelmanagement/hms/order/model/Order.java
 M apps/api/src/main/java/com/hotelmanagement/hms/order/service/OrderService.java
 M apps/api/src/main/java/com/hotelmanagement/hms/payment/dto/PaymentRequest.java
 M apps/api/src/main/java/com/hotelmanagement/hms/payment/model/CashierShift.java
 M apps/api/src/main/java/com/hotelmanagement/hms/payment/model/Payment.java
 M apps/api/src/main/java/com/hotelmanagement/hms/payment/service/PaymentAccountService.java
 M apps/api/src/main/java/com/hotelmanagement/hms/payment/service/PaymentService.java
 M apps/api/src/main/java/com/hotelmanagement/hms/payment/service/PaymentWorkflow.java
 M apps/api/src/main/java/com/hotelmanagement/hms/platform/currency/service/ExchangeRateService.java
 M apps/api/src/main/java/com/hotelmanagement/hms/platform/service/BranchService.java
 M apps/api/src/main/java/com/hotelmanagement/hms/platform/service/PlatformService.java
 M apps/api/src/main/java/com/hotelmanagement/hms/product/service/ProductService.java
 M apps/api/src/main/java/com/hotelmanagement/hms/purchase/model/PurchaseOrder.java
 M apps/api/src/main/java/com/hotelmanagement/hms/purchase/service/PurchaseOrderService.java
 M apps/api/src/main/java/com/hotelmanagement/hms/report/web/ExpenseController.java
 M apps/api/src/main/java/com/hotelmanagement/hms/reservation/dto/ReservationRequest.java
 M apps/api/src/main/java/com/hotelmanagement/hms/reservation/dto/ReservationUpdateRequest.java
 M apps/api/src/main/java/com/hotelmanagement/hms/reservation/model/Reservation.java
 M apps/api/src/main/java/com/hotelmanagement/hms/reservation/model/ReservationRoom.java
 M apps/api/src/main/java/com/hotelmanagement/hms/reservation/service/ReservationService.java
 M apps/api/src/main/java/com/hotelmanagement/hms/room/service/RoomService.java
 M apps/api/src/main/java/com/hotelmanagement/hms/shared/web/ApiError.java
 M apps/api/src/main/java/com/hotelmanagement/hms/shared/web/ApiException.java
 M apps/api/src/main/java/com/hotelmanagement/hms/shared/web/GlobalApiExceptionHandler.java
 M apps/api/src/main/java/com/hotelmanagement/hms/shared/web/RequestCorrelationFilter.java
 M apps/api/src/main/java/com/hotelmanagement/hms/stay/model/Stay.java
 M apps/api/src/main/java/com/hotelmanagement/hms/stay/service/StayService.java
 M apps/api/src/main/resources/application-prod.yml
 M apps/api/src/test/java/com/hotelmanagement/hms/FinancialConfigurationTest.java
 M apps/api/src/test/java/com/hotelmanagement/hms/PaymentFxWorkflowTest.java
 M apps/web/app/(dashboard)/reservations/page.tsx
 M apps/web/app/api/[...path]/route.ts
 M apps/web/app/api/platform/hotels/route.ts
 M apps/web/lib/hms-api.ts
 M apps/web/lib/i18n.ts
?? apps/api/src/main/java/com/hotelmanagement/hms/shared/web/BusinessRuleException.java
?? apps/api/src/test/java/com/hotelmanagement/hms/ApiErrorContractTest.java
?? apps/web/components/operations/ReservationDetails.tsx
?? apps/web/lib/api-proxy-errors.ts
?? deployment/migrations-v1-v32.sha256
?? deployment/production/README.md
?? deployment/production/deploy.sh
?? docs/implementation/payment-account-api-hardening.md
?? docs/implementation/payment-accounts-currencies-password-recovery.md
?? docs/implementation/reservation-errors-production-deployment.md
?? scripts/preview-reservation-calendar.cjs
?? scripts/test-production-deployment.py
?? scripts/test-web-errors.cjs
```
