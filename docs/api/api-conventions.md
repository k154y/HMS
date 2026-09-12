# Hotel Management System — API Conventions

## 1. Purpose

This document defines the REST API conventions for the Hotel Management System backend.

All backend modules should follow these conventions unless a documented technical reason requires an exception.

## 2. Base API Path

Application APIs should use the following versioned base path:

`/api/v1`

Examples:

`/api/v1/hotels`

`/api/v1/rooms`

`/api/v1/reservations`

`/api/v1/customers`

`/api/v1/payments`

The API version must not be placed only in frontend code.

## 3. Resource Naming

REST resource paths use:

* Lowercase letters
* Plural resource names
* Hyphens for multi-word resources

Examples:

`/api/v1/reservations`

`/api/v1/room-types`

`/api/v1/stock-movements`

`/api/v1/purchase-orders`

Avoid action-style paths when a normal REST resource operation is sufficient.

## 4. HTTP Methods

Use:

### GET

Read information.

Example:

`GET /api/v1/reservations/{id}`

### POST

Create a resource or perform an explicit domain command that cannot be represented safely as a standard update.

Example:

`POST /api/v1/reservations`

### PATCH

Apply a partial update or controlled state transition where appropriate.

### PUT

Replace a complete resource only when true replacement semantics are intended.

### DELETE

Physical deletion should be used cautiously.

Financial, stock, reservation-history, and audit-sensitive records should generally use cancellation, voiding, deactivation, or reversal rather than permanent deletion.

## 5. HTTP Status Codes

Common successful responses:

* `200 OK` — successful read or update
* `201 Created` — resource created
* `204 No Content` — successful operation with no response body

Common client errors:

* `400 Bad Request` — malformed or invalid request
* `401 Unauthorized` — authentication required or invalid
* `403 Forbidden` — authenticated user lacks permission
* `404 Not Found` — requested resource does not exist within accessible scope
* `409 Conflict` — business or concurrency conflict
* `422 Unprocessable Entity` — request is structurally valid but violates domain validation where appropriate

Server failures:

* `500 Internal Server Error`

The API must not expose stack traces or internal secrets to clients.

## 6. Tenant Context

Clients must not be trusted to choose arbitrary hotel ownership.

The authenticated user's trusted security context determines the hotel or hotels the user may access.

A request must not gain access to another tenant merely by changing:

`hotelId`

`hotel_id`

or another tenant identifier.

When a hotel identifier is accepted in a request, backend authorization must independently verify access to that hotel.

## 7. Branch Context

Branch-level operations must validate that:

1. The branch belongs to the current hotel.
2. The authenticated user has permission to access the branch.

A valid branch UUID alone is not sufficient authorization.

## 8. Resource Identifiers

Public API resource IDs should normally use UUIDs.

Example:

`GET /api/v1/reservations/6af66970-8989-4cc1-b3c3-a089201687bf`

Human-readable references may exist separately.

Examples:

* Reservation reference
* Invoice number
* Receipt number
* Purchase order number

Human-readable references must not replace internal primary keys when doing so weakens integrity.

## 9. Date and Time

API timestamps should use ISO 8601.

Example:

`2026-09-08T12:30:00Z`

Backend transactional timestamps should remain timezone-aware.

The frontend may convert timestamps into the hotel's configured timezone.

A plain date may be used for date-only business values such as:

`2026-09-08`

## 10. Monetary Values

The API must never rely on JavaScript or Java floating-point values for authoritative financial calculations.

Monetary values must preserve fixed decimal precision.

A money-related request or response should clearly identify the amount and, where the context does not already guarantee it, its currency.

Example:

```json
{
  "amount": 150000.00,
  "currency": "RWF"
}
```

## 11. Pagination

List endpoints expected to grow significantly should support pagination.

Typical query parameters:

`page`

`size`

Example:

`GET /api/v1/customers?page=0&size=20`

Default and maximum page sizes must be controlled by the backend.

Clients must not be able to request unlimited datasets through an excessively large page size.

## 12. Filtering

Filtering should use explicit query parameters.

Example:

`GET /api/v1/payments?paymentMethod=CASH`

Possible filters may include:

* Date range
* Status
* Branch
* Customer
* Payment method
* Department
* Cashier

Only implemented and documented filters should be exposed.

## 13. Sorting

List endpoints may support controlled sorting.

The backend must whitelist sortable fields.

Clients must not pass arbitrary database column or SQL expressions.

## 14. Request DTOs

Controllers should accept dedicated request objects.

Persistence entities should not normally be accepted directly as API request bodies.

Request DTOs help enforce:

* Validation
* Security boundaries
* Stable API contracts
* Controlled writable fields

## 15. Response DTOs

Persistence entities should not normally be returned directly.

Response DTOs should expose only fields appropriate to the current API and user permissions.

Sensitive internal fields must not leak accidentally through entity serialization.

## 16. Validation

Request validation occurs at multiple levels.

### Structural validation

Examples:

* Required field
* Email format
* Maximum length
* Positive quantity

### Domain validation

Examples:

* Departure date must follow arrival date
* Room must be available
* Credit limit must not be exceeded without authorization
* Payment cannot exceed an allowed outstanding balance
* Stock movement must be valid for the source transaction

Frontend validation improves usability but does not replace backend validation.

## 17. Reservation Concurrency

Room availability search and reservation confirmation are separate operations.

Even when a room appeared available during search, the backend must validate availability again when the reservation is confirmed.

This protects against concurrent bookings.

## 18. Idempotency

Operations that may be retried because of network failure should be designed to avoid duplicate financial or operational effects where necessary.

Important future candidates include:

* Payments
* External payment callbacks
* Order submission
* Reservation confirmation
* Purchase receiving

The specific idempotency mechanism should be introduced when these endpoints are implemented.

## 19. Auditability

Sensitive API actions must preserve enough information to identify:

* Authenticated user
* Hotel
* Branch where applicable
* Action
* Resource
* Timestamp
* Reason where required
* Approval where required

## 20. API Documentation

Only implemented API routes should be documented as existing endpoints.

Planned endpoints must be clearly identified as planned.

Frontend code must never depend on an endpoint that has not been implemented and verified.
