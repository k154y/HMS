package com.hotelmanagement.hms.reservation.repository;
import com.hotelmanagement.hms.reservation.model.ReservationGuest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface ReservationGuestRepository extends JpaRepository<ReservationGuest,UUID> {}
