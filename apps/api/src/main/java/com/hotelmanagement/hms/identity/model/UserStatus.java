package com.hotelmanagement.hms.identity.model;

/**
 * Security state of a global HMS user account.
 *
 * Hotel-level access is handled separately through tenant
 * memberships. Disabling a global account therefore prevents
 * authentication across all hotels accessible by that identity.
 */
public enum UserStatus {

    /**
     * The account may authenticate normally.
     */
    ACTIVE,

    /**
     * Authentication is temporarily prevented, for example after
     * security controls detect too many failed login attempts.
     */
    LOCKED,

    /**
     * The account has been administratively disabled.
     */
    DISABLED
}