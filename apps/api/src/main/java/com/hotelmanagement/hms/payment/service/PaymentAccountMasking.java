package com.hotelmanagement.hms.payment.service;

import java.util.LinkedHashMap;
import java.util.Map;

/** Response-only masking. Never apply to persistence or approval posting data. */
public final class PaymentAccountMasking {
    private PaymentAccountMasking() {}

    public static String mask(String identifier) {
        if (identifier == null) return null;
        int length = identifier.codePointCount(0, identifier.length());
        if (length <= 4) return "*".repeat(Math.max(1, length));
        int suffix = identifier.offsetByCodePoints(0, length - 4);
        return "*".repeat(length - 4) + identifier.substring(suffix);
    }

    /** Keep the existing JSON field name for compatibility, but return a masked copy. */
    public static Map<String, Object> snapshotResponse(Map<String, Object> row) {
        var result = new LinkedHashMap<>(row);
        if (result.containsKey("payment_account_identifier")) {
            result.put("payment_account_identifier", mask((String) result.get("payment_account_identifier")));
        }
        return result;
    }
}
