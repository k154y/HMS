package com.hotelmanagement.hms.identity.authorization.decision;

/**
 * Internal reasons why a tenant-scoped authorization request
 * may be denied.
 *
 * These values are intended for backend logic, testing, and
 * controlled audit/logging. They should not automatically be
 * exposed as detailed messages to untrusted API clients.
 */
public enum AuthorizationDenialReason {

    USER_NOT_FOUND,

    ACCOUNT_NOT_ACTIVE,

    MEMBERSHIP_NOT_FOUND,

    MEMBERSHIP_NOT_ACTIVE,

    BRANCH_NOT_FOUND,

    BRANCH_ACCESS_DENIED,

    PERMISSION_DENIED
}