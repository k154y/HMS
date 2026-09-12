# Hotel Management System

A professional web-based hotel management system designed to manage the full operations of hotels, including reception, room management, reservations, guest folios, bar and restaurant POS, kitchen orders, stock management, cashier shifts, deferred payments, accounting reports, audit trails, and subscription-based hotel access.

## Product Vision

The system is designed as a multi-tenant SaaS platform where many hotels can register, use a free trial, and later upgrade to a paid subscription.

Each hotel has its own users, rooms, guests, stock, invoices, payments, reports, and settings. One hotel must never access the data of another hotel.

## Main Business Goals

- Manage hotel rooms and reservations
- Manage guest check-in and check-out
- Track guest consumption through guest folios
- Support clients who consume first and pay later
- Manage bar, restaurant, and kitchen operations
- Track stock from purchase to consumption
- Reduce cheating through audit trails and approvals
- Manage cashier shifts and daily revenue
- Provide manager and owner dashboards
- Support a three-month free trial for hotels
- Scale from one hotel to many hotels and branches

## Technology Stack

### Frontend

- Next.js
- React
- TypeScript
- Tailwind CSS

### Backend

- Spring Boot
- Java
- Spring Security
- REST API
- WebSocket for real-time updates

### Database and Infrastructure

- PostgreSQL
- Redis
- RabbitMQ
- Docker
- Nginx for production reverse proxy

## Project Structure

```text
hotel-management-system/
├── apps/
│   ├── web/                 # Next.js frontend application
│   └── api/                 # Spring Boot backend application
├── docs/                    # Product and technical documentation
├── infra/
│   ├── docker/              # Docker-related infrastructure files
│   ├── nginx/               # Nginx configuration for deployment
│   └── postgres/            # PostgreSQL initialization scripts
├── scripts/                 # Build, test, deployment, and maintenance scripts
├── .github/
│   └── workflows/           # GitHub Actions CI/CD workflows
├── docker-compose.dev.yml   # Local development services
├── .editorconfig            # Editor formatting rules
├── .gitignore               # Files excluded from Git
└── README.md