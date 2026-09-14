package com.hotelmanagement.hms.platform.onboarding.dto;

import com.hotelmanagement.hms.platform.dto.HotelResponse;
import java.util.UUID;
public record OnboardingResponse(UUID ownerUserId, UUID membershipId, HotelResponse hotel) {}
