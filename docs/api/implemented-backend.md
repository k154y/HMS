# Implemented backend API

This document describes implemented identity, onboarding, audit, staff administration, hotel, branch and FX routes. Operational hospitality modules are not implemented yet.

## Shared contract

JSON over HTTP; base path `/api/v1`. Protected requests use `Authorization: Bearer <accessToken>`.
Every request returns `X-Request-ID`. A supplied identifier is accepted only when it matches
`[A-Za-z0-9._-]{1,64}`; otherwise the server generates a UUID.

All routes below return 400 for malformed input/invalid fields, 401 for invalid or missing required
authentication, 403 for denied permissions, 404 for a resource outside a scoped lookup, and 409 for
duplicates/state conflicts. A hotel or branch authorization failure returns generic 403 without
revealing resource details. Unexpected errors return generic 500.

Example error:
```json
{"timestamp":"2026-09-12T12:00:00Z","status":400,"code":"VALIDATION_FAILED","message":"One or more fields are invalid.","path":"/api/v1/onboarding/signup","requestId":"frontend-request-1","fieldErrors":[{"field":"email","message":"must be a well-formed email address"}]}
```

Lists accept `page=0&size=50` (zero-based page, size 1–100). The stable page response contains
`content`, `number`, `size`, `totalElements`, `totalPages`, `first`, `last` only.
Results sort by creation time and UUID, except FX (effective time descending, UUID) and audit
(event time descending, UUID). There are no unbounded list routes.

## Authentication and onboarding

| Method and path | Authentication / permission | Request | Success |
|---|---|---|---|
| POST /auth/login | Public; Redis throttle | Login example | 200 token pair |
| POST /auth/refresh | Public; Redis throttle | Refresh example | 200 new token pair |
| POST /auth/logout | Active account JWT; no hotel permission | Refresh example | 204 empty |
| POST /auth/password | Active account JWT; Redis throttle | Password example | 204 empty |
| POST /onboarding/signup | Public; Redis throttle | Signup example | 201 onboarding response |
| POST /onboarding/hotels | Active account JWT; Redis throttle | Hotel settings example | 201 onboarding response |

Login:
```json
{"email":"owner@example.com","password":"a long owner passphrase"}
```
Refresh and logout:
```json
{"refreshToken":"<opaque-token>"}
```
Password:
```json
{"currentPassword":"a long owner passphrase","newPassword":"a different long passphrase"}
```
Token pair:
```json
{"accessToken":"<JWT>","tokenType":"Bearer","expiresIn":900,"accessTokenExpiresAt":"2026-09-12T12:15:00Z","refreshToken":"<opaque-token>","refreshTokenExpiresAt":"2026-10-12T12:00:00Z"}
```

Passwords have 15–128 Unicode code points, are never trimmed, and can contain spaces.
A password change requires the current password and revokes **all refresh sessions**, including the
current session. Already-issued access JWTs remain usable until their short expiry while the account
remains active; login again after changing the password. Disabling/locking the account rejects its
JWTs immediately. Roles and permissions always come from the database.

Failed password attempts persist and temporarily lock an account according to the configured policy.
A refresh token can rotate only once, including concurrent requests. Reusing a rotated token is
rejected; automatic successor-family revocation is not implemented. Logout cannot revoke another
user's session. Invalid credentials, disabled/locked accounts, and expired tokens produce generic 401.

Signup:
```json
{"email":"owner@example.com","password":"a long owner passphrase","fullName":"Hotel Owner","phone":null,"preferredLanguage":"en","hotel":{"code":"LAKE","legalName":"Lake Hotel Ltd","displayName":"Lake Hotel","tin":null,"phone":null,"email":"office@example.com","address":"Hotel address","currencyCode":"RWF","timezone":"Africa/Kigali","defaultLanguage":"en"}}
```
Onboarding response:
```json
{"ownerUserId":"<uuid>","membershipId":"<uuid>","hotel":{"id":"<uuid>","code":"LAKE","legalName":"Lake Hotel Ltd","displayName":"Lake Hotel","tin":null,"phone":null,"email":"office@example.com","address":"Hotel address","currencyCode":"RWF","timezone":"Africa/Kigali","defaultLanguage":"en","status":"TRIAL","trialStartedAt":"2026-09-12T12:00:00Z","trialEndsAt":"2026-12-12T12:00:00Z","gracePeriodEndsAt":null,"createdAt":"2026-09-12T12:00:00Z","updatedAt":"2026-09-12T12:00:00Z"}}
```
The README explicitly establishes self-service registration. Signup transactionally creates the
global identity, hotel, three-calendar-month trial, default roles, all-branch membership and OWNER
assignment. Duplicate normalized emails or hotel codes return generic 409; no identity is silently
attached or overwritten. Existing users log in and use `POST /onboarding/hotels` to create another
hotel under their own identity. Signup does not issue tokens automatically.

