package com.hotelmanagement.hms.identity.authentication.dto;

import com.hotelmanagement.hms.identity.authentication.model.AccessToken;
import com.hotelmanagement.hms.identity.authentication.session.service.RefreshSessionService.CreatedRefreshSession;
import com.hotelmanagement.hms.identity.authentication.session.service.RefreshSessionService.RotatedSession;

import java.time.Instant;
import java.time.OffsetDateTime;

public record AuthenticationResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        Instant accessTokenExpiresAt,
        String refreshToken,
        OffsetDateTime refreshTokenExpiresAt
) {

    public static AuthenticationResponse from(
            AccessToken accessToken,
            CreatedRefreshSession refreshSession) {

        return new AuthenticationResponse(
                accessToken.token(),
                accessToken.tokenType(),
                accessToken.expiresInSeconds(),
                accessToken.expiresAt(),
                refreshSession.rawRefreshToken(),
                refreshSession.expiresAt()
        );
    }

    public static AuthenticationResponse from(
            RotatedSession rotatedSession) {

        AccessToken accessToken =
                rotatedSession.accessToken();

        return new AuthenticationResponse(
                accessToken.token(),
                accessToken.tokenType(),
                accessToken.expiresInSeconds(),
                accessToken.expiresAt(),
                rotatedSession.refreshToken(),
                rotatedSession.refreshTokenExpiresAt()
        );
    }
}