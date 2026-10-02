package com.hotelmanagement.hms.reservation.repository;

import com.hotelmanagement.hms.reservation.model.Reservation;
import com.hotelmanagement.hms.reservation.model.ReservationStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository
        extends JpaRepository<Reservation, UUID> {

    @Query("""
            select r
            from Reservation r
            where r.id = :id
              and r.hotelId = :hotel
              and r.branchId = :branch
            """)
    Optional<Reservation> lock(
            UUID id,
            UUID hotel,
            UUID branch
    );

    @Query(
            value = """
                    select r.*
                    from reservations r
                    where r.hotel_id = :hotel
                      and r.branch_id = :branch
                    order by
                      case
                        when r.status = 'CONFIRMED' then 0
                        when r.status = 'CHECKED_IN' then 1
                        else 2
                      end,
                      case
                        when r.status = 'CONFIRMED'
                          then r.check_in
                      end asc,
                      case
                        when r.status = 'CHECKED_IN'
                          then r.check_out
                      end asc,
                      case
                        when r.status not in (
                          'CONFIRMED',
                          'CHECKED_IN'
                        )
                          then r.check_out
                      end desc,
                      r.created_at desc,
                      r.id
                    """,
            countQuery = """
                    select count(*)
                    from reservations r
                    where r.hotel_id = :hotel
                      and r.branch_id = :branch
                    """,
            nativeQuery = true
    )
    Page<Reservation> findOperational(
            UUID hotel,
            UUID branch,
            Pageable page
    );

    List<Reservation>
    findByHotelIdAndBranchIdAndStatusAndCheckOutLessThanEqual(
            UUID hotel,
            UUID branch,
            ReservationStatus status,
            LocalDate checkOut
    );

    Optional<Reservation>
    findByIdAndHotelIdAndBranchId(
            UUID id,
            UUID hotel,
            UUID branch
    );

    boolean existsByHotelIdAndReservationReference(
            UUID hotel,
            String ref
    );

    @Query("""
            select r
            from Reservation r
            join ReservationRoom rr
              on rr.reservationId = r.id
            where r.hotelId = :hotel
              and r.branchId = :branch
              and rr.roomId = :room
              and r.status in (
                  'PENDING',
                  'CONFIRMED',
                  'CHECKED_IN'
              )
              and r.checkIn < :out
              and r.checkOut > :in
            """)
    boolean overlaps(
            UUID hotel,
            UUID branch,
            UUID room,
            LocalDate in,
            LocalDate out
    );

 @Query("""
        select case
            when count(rr) > 0 then true
            else false
        end
        from ReservationRoom rr
        join Reservation r
          on r.id = rr.reservationId
        where rr.hotelId = :hotel
          and rr.branchId = :branch
          and rr.roomId = :room
          and rr.active = true
          and r.id <> :reservation
          and r.status in (
              'PENDING',
              'CONFIRMED',
              'CHECKED_IN'
          )
          and rr.reservationCheckIn < :out
          and rr.reservationCheckOut > :in
        """)
 boolean overlapsOther(
         UUID hotel,
         UUID branch,
         UUID room,
         UUID reservation,
         LocalDate in,
         LocalDate out
 );

}
