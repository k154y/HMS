package com.hotelmanagement.hms.payment.repository;
import com.hotelmanagement.hms.payment.model.*; import org.springframework.data.jpa.repository.*; import java.util.*;
public interface PaymentRepository extends JpaRepository<Payment,UUID> {
 Optional<Payment> findByHotelIdAndBranchIdAndIdempotencyKey(UUID hotel,UUID branch,String key);
 Optional<Payment> findByIdAndHotelIdAndBranchId(UUID id,UUID hotel,UUID branch);
 @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
 @Query("select p from Payment p where p.id=:id and p.hotelId=:hotel and p.branchId=:branch")
 Optional<Payment> lock(UUID id,UUID hotel,UUID branch);
}

