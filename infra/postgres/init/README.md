# PostgreSQL initialization

This directory is mounted into `/docker-entrypoint-initdb.d` by the development Compose file. Optional initialization scripts placed here run only when PostgreSQL creates a new data volume.

Application schema changes belong in `apps/api/src/main/resources/db/migration` and are applied by Flyway. Do not delete database volumes to apply a migration.
