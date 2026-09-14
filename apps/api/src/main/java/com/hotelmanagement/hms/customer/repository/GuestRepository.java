package com.hotelmanagement.hms.customer.repository;
import com.hotelmanagement.hms.customer.model.Guest;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
import java.util.*;
public interface GuestRepository extends JpaRepository<Guest,UUID> {
    Optional<Guest> findByIdAndHotelId(UUID id,UUID hotelId);
    Page<Guest> findByHotelId(UUID hotelId,Pageable page);
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Guest e where e.id=:id and e.hotelId=:hotel")
    Optional<Guest> lock(UUID id,UUID hotel);
}
