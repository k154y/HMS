# Hotel Management System

HMS contains two applications: a Spring Boot API and a Next.js frontend. The frontend uses authenticated Spring endpoints; PostgreSQL stores hotel data, Redis supports authentication/session infrastructure, and RabbitMQ provides messaging infrastructure.

## Project layout

```text
HMS/
  apps/
    api/
      src/main/java/       Spring modules grouped by business area
      src/main/resources/  Configuration and Flyway database migrations
      src/test/java/       Backend and integration tests
      pom.xml              Java dependencies and build
    web/
      app/                 Pages, layouts and authenticated API proxy
      components/          Shared UI and operations components
      lib/                 Authentication, API client and translations
      types/               Shared TypeScript declarations
      package.json         Frontend commands and dependencies
  docs/                    Product, architecture, API and setup documentation
    maintenance/           Folder cleanup inventory and recovery instructions
    testing/               Recorded test results
  scripts/                 Local verification scripts
  infra/postgres/init/     Optional first-start database initialization
  .devcontainer/           Java development container configuration
  docker-compose.dev.yml   Local application services
  docker-compose.test.yml  Isolated PostgreSQL/Redis test services
  Makefile                 Backend and Docker shortcuts
```

Generated `node_modules`, `.next` and `target` directories remain local and are ignored by Git. Local environment files and test credentials must remain private. The recoverable cleanup archive is under `.local/`, also ignored by Git.

## Run locally

Run from the repository root with Docker Desktop running:

```powershell
docker compose -f docker-compose.dev.yml up -d
docker compose -f docker-compose.dev.yml exec -e JAVA_HOME=/usr/lib/jvm/msopenjdk-current app mvn -B -ntp -f apps/api/pom.xml spring-boot:run
```

In a second terminal:

```powershell
cd apps/web
npm ci
# On a fresh checkout, copy .env.local.example to .env.local and supply its values.
npm run dev -- -p 3001
```

Frontend: http://localhost:3001/login. Backend health: http://localhost:8081/actuator/health. The Docker development backend connects to the database using internal service names; a backend launched directly on Windows needs the host ports from the Compose file.

## Verify changes

```powershell
# Frontend, from apps/web
npx tsc --noEmit

# Translation coverage, from repository root
node scripts/check-translations.cjs

# Isolated backend integration services
docker compose -f docker-compose.dev.yml -f docker-compose.test.yml up -d test-postgres test-redis
docker compose -f docker-compose.dev.yml exec -T -e JAVA_HOME=/usr/lib/jvm/msopenjdk-current -e HMS_TEST_DB_URL=jdbc:postgresql://test-postgres:5432/hms_test -e HMS_TEST_DB_USER=hms_test -e HMS_TEST_DB_PASSWORD=hms_test_only -e HMS_TEST_REDIS_HOST=test-redis app mvn -B -ntp -f apps/api/pom.xml test
```

Scripts named `test-local-*.ps1` use existing local demo credentials and may create labelled test records in the demo hotel. They are for local testing, not a production database.

See [verification status](docs/feature-verification.md), [API documentation](docs/api/implemented-backend.md), and [cleanup notes](docs/maintenance/project-cleanup.md). Feature descriptions are not a claim that every workflow has completed acceptance testing.

See the [production system design](docs/architecture/production-system-design.md) for modules, database tables, permissions, workflows, reports and the remaining production milestones.
