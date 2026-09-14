package com.hotelmanagement.hms.inventory.repository;
import com.hotelmanagement.hms.inventory.model.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
import java.util.*;
import java.math.BigDecimal;
public interface StockMovementRepository extends JpaRepository<StockMovement,UUID> {
 @Query("select coalesce(sum(m.quantity),0) from StockMovement m where m.hotelId=:hotel and m.branchId=:branch and m.productId=:product")
 BigDecimal balance(UUID hotel,UUID branch,UUID product);
 Optional<StockMovement> findByIdAndHotelIdAndBranchId(UUID id,UUID hotel,UUID branch);
 Optional<StockMovement> findByHotelIdAndBranchIdAndProductIdAndKindAndSourceId(UUID hotel,UUID branch,UUID product,MovementKind kind,UUID source);
 Page<StockMovement> findByHotelIdAndBranchIdAndProductId(UUID hotel,UUID branch,UUID product,Pageable page);
}