## Hotel, branch and currency management

All paths in this section are relative to `/api/v1/hotels/{hotelId}`, require an active account and
membership, and use the listed database permission. Branch routes additionally require access to
the specified branch. Branch lists contain only branches accessible to the caller.

| Method and suffix | Permission / scope | Request | Response |
|---|---|---|---|
| GET (hotel root) | HOTEL_SETTINGS_VIEW / hotel | No body | 200 hotel object from onboarding example |
| PUT (hotel root) | HOTEL_SETTINGS_MANAGE / hotel | Hotel settings example | 200 updated hotel |
| GET /branches | BRANCH_VIEW / accessible branches | page, size | 200 page of branch objects |
| POST /branches | BRANCH_MANAGE / hotel | Branch example | 201 branch |
| GET /branches/{branchId} | BRANCH_VIEW / branch | No body | 200 branch |
| PUT /branches/{branchId} | BRANCH_MANAGE / branch | Branch example | 200 branch |
| PUT /branches/{branchId}/status | BRANCH_MANAGE / branch | {"active":false} | 200 branch |
| GET /exchange-rates | EXCHANGE_RATE_VIEW / hotel | page, size | 200 page of FX objects |
| POST /exchange-rates | EXCHANGE_RATE_MANAGE / hotel | FX example | 201 FX object |
| GET /exchange-rates/applicable | EXCHANGE_RATE_VIEW / hotel | currency=USD; optional effectiveAt ISO offset timestamp | 200 FX object |

Hotel settings example (also the existing-owner onboarding body):
```json
{"code":"LAKE","legalName":"Lake Hotel Ltd","displayName":"Lake Hotel","tin":null,"phone":null,"email":"office@example.com","address":"Hotel address","currencyCode":"RWF","timezone":"Africa/Kigali","defaultLanguage":"en"}
```
Legal/display names are required (max 200); code max 50; email max 255; address max 2000.
Languages are en/fr/rw. Timezones must be valid Java zone IDs. Currency is three uppercase letters.
Code and base currency cannot change through hotel update (409 IMMUTABLE_HOTEL_SETTING).
Trial dates and subscription status cannot be edited by a tenant. Missing currency/language/timezone
on creation use existing defaults RWF/en/Africa/Kigali. Null language/timezone on update retain current values.

Branch:
```json
{"code":"HQ","name":"Main branch","phone":null,"email":null,"address":null,"timezone":"Africa/Kigali"}
```
Branch response:
```json
{"id":"<uuid>","hotelId":"<uuid>","code":"HQ","name":"Main branch","phone":null,"email":null,"address":null,"timezone":"Africa/Kigali","active":true,"createdAt":"2026-09-12T12:00:00Z","updatedAt":"2026-09-12T12:00:00Z"}
```
Codes are normalized uppercase and unique per hotel; duplicate returns 409. Branch name is required.
Creation without timezone inherits hotel timezone. Deactivation preserves the branch and history.

FX request:
```json
{"currencyCode":"USD","rateToBase":1450.00000000,"effectiveFrom":"2026-09-12T12:00:00Z"}
```
FX response:
```json
{"id":"<uuid>","hotelId":"<uuid>","baseCurrencyCode":"RWF","currencyCode":"USD","rateToBase":1450.00000000,"enabled":true,"effectiveFrom":"2026-09-12T12:00:00Z","createdAt":"2026-09-12T12:00:00Z"}
```
Convention: 1 foreign unit = rateToBase base units. Rate must be positive, up to 11 integer and
8 fractional digits. effectiveFrom defaults to now. Each POST inserts a historical rate.
Missing/disabled applicable rate or duplicate effective time produces 409. Base currency needs no
FX record (requesting one returns 400). No branch overrides. Payment persistence and payment FX
snapshots are not yet implemented.

