# Backend implementation status

## Verified baseline and preserved work

The original foundation consists of platform, currency, global identities, memberships,
authorization and authentication with migrations V1–V7. Existing uncommitted authentication work
was preserved and extended. The modular Spring Boot monolith, Java 21, Spring Boot 4.1.1,
PostgreSQL, Flyway, Redis and RabbitMQ remain unchanged. Frontend files were not changed.

Windows has no Maven/Make on PATH; verification runs in the existing development container.
The baseline clean compile and make backend-check both passed before edits.

## Implemented in this continuation

- Atomic new-owner onboarding and authenticated existing-owner hotel creation, with calendar trials.
- Login failure persistence and account-row locking for concurrent security mutations.
- Serialized refresh rotation and cross-user logout protection.
- Password changes and revocation of all refresh sessions.
- Active-account JWT verification, required issuer/type/subject/expiry/issued-at claims.
- Request correlation, stable global errors, explicit CORS and Redis request throttling.
- Migration V8: immutable audit-event persistence, tenant-scoped reading and transactional recording.
- Permission-checked staff/membership/branch access and custom-role administration.
- Permission-checked hotel/branch configuration and append-only FX management.
- PostgreSQL/Redis Testcontainers tests, rollback/concurrency checks and HTTP security tests.

The backend is **not complete for the requested hospitality stage**. These are foundation and
administration phases; do not describe missing operational modules as implemented.

## Remaining dependency work

Customers/companies/guests; room types, rooms and room states; reservation availability, booking,
calendar and concurrency constraints; stays/check-in/check-out; folio charge ledger;
product catalog; vendors/purchases/payables; stock movements and transfers; POS/orders and
kitchen/bar routing; payments/idempotency/refunds/split tender/FX snapshots; cashier shifts;
credit/receivables; approvals; housekeeping; maintenance; operational/financial reports;
outbox/RabbitMQ/WebSocket delivery and authorization.

Mandatory reservation/stock/payment tenant-isolation and financial tests must accompany those
modules. Current tests cannot establish correctness for modules that do not exist.

Additional hardening: refresh successor-family reuse revocation, before/after audit metadata,
more authorization mutation race tests, administrative last-owner recovery safeguards,
dedicated platform administration and monitoring access. Access tokens remain valid until expiry
after password changes; refresh sessions are revoked. Application-level hotel creation helpers
remain internal and must never be exposed without the onboarding transaction.

## Conservative decisions

Self-service onboarding follows the repository README. Existing email identities are never
silently attached by public signup. An authenticated account can create another hotel for itself.
Platform super-admin has not been added to tenant roles.

Hotel code/base currency are immutable through tenant settings; changing accounting currency
requires a separately designed workflow. Tenant settings cannot extend trials or change subscription
status. System-defined roles are protected from edits through the new API.

Authentication endpoints fail closed with 503 when Redis throttling is unavailable; throttling
uses the socket peer address. A deployment behind a proxy must establish trusted proxy handling
before using forwarded client IPs. No raw forwarded IP header is trusted.

Actuator health is public; other actuator endpoints are denied pending a dedicated monitoring
security boundary. Do not grant monitoring access merely because a user owns a hotel.

## Verification commands

Verification on 2026-09-12: the final clean Maven test run passed 23 tests with zero failures,
errors or skips (18 PostgreSQL/Redis integration and HTTP tests, two JWT tests, three password/
request-correlation tests). Flyway V1–V8 and Hibernate schema validation ran successfully in a
fresh PostgreSQL 16 container. The application started on a real HTTP port; public health returned
200/UP and the real HTTP authentication throttle returned 429 when its limit was reached.
The earlier throttle-test failure was corrected by supplying the servlet path in the MockMvc request.
Testcontainer shutdown can emit Redis reconnect warnings after assertions complete.

Inside the development container:
```sh
mvn -B -ntp -f apps/api/pom.xml -DskipTests clean compile
mvn -B -ntp -f apps/api/pom.xml test
make backend-check
```

For this Windows Docker Desktop host:
```powershell
docker compose -f docker-compose.dev.yml exec -T -e TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal app mvn -B -ntp -f apps/api/pom.xml test
```

Testcontainers requires Docker socket access. Test databases and Redis are disposable containers;
tests never clear the development hotel's database. Do not replace PostgreSQL tests with H2.
Testcontainers maps ephemeral ports for test services only; development infrastructure mappings
were not changed.

## Production configuration

Set SPRING_PROFILES_ACTIVE=prod; HMS_DB_HOST/PORT/NAME/USER/PASSWORD;
HMS_REDIS_HOST/PORT/PASSWORD; HMS_RABBITMQ_HOST/PORT/USERNAME/PASSWORD;
HMS_JWT_SECRET (at least 32 cryptographically random bytes), HMS_JWT_ISSUER;
HMS_JWT_ACCESS_TOKEN_MINUTES and HMS_JWT_REFRESH_TOKEN_DAYS;
HMS_AUTH_MAX_FAILED_ATTEMPTS, HMS_AUTH_LOCK_MINUTES, HMS_AUTH_REQUESTS_PER_MINUTE;
HMS_ALLOWED_ORIGINS (comma-separated explicit origins, no wildcard).

Use TLS termination and a restricted network path to database, Redis and RabbitMQ. Separate Flyway
migration ownership from the runtime database role so runtime credentials cannot disable immutable
history triggers. Configure OTEL service/exporter settings against a real collector or disable
unneeded exporters using Spring Boot properties; the existing HMS_OTEL_* example variables are not
yet mapped by application configuration. R2 placeholders are unused: no storage feature is implemented.
No production secret was generated or committed.
