# Verification status

Updated 13 September 2026. This file separates implemented work from completed verification; it is not a production acceptance certificate.

## Passed checks

- Full backend suite against isolated PostgreSQL and Redis: 29 tests passed, no failures or skips (`apps/api/target/latest-tests.log`). Includes booking overlap, tenant isolation and ledger integrity cases.
- Simplified room request JSON regression: one additional test passed (`apps/api/target/room-contract-test.log`). This fixes omitted inherited occupancy fields rejected by Jackson.
- Frontend TypeScript check passed again after cleanup and removal of unused dependencies.
- Production Next.js build passed after cleanup: compilation, type validation and 30 generated pages.
- Translation checker passed with 389 keys across English, French and Kinyarwanda, including 215 literal UI keys.
- Expanded live demo workflow passed all 29 recorded checks, including purchases, POS, refunds, folio reuse, room inheritance/edit/archive guards, room-specific stay prices, food and room collection permissions, housekeeping, accountant product corrections, credit settlement and simultaneous accountant/auditor roles. Results: `testing/operational-test-results.json`.
- Browser verified styled login, English/French/Kinyarwanda switching, owner dashboard, room edit/delete controls, and automatic nightly-rate defaults from the selected room type.

## Implemented and covered by the checks above

- Room and room-type edit/archive controls, bed defaults, room-specific prices and reservation deletion guards.
- Stock product editing restricted by PRODUCT_MANAGE; immutable stock adjustment history and locked conversion units after activity.
- Multiple role checkboxes and aggregate permission navigation.
- Grouped customer portfolios with separate historical billing records; new orders reuse an existing open customer/resident folio.
- Payment collection scope for food/services versus room charges, approval by another authorised account, and refund ledger reversal.
- Session refresh, navigation-time session checks, and rejection of profiles without active hotel access.
- POS menu cards and a link to product/menu management.
- Shared UI spacing, typography, borders, forms and tables.

## Known verification limits

- An earlier check found the kitchen demo membership suspended. The latest read of the live system and the 14-account web run passed. Tests preserve account state and do not reactivate suspended users.
- Browser automation was temporarily blocked by automatic approval review reporting a usage limit; a fresh usage check allowed it to resume. Interactive checks resumed and confirmed session-expiry redirection, owner login, POS subtotal confirmation, order preview and French/Kinyarwanda behavior.
- The reference document also describes advanced table management, credit limits/due dates, separate credit request decisions, events/conference reservations, deposits, and reason/approval workflows for voids. These are not all implemented by the current basic customer/order/folio workflow and must not be described as finished.
- Current list endpoints with fixed limits need pagination before claiming complete historical reporting at scale.

## Folder cleanup

Only `apps/api` and `apps/web` remain in the active application structure. Unused legacy copies, generated Prisma client, stale cache, obsolete helpers and empty placeholders are in an ignored recovery archive. See `maintenance/project-cleanup.md`. Recorded test results belong in `testing/`.




