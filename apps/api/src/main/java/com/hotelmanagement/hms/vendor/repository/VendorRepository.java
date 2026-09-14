package com.hotelmanagement.hms.vendor.repository;
import com.hotelmanagement.hms.vendor.model.Vendor;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
import java.util.*;
public interface VendorRepository extends JpaRepository<Vendor,UUID> {
 Optional<Vendor> findByIdAndHotelId(UUID id,UUID hotel);
 Page<Vendor> findByHotelId(UUID hotel,Pageable page);
 @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
 @Query("select e from Vendor e where e.id=:id and e.hotelId=:hotel")
 Optional<Vendor> lock(UUID id,UUID hotel);
}
