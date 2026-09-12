package com.hotelmanagement.hms.platform.repository;

import com.hotelmanagement.hms.platform.model.Hotel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface HotelRepository extends JpaRepository<Hotel, UUID> {

    Optional<Hotel> findByCode(String code);

    boolean existsByCode(String code);
}