package com.hotelmanagement.hms.product.repository;
import com.hotelmanagement.hms.product.model.Product;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
import java.util.*;
public interface ProductRepository extends JpaRepository<Product,UUID> {
 Optional<Product> findByIdAndHotelId(UUID id,UUID hotel);
 Page<Product> findByHotelId(UUID hotel,Pageable page);
 @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
 @Query("select e from Product e where e.id=:id and e.hotelId=:hotel")
 Optional<Product> lock(UUID id,UUID hotel);
}
