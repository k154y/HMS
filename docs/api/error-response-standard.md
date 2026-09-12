# Hotel Management System — API Error Response Standard

## 1. Purpose

The Hotel Management System must return predictable and secure API errors.

Frontend applications should not need to interpret different error structures from different backend modules.

## 2. Standard Error Structure

Application errors should follow a consistent structure similar to:

```json
{
  "timestamp": "2026-09-08T12:30:00Z",
  "status": 409,
  "code": "ROOM_NOT_AVAILABLE",
  "message": "The selected room is no longer available for the requested dates.",
  "path": "/api/v1/reservations",
  "requestId": "218978ef-3466-418b-a7b3-164d53929345",
  "fieldErrors": []
}
```

Not every field must appear when it is not applicable.

## 3. Error Code

The `code` field is a stable machine-readable application error identifier.

Examples:

`VALIDATION_FAILED`

`UNAUTHORIZED`

`ACCESS_DENIED`

`RESOURCE_NOT_FOUND`

`ROOM_NOT_AVAILABLE`

`RESERVATION_CONFLICT`

`CREDIT_LIMIT_EXCEEDED`

`INSUFFICIENT_STOCK`

`PAYMENT_AMOUNT_INVALID`

`SHIFT_NOT_OPEN`

The frontend should prefer the stable error code over parsing human-readable error text.

## 4. Error Message

The `message` field provides a safe human-readable summary.

It must not expose:

* SQL statements
* Passwords
* JWT secrets
* Stack traces
* Internal file paths
* Database credentials
* Cloud credentials

## 5. Validation Errors

Field validation failures should identify the affected fields.

Example:

```json
{
  "timestamp": "2026-09-08T12:30:00Z",
  "status": 400,
  "code": "VALIDATION_FAILED",
  "message": "One or more fields are invalid.",
  "path": "/api/v1/reservations",
  "requestId": "4df38d20-e386-4012-a37b-2f7a41b20713",
  "fieldErrors": [
    {
      "field": "departureDate",
      "code": "INVALID_DATE_RANGE",
      "message": "Departure date must be after arrival date."
    }
  ]
}
```

## 6. Not Found Behavior

A resource that does not exist or is not visible inside the user's authorized tenant scope may return:

`404 Not Found`

This helps avoid exposing the existence of resources belonging to another hotel.

## 7. Authentication Failure

Missing, expired, or invalid authentication should normally return:

`401 Unauthorized`

Example code:

`UNAUTHORIZED`

## 8. Authorization Failure

An authenticated user who lacks the required permission should normally receive:

`403 Forbidden`

Example code:

`ACCESS_DENIED`

The response should not reveal restricted business data.

## 9. Business Conflicts

Use a conflict response when the requested operation conflicts with current business state.

Examples:

* Room booked by another reservation
* Duplicate reference
* Invalid state transition
* Cashier shift already closed

Typical status:

`409 Conflict`

Example:

```json
{
  "status": 409,
  "code": "ROOM_NOT_AVAILABLE",
  "message": "The selected room is no longer available."
}
```

## 10. Financial Errors

Financial error messages must be clear but must not expose information outside the user's authorization scope.

Examples:

`PAYMENT_AMOUNT_INVALID`

`PAYMENT_ALREADY_REVERSED`

`CREDIT_LIMIT_EXCEEDED`

`CASHIER_SHIFT_CLOSED`

`REFUND_REQUIRES_APPROVAL`

## 11. Inventory Errors

Examples:

`INSUFFICIENT_STOCK`

`STOCK_ADJUSTMENT_REQUIRES_APPROVAL`

`INVALID_STOCK_MOVEMENT`

`GOODS_RECEIPT_ALREADY_CONFIRMED`

## 12. Reservation Errors

Examples:

`ROOM_NOT_AVAILABLE`

`INVALID_STAY_DATE_RANGE`

`RESERVATION_ALREADY_CANCELLED`

`RESERVATION_ALREADY_CHECKED_IN`

`CHECKOUT_BALANCE_REMAINING`

## 13. Request Identifier

Every request should eventually have a request or correlation identifier.

This identifier helps connect:

* API response
* Application log
* Audit event
* Background event
* Production investigation

The identifier must not contain confidential user information.

## 14. Unexpected Server Errors

Unexpected backend failures should return a generic response.

Example:

```json
{
  "status": 500,
  "code": "INTERNAL_ERROR",
  "message": "An unexpected error occurred.",
  "requestId": "98c3ea97-a477-4332-9ed2-dd92ed82b19c"
}
```

Detailed exception information belongs in protected server logs, not in the API response.

## 15. Localization

Stable API error codes should remain language-neutral.

For example:

`ROOM_NOT_AVAILABLE`

The frontend may translate this code into:

* English
* French
* Kinyarwanda

This avoids storing language-specific business logic inside backend exception identifiers.

## 16. Logging Relationship

Server logs should include the same request identifier returned to the client.

This allows support staff to investigate a problem without exposing internal error details to the user.
