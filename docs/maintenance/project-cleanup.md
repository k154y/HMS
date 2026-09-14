# Project cleanup — 13 September 2026

The active applications are `apps/api` and `apps/web`. Runtime code imports were checked before moving obsolete material.

Moved out of the active project structure:

- `apps/web-legacy`: unused copies of the previous Prisma routes.
- `apps/web/app/generated`: unused generated Prisma client.
- `apps/web/.next-before-css-fix`: old generated frontend cache.
- `apps/web/messages`, `lib/translate.ts`: superseded translation mechanism; the active source is `lib/i18n.ts`.
- `apps/web/lib/api.ts`, `lib/spring.ts`: unused direct-API helpers; the active helper is `lib/hms-api.ts`.
- `apps/api/bin`: unused IDE output and copied configuration.
- Empty `.github`, `infra/docker`, `infra/monitoring`, `infra/nginx` placeholders. The configured PostgreSQL initialization folder remains.

Automatic approval review rejected permanent bulk deletion. The cleanup therefore uses a recoverable archive under `.local/cleanup-2026-09-13`, preserving the original relative paths. Nothing in this archive is imported or built. To recover a file, copy its archived counterpart back to its original relative path after checking that it will not overwrite newer work.

The inventory is in `cleanup-manifest.json`. Local environment files, credentials, dependency installation, active build outputs, all Flyway migrations, tests and database volumes were retained. Test result snapshots are grouped in `docs/testing`; verification scripts were updated to match.

Unused frontend packages bcryptjs, next-intl and recharts were removed with npm, updating the lockfile and removing 67 packages. Frontend type checking and translation checks passed after cleanup. npm run typecheck and npm run check:translations are now explicit scripts.
