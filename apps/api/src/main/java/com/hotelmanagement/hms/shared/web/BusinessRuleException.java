package com.hotelmanagement.hms.shared.web;

/** Explicitly reviewed user-safe business rule; preserves legacy Java caller compatibility. */
public final class BusinessRuleException extends IllegalStateException {
    private final String code;
    public BusinessRuleException(String safeMessage) { this("STATE_CONFLICT", safeMessage); }
    public BusinessRuleException(String code, String safeMessage) { super(safeMessage); this.code=code; }
    public String code() { return code; }
}
