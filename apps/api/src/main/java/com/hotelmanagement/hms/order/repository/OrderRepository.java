package com.hotelmanagement.hms.order.repository;
import com.hotelmanagement.hms.order.model.*; import org.springframework.data.jpa.repository.*; import java.util.*;
public interface OrderRepository extends JpaRepository<Order,UUID>{
 @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
 Optional<Order> findByIdAndHotelIdAndBranchId(UUID id,UUID h,UUID b);
}
