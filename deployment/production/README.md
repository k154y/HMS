# Production deployment automation

The workflow is implemented but has not been deployed or configured on GitHub/VPS by this task. Existing production Compose, services, secrets and database remain in place. This automates the **backend API only**; frontend hosting retains its existing release process.

## One-time GitHub setup

Create the `production` Environment. Restrict its deployment branches to `main`. Protect `main` with reviews and the workflow's backend/frontend checks; review changes to workflow, deployment scripts and migration checksums especially carefully. Required environment reviewers are optional; enabling them adds a human approval to each deployment.

Configure these environment secrets (repository secrets also work):

| Name | Value |
| --- | --- |
| `HMS_PROD_HOST` | Actual VPS DNS hostname or IPv4 address; no URL prefix |
| `HMS_PROD_USER` | The deployment account chosen and provisioned by your VPS administrator |
| `HMS_PROD_PORT` | SSH port; omit for 22 |
| `HMS_PROD_SSH_PRIVATE_KEY` | Dedicated noninteractive SSH private key, with newlines preserved |
| `HMS_PROD_KNOWN_HOSTS` | Trusted OpenSSH known-hosts entry for that host and port; verify its fingerprint through a trusted VPS console/channel |

Configure Environment **variable** `HMS_PROD_HEALTH_URL` to the actual public HTTPS API health URL returning JSON with `status: UP`, normally ending `/actuator/health`. Use a direct endpoint without redirects, credentials or query parameters. The hostname/path are deliberately not invented here. For nondefault SSH ports the known-hosts entry normally identifies `[host]:port`.

Permit the workflow token to publish the existing `ghcr.io/k154y/hms-api` package. Check organization/package policy if publication is denied. Production SSH secrets are only referenced in the main-push deployment job.

GitHub references: [environments and protection rules](https://docs.github.com/en/actions/reference/workflows-and-actions/deployments-and-environments), [concurrency behavior](https://docs.github.com/en/actions/how-tos/write-workflows/choose-when-workflows-run/control-workflow-concurrency).

## One-time VPS setup

Have an administrator provision a dedicated deployment account, using your actual account naming policy. This repository does not establish which accounts currently exist. Install its public key in that account's `authorized_keys`, restrict file permissions (directory 700, file 600), and consider OpenSSH's `restrict` key option to disable forwarding and PTY. The key must still permit the noninteractive `bash -s` command used by the workflow.

Required host tools: Bash, Python 3 with its standard library, `flock` (util-linux), `curl`, Docker Engine and Compose v2. The account needs noninteractive Docker access, read access to `/opt/hms/production/docker-compose.yml`, and read/write access to the existing `/opt/hms/production/.env` and deployment directory for atomic replacement, backups and `.deployment.lock`. Keep this directory restricted and `.env`/backups mode 600. Ensure the account can operate the existing production project, not a newly invented checkout. Docker access is effectively host-root access: protect the deployment key, repository write access and Environment accordingly.

For a private GHCR package, configure `docker login ghcr.io` once **as the deployment account**, using an independently managed credential with package-read permission and password-stdin or a credential helper. The runner's image-push login does not authenticate the VPS. Do not commit that credential or print it in logs.

Confirm the existing Redis/RabbitMQ services and `kivu_proxy` network are healthy and the reverse proxy routes to `hms_production_api:8081`. The script recreates only `api` with `--no-deps`; it cannot bootstrap missing infrastructure. Preserve the current production `.env` and all required application settings; ensure exactly one `HMS_IMAGE_TAG=` entry exists. Do not replace `.env` with the example file.

Confirm the application's database user already has the rights required by Flyway, Flyway is enabled, and the normal database backup/restore process is operational. Environment backups are **not database backups**. Verify outbound GHCR access, runner-to-VPS SSH reachability and public HTTPS health access from the VPS. The current Dockerfile supplies the container healthcheck. Arrange protected retention/cleanup for timestamped `.env.backup.*` files, which contain production secrets.

No VPS user, credential, network rule, GitHub setting or production data was changed during implementation.

## Future release sequence

1. Work on a feature branch, test, integrate through `devs`, then merge/push to `main`.
2. Backend tests run against disposable services. Offline deployment checks verify release gates and immutable V1–V32 checksums. Frontend error tests, translation checks, TypeScript and production build must pass.
3. The image job builds/pushes `ghcr.io/k154y/hms-api:${github.sha}`, carrying the same `org.opencontainers.image.revision` label.
4. Only `push` to `refs/heads/main` starts `deploy-production`. PR, `devs`, `yfeature` and manual workflow-dispatch runs cannot deploy. The job depends on backend tests and the image job; the image job also depends on frontend verification.
5. Strict host-key-checked SSH sends the checked-out release's deployment script to the VPS. GitHub concurrency serializes deployment; VPS `flock` also prevents overlap. In-progress main workflows are not automatically cancelled. GitHub can replace pending concurrency-group runs with newer pushes; it does not promise deployment of every intermediate SHA.
6. In `/opt/hms/production`, acquire the lock, read only the current image tag for audit, and make a timestamped mode-600 `.env` backup. Pull the exact SHA image and reject a mismatched revision label **before** changing `.env`.
7. Atomically replace only `HMS_IMAGE_TAG`; preserve all other bytes. Run `docker compose --env-file .env -f docker-compose.yml up -d --force-recreate --no-deps api`.
8. Spring Boot starts and Flyway applies pending migrations normally. No raw SQL, separate migration runner, or Flyway repair is involved.
9. Wait up to 300 seconds for container health, verify the running image's revision, then try public health up to 12 times (10-second request limit, 5-second spacing). Only then report success. The overall job timeout is 15 minutes.

## Migration policy and failure recovery

V1–V32 are applied and immutable. `deployment/migrations-v1-v32.sha256` guards their SQL content, normalizing Git's CRLF/LF checkout difference only. New migrations must start at V33 and continue sequentially, be forward-only, tested before merge, and preferably additive/backward-compatible. Never alter the old manifest to conceal an applied migration edit.

On any deployment failure the Action fails. Only allowlisted diagnostic events are emitted from recent container logs; arbitrary SQL/provider/exception text is withheld. Inspect protected VPS logs and Flyway history through the established operational process, using the reported reference/revision. Neither `.env` nor application/database state is automatically rolled back.

Identify whether failure preceded recreation, occurred in migration/startup, or is limited to proxy/public health. Preserve the timestamped environment backup and database recovery point. Prefer a reviewed forward fix and a new immutable image/migration. An application rollback is only appropriate after explicitly establishing database compatibility with the earlier application; restoring the old image tag alone does not undo a migration. If a database restore is required, coordinate a consistent app/database recovery with the operator and account for writes since the backup. Never blindly run `flyway repair` or manually patch production SQL as a deployment shortcut.

One API instance is recreated, so this approach permits a short interruption. Pull/SSH/registry failure, slow migrations, wrong permissions and unavailable infrastructure/public health can fail a release. Actual credentials, VPS behavior and the first deployment still require operational validation after setup; local tests use mocked remote commands.
