package com.hotelmanagement.hms.platform.service;

import com.hotelmanagement.hms.platform.dto.BranchResponse;
import com.hotelmanagement.hms.platform.dto.CreateBranchRequest;
import com.hotelmanagement.hms.platform.model.Branch;
import com.hotelmanagement.hms.platform.model.Hotel;
import com.hotelmanagement.hms.platform.repository.BranchRepository;
import com.hotelmanagement.hms.platform.repository.HotelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.UUID;

@Service
public class BranchService {

    private final HotelRepository hotelRepository;
    private final BranchRepository branchRepository;

    public BranchService(
            HotelRepository hotelRepository,
            BranchRepository branchRepository) {

        this.hotelRepository = hotelRepository;
        this.branchRepository = branchRepository;
    }

    /**
     * Creates a branch under a specific hotel tenant.
     *
     * The hotel ID will eventually come from trusted platform/security
     * context rather than being blindly accepted from browser input.
     */
    @Transactional
    public BranchResponse createBranch(
            UUID hotelId,
            CreateBranchRequest request) {

        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Hotel not found: " + hotelId));

        String code = normalizeCode(request.code());

        if (branchRepository.existsByHotel_IdAndCode(
                hotelId,
                code)) {

            throw new IllegalStateException(
                    "Branch code '" + code
                            + "' already exists for this hotel.");
        }

        String timezone = trimToNull(request.timezone());

        if (timezone == null) {
            timezone = hotel.getTimezone();
        }

        validateTimezone(timezone);

        OffsetDateTime now =
                OffsetDateTime.now(ZoneOffset.UTC);

        Branch branch = Branch.create(
                hotel,
                code,
                request.name().trim(),
                trimToNull(request.phone()),
                trimToNull(request.email()),
                trimToNull(request.address()),
                timezone,
                now
        );

        Branch savedBranch =
                branchRepository.saveAndFlush(branch);

        return BranchResponse.from(savedBranch);
    }

    private String normalizeCode(String code) {
        return code
                .trim()
                .toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {

        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }

    private void validateTimezone(String timezone) {

        try {
            ZoneId.of(timezone);
        } catch (DateTimeException exception) {
            throw new IllegalArgumentException(
                    "Invalid timezone: " + timezone,
                    exception
            );
        }
    }
}