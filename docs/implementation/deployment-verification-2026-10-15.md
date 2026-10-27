# Deployment verification — 2026-10-15

Baseline: branch `yfeature`, commit `8b5f94b`. Tests include the existing uncommitted changes; this is not verification of the committed tree alone.

## Results

| Check | Result |
| --- | --- |
| Backend Java and test compilation | Passed |
| Complete Maven test suite | 131 passed, 0 failures, 0 errors, 0 skipped |
| Frontend regression tests, including CSV export | 30 passed, 0 failed |
| Standalone TypeScript | Passed |
| Offline deployment safety | 6 passed |
| Translation validation | Failed: missing translation keys and untranslated UI text |
| Frontend production build | Passed with one worker; initial seven-worker attempt failed collecting page data with ENOMEM |

Deployment is blocked by translation validation, which is required by the existing CI frontend job. Missing keys include `Roles`, `Staff roles (select more than one)`, `Create product`, `Stock operations`, `Select registered product`, `Add product from catalog`, `Print receipt`, and `Print invoice`. The checker also reports untranslated text in product forms, goods receiving, invoices, login and other screens. These failures were not suppressed or repaired in this verification task.

## Commands

From the repository root:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/test-hms-consistency.ps1 -ContainerBuild -Tests '*Test'
docker exec -w /workspace hotel_hms_app node --test scripts/test-workspace-navigation.cjs scripts/test-commerce-review.cjs scripts/test-web-errors.cjs scripts/test-report-export.cjs
docker exec -w /workspace/apps/web hotel_hms_app npm run check:translations
docker exec -w /workspace/apps/web hotel_hms_app npx --no-install tsc --noEmit
docker exec -e NEXT_OUTPUT_DIR=.next-build -w /workspace/apps/web hotel_hms_app npm run build
docker exec -e NEXT_OUTPUT_DIR=.next-build -e CIRCLE_NODE_TOTAL=2 -w /workspace/apps/web hotel_hms_app npm run build
```

The retry uses Next.js's existing `CIRCLE_NODE_TOTAL` setting to select one build worker without editing application configuration. Build-generated `tsconfig.json` and `next-env.d.ts` changes are restored from their pre-build bytes.

Offline deployment tests ran in the existing `python:3.12-slim` image, with networking disabled and a read-only repository mount:

```powershell
python -m pip install --target "$env:TEMP/kbp-deployment-python-20261015" PyYAML==6.0.2
docker run --rm --pull never --network none -v "${PWD}:/workspace:ro" -v "${env:TEMP}/kbp-deployment-python-20261015:/test-python:ro" -e PYTHONPATH=/test-python -w /workspace python:3.12-slim python scripts/test-production-deployment.py
```

PyYAML was installed only into a temporary test directory, not project dependencies. Earlier attempts using the app container's minimal Python failed because it lacked venv/standard-library modules; the disposable Python image completed all six tests successfully. Deployment commands are mocked by this test harness.

## Test-environment repairs

The first backend attempt failed before Maven because RabbitMQ could not read its Erlang cookie (`eacces`). The test-only RabbitMQ container now runs as its existing `100:101` account, matching its tmpfs ownership. The next attempt started RabbitMQ but its health check timed out under load. The RabbitMQ health timeout was increased from 5 to 30 seconds and the disposable-service wait from 120 to 300 seconds. The subsequent complete suite passed. Existing edits in these files were preserved.

## Files changed by this task

- `apps/web/components/WorkspaceNavigation.tsx`: remove unused link import and avoid rendering an empty signed-in navigation element; the main Back to platform link was already removed in the current baseline.
- `apps/web/components/WorkspaceAccessState.tsx`: remove the remaining Back to platform link from workspace-access screens.
- `docker-compose.test.yml`: RabbitMQ test user and health timeout only.
- `scripts/test-hms-consistency.ps1`: disposable-service readiness timeout only.
- This report.

Pre-existing Java security/error-handler/test edits and backup files were preserved. No production business logic or migrations were changed. No development services were restarted; development and production databases were not used by the integration tests. The unique test Compose project and its temporary services were removed after completion.

## Evidence and limits

- Full successful Maven log: `%TEMP%/kbp-backend-full-suite.log`.
- Initial environment failures: `%TEMP%/kbp-backend-verification.log` and `%TEMP%/kbp-backend-verification-retry.log`.
- Build logs: `%TEMP%/kbp-frontend-build.log` and `%TEMP%/kbp-frontend-build-retry.log`.
- Surefire results: `apps/api/target/surefire-reports` (131 tests in this run).
- A repository-wide whitespace check also reports pre-existing whitespace/CRLF differences; these were not rewritten.
- No deployment, image publication, production startup, live-service smoke test or manual browser/business-flow acceptance test was performed. Passing automated tests does not verify production secrets, infrastructure or live database migration compatibility.
