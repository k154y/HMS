package com.hotelmanagement.hms.nonresident.model;

/**
 * Business source of a non-resident bill.
 *
 * These values describe why the customer is consuming hotel
 * goods/services without having a hotel stay or room folio.
 */
public enum NonResidentBillType {

    RESTAURANT,
    BAR,
    LAUNDRY,
    TRANSPORT,
    SWIMMING_POOL,
    DAY_USE,
    EVENT,
    CONFERENCE,
    OUTSIDE_CATERING,
    OTHER
}