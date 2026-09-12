package com.hotelmanagement.hms.platform.dto;

import com.hotelmanagement.hms.platform.model.Hotel;
import com.hotelmanagement.hms.platform.model.HotelStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record HotelResponse(
        UUID id,
        String code,
        String legalName,
        String displayName,
        String tin,
        String phone,
        String email,
        String address,
        String currencyCode,
        String timezone,
        String defaultLanguage,
        HotelStatus status,
        OffsetDateTime trialStartedAt,
        OffsetDateTime trialEndsAt,
        OffsetDateTime gracePeriodEndsAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static HotelResponse from(Hotel hotel) {
        return new HotelResponse(
                hotel.getId(),
                hotel.getCode(),
                hotel.getLegalName(),
                hotel.getDisplayName(),
                hotel.getTin(),
                hotel.getPhone(),
                hotel.getEmail(),
                hotel.getAddress(),
                hotel.getCurrencyCode(),
                hotel.getTimezone(),
                hotel.getDefaultLanguage(),
                hotel.getStatus(),
                hotel.getTrialStartedAt(),
                hotel.getTrialEndsAt(),
                hotel.getGracePeriodEndsAt(),
                hotel.getCreatedAt(),
                hotel.getUpdatedAt()
        );
    }
}