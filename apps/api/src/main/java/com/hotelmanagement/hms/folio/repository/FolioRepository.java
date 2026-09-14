package com.hotelmanagement.hms.folio.repository;
import com.hotelmanagement.hms.folio.model.Folio;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
import java.util.*;
public interface FolioRepository extends JpaRepository<Folio,UUID> {
 Optional<Folio> findByIdAndHotelIdAndBranchId(UUID id,UUID hotel,UUID branch);
 Page<Folio> findByHotelIdAndBranchId(UUID hotel,UUID branch,Pageable page);
 @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
 @Query("select f from Folio f where f.id=:id and f.hotelId=:hotel and f.branchId=:branch")
 Optional<Folio> lock(UUID id,UUID hotel,UUID branch);
}
