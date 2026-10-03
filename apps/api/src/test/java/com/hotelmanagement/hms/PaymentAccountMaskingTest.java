package com.hotelmanagement.hms;

import com.hotelmanagement.hms.payment.service.PaymentAccountMasking;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class PaymentAccountMaskingTest {
    @Test void masksIdentifiersWithoutRevealingShortValues() {
        assertNull(PaymentAccountMasking.mask(null));
        assertEquals("*", PaymentAccountMasking.mask(""));
        assertEquals("*", PaymentAccountMasking.mask("1"));
        assertEquals("****", PaymentAccountMasking.mask("1234"));
        assertEquals("*0123", PaymentAccountMasking.mask("00123"));
        assertEquals("******7890", PaymentAccountMasking.mask("1234567890"));
        assertEquals("*********3456", PaymentAccountMasking.mask("+250788123456"));
    }

    @Test void responseMaskingDoesNotMutateSnapshot() {
        Map<String,Object> stored = Map.of("payment_account_identifier", "0012345678");
        assertEquals("******5678", PaymentAccountMasking.snapshotResponse(stored).get("payment_account_identifier"));
        assertEquals("0012345678", stored.get("payment_account_identifier"));
    }
}
