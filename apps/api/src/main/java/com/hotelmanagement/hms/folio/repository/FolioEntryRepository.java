package com.hotelmanagement.hms.folio.repository;
import com.hotelmanagement.hms.folio.model.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
import java.util.*;
import java.math.BigDecimal;
public interface FolioEntryRepository extends JpaRepository<FolioEntry,UUID> {
 @Query("select coalesce(sum(e.amount),0) from FolioEntry e where e.folioId=:folio and e.hotelId=:hotel and e.branchId=:branch")
 BigDecimal balance(UUID hotel,UUID branch,UUID folio);
 Optional<FolioEntry> findByIdAndHotelIdAndBranchId(UUID id,UUID hotel,UUID branch);
 boolean existsByFolioIdAndKindAndSourceId(UUID folio,EntryKind kind,UUID source);
 Page<FolioEntry> findByHotelIdAndBranchIdAndFolioId(UUID hotel,UUID branch,UUID folio,Pageable page);
}
