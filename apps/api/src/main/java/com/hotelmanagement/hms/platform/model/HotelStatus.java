package com.hotelmanagement.hms.platform.model;

/**
 * Lifecycle state of a hotel tenant on the HMS platform.
 *
 * These values must remain aligned with the database constraint
 * defined for hotels.status.
 */
public enum HotelStatus {
    TRIAL,
    ACTIVE,
    EXPIRED,
    SUSPENDED,
    CANCELLED
}