## Staff and roles

All paths below are relative to `/api/v1/hotels/{hotelId}`; active JWT/account/membership required.
These are hotel-wide administrative operations. No platform authority exists in hotel roles.

| Method and suffix | Permission | Request | Response |
|---|---|---|---|
| GET /memberships | USER_VIEW | page, size | 200 page of memberships |
| POST /memberships | USER_MANAGE | Staff example | 201 membership |
| PUT /memberships/{membershipId}/status | USER_MANAGE | {"status":"SUSPENDED"} | 200 membership |
| PUT /memberships/{membershipId}/branch-access | USER_MANAGE | Branch access example | 204 empty |
| GET /roles | ROLE_VIEW | page, size | 200 page of roles |
| POST /roles | ROLE_MANAGE | Role example | 201 role |
| PUT /roles/{roleId} | ROLE_MANAGE | Role example | 200 role |
| GET /roles/{roleId}/permissions | ROLE_VIEW | No body | 200 ["BRANCH_VIEW"] |
| PUT /roles/{roleId}/permissions/{permission} | ROLE_MANAGE | No body | 204 empty |
| DELETE /roles/{roleId}/permissions/{permission} | ROLE_MANAGE | No body | 204 empty |
| PUT /memberships/{membershipId}/roles/{roleId} | ROLE_MANAGE | No body | 204 empty |
| DELETE /memberships/{membershipId}/roles/{roleId} | ROLE_MANAGE | No body | 204 empty |

Staff creation (new global identity):
```json
{"existingUserId":null,"email":"staff@example.com","password":"a long staff passphrase","fullName":"Staff Name","phone":null,"preferredLanguage":"fr","allBranches":false}
```
Attach an existing identity:
```json
{"existingUserId":"<uuid>","allBranches":false}
```
Existing identity requests must omit identity details/password; those fields cannot be overwritten.
Duplicate normalized email or existing hotel membership returns 409. Memberships created with
allBranches=false initially have no operational branch access.

Membership response:
```json
{"id":"<uuid>","userId":"<uuid>","hotelId":"<uuid>","status":"ACTIVE","allBranches":false,"createdAt":"2026-09-12T12:00:00Z","updatedAt":"2026-09-12T12:00:00Z"}
```
Status enum: ACTIVE, SUSPENDED, REVOKED. Membership history is retained.

Branch access replacement:
```json
{"allBranches":false,"branchIds":["<uuid>"]}
```
At most 100 distinct branch IDs, all belonging to this hotel. allBranches=true requires an empty
branchIds array. The replacement is atomic; invalid branch selection rolls back all changes.

Role request:
```json
{"code":"CUSTOM_VIEWER","name":"Custom viewer","description":"Read access","active":true}
```
Role response:
```json
{"id":"<uuid>","code":"CUSTOM_VIEWER","name":"Custom viewer","description":"Read access","active":true,"systemDefined":false}
```
Code uses uppercase letters, digits and underscores, beginning with a letter (max 100).
Name is required (max 150); description max 2000. Codes are immutable and unique per hotel.
System-defined roles cannot be edited or have their permissions changed (409 SYSTEM_ROLE_PROTECTED).
Assign/remove membership roles is allowed; assigning inactive roles or assigning to revoked
memberships returns 409. Cross-hotel memberships/roles cannot be used together.
There are no role or membership hard-delete endpoints.

## Audit

`GET /api/v1/hotels/{hotelId}/audit-events?page=0&size=50` requires active JWT/account/membership
and AUDIT_VIEW; hotel-wide scope. No body. Returns 200 page:
```json
{"content":[{"id":"<uuid>","hotelId":"<uuid>","branchId":null,"actorUserId":"<uuid>","action":"HOTEL_ONBOARDED","entityType":"HOTEL","entityId":"<uuid>","occurredAt":"2026-09-12T12:00:00Z","requestId":"frontend-request-1"}],"number":0,"size":50,"totalElements":1,"totalPages":1,"first":true,"last":true}
```
Audit covers implemented onboarding, password/login/logout, staff/role/branch-access, hotel/branch and
exchange-rate changes. Global authentication events have null hotel and are not returned in hotel
audit lists. Database triggers reject audit updates/deletes. No audit mutation API exists.
Before/after snapshots and a platform security-event reader are not yet implemented.
