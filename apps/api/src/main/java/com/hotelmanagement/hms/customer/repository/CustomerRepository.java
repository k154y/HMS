package com.hotelmanagement.hms.customer.repository;
import com.hotelmanagement.hms.customer.model.Customer;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
import java.util.*;
public interface CustomerRepository extends JpaRepository<Customer,UUID> {
    Optional<Customer> findByIdAndHotelId(UUID id,UUID hotelId);
    Page<Customer> findByHotelId(UUID hotelId,Pageable page);
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Customer e where e.id=:id and e.hotelId=:hotel")
    Optional<Customer> lock(UUID id,UUID hotel);
}
