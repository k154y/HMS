# Hotel Management System — System Architecture

## 1. Architecture Style

The first production version of the Hotel Management System will use a modular monolith architecture.

The backend is one Spring Boot application with strongly separated business modules.

The initial architecture must not use microservices.

## 2. Main Technology Stack

### Frontend

* Next.js
* TypeScript
* Tailwind CSS

### Backend

* Java
* Spring Boot
* Spring Security
* JWT
* WebSocket

### Persistence

* PostgreSQL

### Cache

* Redis

### Background Processing

* RabbitMQ

Kafka may be introduced later only when justified by actual scale or streaming requirements.

### File Storage

Preferred initial provider:

* Cloudflare R2

The storage design should remain compatible with S3-style object storage.

### Deployment

Initial:

* Docker
* Docker Compose
* Cloud server

Future:

* Kubernetes

### Observability

* OpenTelemetry
* Prometheus
* Grafana

### CI/CD

* GitHub Actions

## 3. Modular Monolith

The backend application remains one deployable Spring Boot service.

Business boundaries must still be kept separate.

Planned modules include:

* Authentication
* Hotels
* Branches
* Subscriptions
* Users
* Roles and permissions
* Customers
* Companies
* Guests
* Rooms
* Reservations
* Stays
* Folios
* Orders
* Payments
* Products
* Inventory
* Purchasing
* Vendors
* Accounting
* Housekeeping
* Maintenance
* Notifications
* Audit
* Reporting

Modules may communicate through clearly defined services and domain events.

Direct uncontrolled access between module internals should be avoided.

## 4. Multi-Tenant Model

The system is a multi-hotel SaaS platform.

Each hotel is a tenant.

A tenant may contain one or multiple branches.

Typical relationship:

Hotel → Branch → Operational Data

Tenant ownership must be enforced at backend and database access levels.

The frontend must never be trusted to enforce tenant isolation.

## 5. Database Strategy

The first architecture uses one PostgreSQL database containing multiple hotel tenants.

Tenant-owned tables must contain `hotel_id`.

Branch-owned records also contain `branch_id` where applicable.

Database migrations are managed using Flyway.

Hibernate automatic schema creation must not be used as the production schema-management mechanism.

## 6. Transaction Strategy

Operations that must succeed together should execute in one database transaction.

Examples:

* Purchase confirmation + stock receipt
* Payment + balance update
* Reservation confirmation + room allocation
* Order confirmation + resulting financial charge

Partial completion of such operations must be avoided.

## 7. Real-Time Communication

WebSocket will be used when real-time updates materially improve operations.

Examples:

* New kitchen order
* Order ready
* Reservation change
* Room status update
* New internal notification

Ordinary CRUD operations remain REST-based.

## 8. Background Processing

RabbitMQ will handle asynchronous tasks when the user should not need to wait for them.

Examples may include:

* Notification delivery
* Email
* Report processing
* Subscription reminders
* Non-critical asynchronous events

Core financial correctness must not depend on an unreliable asynchronous operation.

## 9. Caching

Redis may be used for:

* Frequently accessed reference data
* Short-lived computed data
* Rate limiting
* Temporary authentication/security data
* Selected performance optimizations

PostgreSQL remains the authoritative source for transactional hotel data.

Redis must not become the only source of truth for reservations, payments, stock, or accounting.

## 10. Files

Hotel files are stored outside PostgreSQL in object storage.

PostgreSQL stores metadata and ownership information.

Object storage may contain:

* Guest documents
* Hotel documents
* Receipts
* Generated files
* Images

Access must respect hotel ownership and application permissions.

## 11. Deployment Model

Development:

Docker Compose

Production first phase:

* Frontend container
* Backend container
* Redis
* RabbitMQ
* PostgreSQL or managed PostgreSQL
* Reverse proxy

Production expansion:

Kubernetes only when operational scale requires it.

## 12. Design Principle

Infrastructure complexity must be introduced only when justified.

The system should remain:

* Secure
* Maintainable
* Auditable
* Testable
* Scalable
* Deployable by a small engineering team
