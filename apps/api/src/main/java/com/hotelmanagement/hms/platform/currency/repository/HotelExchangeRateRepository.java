package com.hotelmanagement.hms.platform.currency.repository;

import com.hotelmanagement.hms.platform.currency.model.HotelExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HotelExchangeRateRepository
        extends JpaRepository<HotelExchangeRate, UUID> {

    org.springframework.data.domain.Page<HotelExchangeRate> findByHotel_Id(UUID hotelId, org.springframework.data.domain.Pageable pageable);

    Optional<HotelExchangeRate>
    findFirstByHotel_IdAndCurrencyCodeAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
            UUID hotelId,
            String currencyCode,
            OffsetDateTime effectiveAt
    );

    List<HotelExchangeRate>
    findByHotel_IdAndCurrencyCodeOrderByEffectiveFromDesc(
            UUID hotelId,
            String currencyCode
    );

    List<HotelExchangeRate>
    findByHotel_IdOrderByCurrencyCodeAscEffectiveFromDesc(
            UUID hotelId
    );
}
