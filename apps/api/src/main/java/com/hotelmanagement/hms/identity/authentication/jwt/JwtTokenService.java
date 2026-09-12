package com.hotelmanagement.hms.identity.authentication.jwt;

import com.hotelmanagement.hms.identity.authentication.model.AccessToken;
import com.hotelmanagement.hms.identity.model.UserAccount;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class JwtTokenService {

    private static final String TOKEN_TYPE_CLAIM =
            "token_type";

    private static final String ACCESS_TOKEN_TYPE =
            "access";

    private final JwtEncoder jwtEncoder;
    private final JwtProperties properties;

    public JwtTokenService(
            JwtEncoder jwtEncoder,
            JwtProperties properties) {

        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
    }

    /**
     * Issues a short-lived signed access token.
     *
     * Hotel memberships, branch access, roles, and permissions
     * are intentionally NOT embedded into the JWT.
     *
     * The database remains authoritative for authorization.
     */
    public AccessToken issueAccessToken(
            UserAccount user) {

        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException(
                    "A persisted user is required "
                            + "to issue an access token.");
        }

        Instant issuedAt = Instant.now();

        Instant expiresAt =
                issuedAt.plus(
                        Duration.ofMinutes(
                                properties.accessTokenMinutes())
                );

        JwtClaimsSet claims =
                JwtClaimsSet.builder()
                        .issuer(properties.issuer())
                        .subject(
                                user.getId().toString())
                        .issuedAt(issuedAt)
                        .expiresAt(expiresAt)
                        .id(UUID.randomUUID().toString())
                        .claim(
                                "email",
                                user.getEmail())
                        .claim(
                                TOKEN_TYPE_CLAIM,
                                ACCESS_TOKEN_TYPE)
                        .build();

        Jwt jwt =
                jwtEncoder.encode(
                        JwtEncoderParameters.from(
                                claims)
                );

        return AccessToken.bearer(
                jwt.getTokenValue(),
                issuedAt,
                expiresAt
        );
    }
}