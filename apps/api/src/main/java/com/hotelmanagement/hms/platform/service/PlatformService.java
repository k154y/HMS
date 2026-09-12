package com.hotelmanagement.hms.platform.service;

import com.hotelmanagement.hms.identity.authorization.service.DefaultRoleProvisioningService;
import com.hotelmanagement.hms.platform.dto.CreateHotelRequest;
import com.hotelmanagement.hms.platform.dto.HotelResponse;
import com.hotelmanagement.hms.platform.model.Hotel;
import com.hotelmanagement.hms.platform.repository.HotelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Locale;

@Service
public class PlatformService {

    private static final String DEFAULT_CURRENCY = "RWF";
    private static final String DEFAULT_TIMEZONE = "Africa/Kigali";
    private static final String DEFAULT_LANGUAGE = "en";

    private final HotelRepository hotelRepository;

    private final DefaultRoleProvisioningService
            defaultRoleProvisioningService;

    public PlatformService(
            HotelRepository hotelRepository,
            DefaultRoleProvisioningService defaultRoleProvisioningService) {

        this.hotelRepository = hotelRepository;
        this.defaultRoleProvisioningService =
                defaultRoleProvisioningService;
    }

    /**
     * Creates a hotel tenant with:
     *
     * - a three-calendar-month trial;
     * - normalized hotel settings;
     * - the standard HMS hotel role profiles.
     *
     * All operations run inside one transaction.
     */
    @Transactional
    public HotelResponse createHotel(
            CreateHotelRequest request) {

        String code =
                normalizeCode(request.code());

        if (hotelRepository.existsByCode(code)) {
            throw new IllegalStateException(
                    "A hotel with code '"
                            + code
                            + "' already exists.");
        }

        String timezone =
                valueOrDefault(
                        request.timezone(),
                        DEFAULT_TIMEZONE
                );

        validateTimezone(timezone);

        String currencyCode =
                valueOrDefault(
                        request.currencyCode(),
                        DEFAULT_CURRENCY
                )
                        .toUpperCase(Locale.ROOT);

        String defaultLanguage =
                valueOrDefault(
                        request.defaultLanguage(),
                        DEFAULT_LANGUAGE
                )
                        .toLowerCase(Locale.ROOT);

        OffsetDateTime trialStartedAt =
                OffsetDateTime.now(
                        ZoneOffset.UTC);

        /*
         * Business requirement:
         * exactly three calendar months,
         * not an approximation such as 90 days.
         */
        OffsetDateTime trialEndsAt =
                trialStartedAt.plusMonths(3);

        Hotel hotel =
                Hotel.createTrial(
                        code,
                        request.legalName().trim(),
                        request.displayName().trim(),
                        trimToNull(request.tin()),
                        trimToNull(request.phone()),
                        trimToNull(request.email()),
                        trimToNull(request.address()),
                        currencyCode,
                        timezone,
                        defaultLanguage,
                        trialStartedAt,
                        trialEndsAt
                );

        Hotel savedHotel =
                hotelRepository.saveAndFlush(
                        hotel);

        /*
         * Provision tenant roles only after the hotel has a
         * persistent identifier.
         *
         * Because this method is transactional, failure during
         * provisioning rolls back the hotel creation as well.
         */
        defaultRoleProvisioningService
                .provisionDefaultRoles(
                        savedHotel);

        return HotelResponse.from(
                savedHotel);
    }

    private String normalizeCode(
            String code) {

        return code
                .trim()
                .toUpperCase(Locale.ROOT);
    }

    private String valueOrDefault(
            String value,
            String defaultValue) {

        String normalized =
                trimToNull(value);

        return normalized == null
                ? defaultValue
                : normalized;
    }

    private String trimToNull(
            String value) {

        if (value == null) {
            return null;
        }

        String normalized =
                value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }

    private void validateTimezone(
            String timezone) {

        try {
            ZoneId.of(timezone);
        } catch (DateTimeException exception) {

            throw new IllegalArgumentException(
                    "Invalid timezone: "
                            + timezone,
                    exception
            );
        }
    }
}