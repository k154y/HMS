package com.hotelmanagement.hms.reservation.repository;
import com.hotelmanagement.hms.reservation.model.*; import org.springframework.data.jpa.repository.*; import org.springframework.data.domain.*; import java.time.*; import java.util.*;
public interface ReservationRepository extends JpaRepository<Reservation,UUID> {
 @Query("select r from Reservation r where r.id=:id and r.hotelId=:hotel and r.branchId=:branch") Optional<Reservation> lock(UUID id,UUID hotel,UUID branch);
 Page<Reservation> findByHotelIdAndBranchId(UUID hotel,UUID branch,Pageable page);
 Optional<Reservation> findByIdAndHotelIdAndBranchId(UUID id,UUID hotel,UUID branch);
 boolean existsByHotelIdAndReservationReference(UUID hotel,String ref);
 @Query("select r from Reservation r join ReservationRoom rr on rr.reservationId=r.id where r.hotelId=:hotel and r.branchId=:branch and rr.roomId=:room and r.status in ('PENDING','CONFIRMED','CHECKED_IN') and r.checkIn < :out and r.checkOut > :in")
 boolean overlaps(UUID hotel,UUID branch,UUID room,LocalDate in,LocalDate out);
}
