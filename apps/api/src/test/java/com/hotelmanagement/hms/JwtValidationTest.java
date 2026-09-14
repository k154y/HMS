package com.hotelmanagement.hms;

import com.hotelmanagement.hms.identity.authentication.config.JwtConfiguration;
import com.hotelmanagement.hms.identity.authentication.jwt.JwtProperties;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.*;
import java.time.Instant;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class JwtValidationTest {
    private final JwtConfiguration configuration = new JwtConfiguration(
            new JwtProperties("unit-test-only-key-of-at-least-32-bytes", "hms-test", 15, 30));
    private final JwtEncoder encoder = configuration.jwtEncoder(configuration.jwtSecretKey());
    private final JwtDecoder decoder = configuration.jwtDecoder(configuration.jwtSecretKey());

    private String token(String issuer, String subject, String type, boolean expiry) {
        var builder = JwtClaimsSet.builder().issuer(issuer).subject(subject).issuedAt(Instant.now())
                .claim("token_type", type);
        if(expiry) builder.expiresAt(Instant.now().plusSeconds(60));
        return encoder.encode(JwtEncoderParameters.from(builder.build())).getTokenValue();
    }
    @Test void validAccessTokenWorks() {
        assertNotNull(decoder.decode(token("hms-test",UUID.randomUUID().toString(),"access",true)));
    }
    @Test void issuerTypeSubjectAndExpiryAreRequired() {
        assertThrows(JwtException.class,()->decoder.decode(token("wrong",UUID.randomUUID().toString(),"access",true)));
        assertThrows(JwtException.class,()->decoder.decode(token("hms-test",UUID.randomUUID().toString(),"refresh",true)));
        assertThrows(JwtException.class,()->decoder.decode(token("hms-test","1-1-1-1-1","access",true)));
        assertThrows(JwtException.class,()->decoder.decode(token("hms-test",UUID.randomUUID().toString(),"access",false)));
        assertThrows(JwtException.class,()->decoder.decode("invalid.token.signature"));
    }
}
