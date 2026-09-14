package com.hotelmanagement.hms.platform.repository;

import com.hotelmanagement.hms.platform.model.Hotel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface HotelRepository extends JpaRepository<Hotel, UUID> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select h from Hotel h where h.id=:id")
    Optional<Hotel> findLockedById(UUID id);

    Optional<Hotel> findByCode(String code);

    boolean existsByCode(String code);
}
