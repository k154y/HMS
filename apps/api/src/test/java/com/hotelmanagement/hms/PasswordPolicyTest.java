package com.hotelmanagement.hms;

import com.hotelmanagement.hms.identity.authentication.password.PasswordPolicy;
import com.hotelmanagement.hms.shared.web.RequestCorrelationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import static org.junit.jupiter.api.Assertions.*;

class PasswordPolicyTest {
    private final PasswordPolicy policy = new PasswordPolicy();
    @Test void unicodeCodePointsDefineLength() {
        assertDoesNotThrow(() -> policy.validateNewPassword("😀".repeat(15)));
        assertThrows(IllegalArgumentException.class, () -> policy.validateNewPassword("😀".repeat(14)));
        assertDoesNotThrow(() -> policy.validateNewPassword("😀".repeat(128)));
        assertThrows(IllegalArgumentException.class, () -> policy.validateNewPassword("😀".repeat(129)));
    }
    @Test void passphraseSpacesArePreserved() {
        assertDoesNotThrow(() -> policy.validateNewPassword(" short phrase  "));
        assertThrows(IllegalArgumentException.class, () -> policy.validateNewPassword(" ".repeat(15)));
    }
    @Test void malformedRequestIdIsReplacedAndMdcCleared() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader("X-Request-ID", "x".repeat(1000));
        var response = new MockHttpServletResponse();
        new RequestCorrelationFilter().doFilter(request, response, (req, res) ->
                assertEquals(response.getHeader("X-Request-ID"), org.slf4j.MDC.get("requestId")));
        assertEquals(36, response.getHeader("X-Request-ID").length());
        assertNull(org.slf4j.MDC.get("requestId"));
    }
}
