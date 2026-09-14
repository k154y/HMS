package com.hotelmanagement.hms.stay.repository;
import com.hotelmanagement.hms.stay.model.Stay;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface StayRepository extends JpaRepository<Stay,UUID> {
 List<Stay> findByReservationIdAndHotelIdAndBranchId(UUID reservation,UUID hotel,UUID branch);
}
