package com.hotelmanagement.hms.identity.membership.model;

/**
 * Access state of a user within one hotel tenant.
 *
 * This is different from UserStatus.
 *
 * A global user account may remain ACTIVE while access to one
 * particular hotel is suspended or revoked.
 */
public enum HotelMembershipStatus {

    /**
     * The user may access the hotel according to assigned
     * roles, permissions, and branch restrictions.
     */
    ACTIVE,

    /**
     * Hotel access is temporarily suspended.
     */
    SUSPENDED,

    /**
     * Hotel access has been withdrawn.
     *
     * The membership remains stored for audit/history.
     */
    REVOKED
}