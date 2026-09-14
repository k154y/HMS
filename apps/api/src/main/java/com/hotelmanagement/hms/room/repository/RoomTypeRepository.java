package com.hotelmanagement.hms.room.repository;
import com.hotelmanagement.hms.room.model.RoomType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
import java.util.*;
public interface RoomTypeRepository extends JpaRepository<RoomType,UUID> {
    Optional<RoomType> findByIdAndHotelIdAndBranchId(UUID id,UUID hotelId,UUID branchId);
    Page<RoomType> findByHotelIdAndBranchId(UUID hotelId,UUID branchId,Pageable page);
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from RoomType e where e.id=:id and e.hotelId=:hotel and e.branchId=:branch")
    Optional<RoomType> lock(UUID id,UUID hotel,UUID branch);
}
