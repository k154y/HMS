package com.hotelmanagement.hms.nonresident.model;

/**
 * Display/status projection for a non-resident bill.
 *
 * Financial states such as PAID and PARTIAL are derived from
 * the immutable folio ledger instead of being independently
 * stored in the bill table.
 */
public enum NonResidentBillStatus {

    OPEN,
    PARTIAL,
    PAID,
    CREDIT,
    CANCELLED,
    VOIDED
}