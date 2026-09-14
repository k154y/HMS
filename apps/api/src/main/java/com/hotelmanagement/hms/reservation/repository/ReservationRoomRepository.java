package com.hotelmanagement.hms.reservation.repository;
import com.hotelmanagement.hms.reservation.model.*; import org.springframework.data.jpa.repository.*; import java.util.*;
public interface ReservationRoomRepository extends JpaRepository<ReservationRoom,UUID> {
 List<ReservationRoom> findByReservationId(UUID reservationId);
 List<ReservationRoom> findByReservationIdAndHotelIdAndBranchIdAndActiveTrueOrderByRoomId(UUID reservation,UUID hotel,UUID branch);
}
