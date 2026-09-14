package com.hotelmanagement.hms.room.repository;
import com.hotelmanagement.hms.room.model.Room;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
import java.util.*;
public interface RoomRepository extends JpaRepository<Room,UUID> {
    @Query("""
      select r from Room r where r.hotelId=:hotel and r.branchId=:branch and r.active=true
      and r.operational=com.hotelmanagement.hms.room.model.RoomOperationalState.AVAILABLE
      and r.adults>=:adults and r.children>=:children and not exists (
       select a.id from ReservationRoom a where a.roomId=r.id and a.active=true and a.reservationCheckIn<:end and a.reservationCheckOut>:start)
      """)
    Page<Room> available(UUID hotel,UUID branch,java.time.LocalDate start,java.time.LocalDate end,int adults,int children,Pageable page);
    Optional<Room> findByIdAndHotelIdAndBranchId(UUID id,UUID hotelId,UUID branchId);
    Page<Room> findByHotelIdAndBranchId(UUID hotelId,UUID branchId,Pageable page);
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Room e where e.id=:id and e.hotelId=:hotel and e.branchId=:branch")
    Optional<Room> lock(UUID id,UUID hotel,UUID branch);
}
