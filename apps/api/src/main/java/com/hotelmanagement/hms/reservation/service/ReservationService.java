package com.hotelmanagement.hms.reservation.service;

import com.hotelmanagement.hms.audit.service.AuditService;
import com.hotelmanagement.hms.customer.repository.CustomerRepository;
import com.hotelmanagement.hms.reservation.dto.*;
import com.hotelmanagement.hms.reservation.model.*;
import com.hotelmanagement.hms.reservation.repository.*;
import com.hotelmanagement.hms.room.repository.*;
import com.hotelmanagement.hms.shared.service.OperationScope;
import com.hotelmanagement.hms.shared.web.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Locale;
import java.util.UUID;

import static com.hotelmanagement.hms.identity.authorization.model.PermissionCode.*;


@Service
@Transactional
public class ReservationService {

    private final org.springframework.jdbc.core.JdbcTemplate db;

    private final ReservationRepository reservations;
    private final ReservationRoomRepository allocations;
    private final CustomerRepository customers;
    private final RoomRepository rooms;
    private final RoomTypeRepository roomTypes;

    private final OperationScope scope;
    private final AuditService audit;

    private final com.hotelmanagement.hms.folio.service.FolioService folios;


    public ReservationService(
            ReservationRepository reservations,
            ReservationRoomRepository allocations,
            CustomerRepository customers,
            RoomRepository rooms,
            RoomTypeRepository roomTypes,
            OperationScope scope,
            AuditService audit,
            com.hotelmanagement.hms.folio.service.FolioService folios,
            org.springframework.jdbc.core.JdbcTemplate db) {

        this.db = db;
        this.reservations = reservations;
        this.allocations = allocations;
        this.customers = customers;
        this.rooms = rooms;
        this.roomTypes = roomTypes;
        this.scope = scope;
        this.audit = audit;
        this.folios = folios;
    }


    public ReservationResponse create(
            UUID hotel,
            UUID branch,
            ReservationRequest req) {

        UUID actor =
                scope.branch(
                        hotel,
                        branch,
                        RESERVATION_CREATE
                );

        if (req.checkIn() == null
                || req.checkOut() == null
                || !req.checkOut().isAfter(
                        req.checkIn()
                )
                || req.rooms() == null
                || req.rooms().isEmpty()
                || req.rooms().size() > 20) {

            throw new IllegalArgumentException(
                    "Invalid reservation dates or rooms."
            );
        }


        int adults = 0;
        int children = 0;

        var seen =
                new HashSet<UUID>();

        var guests =
                new HashSet<UUID>();


        for (var allocation : req.rooms()) {

            if (allocation == null
                    || allocation.adults() < 1
                    || allocation.children() < 0
                    || !seen.add(
                            allocation.roomId()
                    )) {

                throw new IllegalArgumentException(
                        "Invalid occupancy or duplicate room."
                );
            }


            adults +=
                    allocation.adults();

            children +=
                    allocation.children();


            if (allocation.guestIds() != null) {

                if (allocation
                        .guestIds()
                        .size()
                        >
                        allocation.adults()
                                + allocation.children()) {

                    throw new IllegalArgumentException(
                            "Too many guests."
                    );
                }


                for (var guest
                        : allocation.guestIds()) {

                    if (guest == null
                            || !guests.add(
                                    guest
                            )) {

                        throw new IllegalArgumentException(
                                "Duplicate guest."
                        );
                    }
                }
            }
        }


        if (adults != req.adults()
                || children != req.children()) {

            throw new IllegalArgumentException(
                    "Room occupancy must match reservation totals."
            );
        }


        customers
                .findByIdAndHotelId(
                        req.bookingCustomerId(),
                        hotel
                )
                .orElseThrow(
                        ApiException::notFound
                );


        String reference =
                "RSV-"
                        + UUID.randomUUID()
                        .toString()
                        .replace(
                                "-",
                                ""
                        )
                        .substring(
                                0,
                                12
                        )
                        .toUpperCase(
                                Locale.ROOT
                        );


        /*
         * A reservation receives its folio immediately.
         *
         * This is important because reservation advances are posted
         * against this same folio before check-in.
         */
        var folio =
                folios.open(
                        hotel,
                        branch,
                        req.bookingCustomerId(),
                        actor
                );


        var reservation =
                reservations.saveAndFlush(
                        Reservation.create(
                                hotel,
                                branch,
                                reference,
                                req.bookingCustomerId(),
                                folio.getId(),
                                req.checkIn(),
                                req.checkOut(),
                                req.adults(),
                                req.children(),
                                req.notes(),
                                actor
                        )
                );


        var saved =
                new ArrayList<ReservationRoom>();

        var requested =
                new ArrayList<>(
                        req.rooms()
                );


        /*
         * Stable room locking order helps prevent deadlocks when
         * reservations contain multiple rooms.
         */
        requested.sort(
                Comparator.comparing(
                        ReservationRequest.RoomBooking::roomId
                )
        );


        for (var allocationRequest
                : requested) {

            var room =
                    rooms.lock(
                            allocationRequest.roomId(),
                            hotel,
                            branch
                    )
                    .orElseThrow(
                            ApiException::notFound
                    );


            if (!room.getActive()
                    || room.getOperational()
                    != com.hotelmanagement.hms.room.model.RoomOperationalState.AVAILABLE) {

                throw new ApiException(
                        409,
                        "ROOM_UNAVAILABLE",
                        "Room is not operationally available."
                );
            }


            if (allocationRequest.adults()
                    > room.getAdults()
                    || allocationRequest.children()
                    > room.getChildren()) {

                throw new IllegalArgumentException(
                        "Occupancy exceeds room capacity."
                );
            }


            BigDecimal rate =
                    room.getNightlyRate();


            if (rate == null) {

                rate =
                        roomTypes
                                .findByIdAndHotelIdAndBranchId(
                                        room.getRoomTypeId(),
                                        hotel,
                                        branch
                                )
                                .orElseThrow(
                                        ApiException::notFound
                                )
                                .getDefaultRate();
            }


            /*
             * A manually overridden reservation rate still requires the
             * existing ROOM_RATE_MANAGE permission.
             */
            if (allocationRequest.rate() != null
                    && allocationRequest
                    .rate()
                    .compareTo(
                            rate
                    )
                    != 0) {

                scope.branch(
                        hotel,
                        branch,
                        ROOM_RATE_MANAGE
                );

                rate =
                        com.hotelmanagement.hms.shared.model.Money
                                .nonnegative(
                                        allocationRequest.rate()
                                );
            }


            var allocation =
                    allocations.saveAndFlush(
                            ReservationRoom.create(
                                    hotel,
                                    branch,
                                    reservation.getId(),
                                    room.getId(),
                                    req.checkIn(),
                                    req.checkOut(),
                                    rate,
                                    allocationRequest.adults(),
                                    allocationRequest.children()
                            )
                    );


            saved.add(
                    allocation
            );


            if (allocationRequest.guestIds()
                    != null) {

                for (var guest
                        : allocationRequest.guestIds()) {

                    if (db.queryForList(
                            """
                            select id
                            from guests
                            where id = ?
                              and hotel_id = ?
                            """,
                            guest,
                            hotel
                    ).isEmpty()) {

                        throw ApiException.notFound();
                    }


                    db.update(
                            """
                            insert into reservation_guests(
                                id,
                                hotel_id,
                                branch_id,
                                created_at,
                                version,
                                allocation_id,
                                guest_id
                            )
                            values(
                                ?, ?, ?, CURRENT_TIMESTAMP, 0, ?, ?
                            )
                            """,
                            UUID.randomUUID(),
                            hotel,
                            branch,
                            allocation.getId(),
                            guest
                    );
                }
            }
        }


        audit.record(
                hotel,
                branch,
                actor,
                "RESERVATION_CREATED",
                "RESERVATION",
                reservation.getId()
        );


        return ReservationResponse.from(
                reservation,
                saved
        );
    }


    @Transactional(readOnly = true)
    public PageResponse<com.hotelmanagement.hms.room.dto.RoomResponse> available(
            UUID hotel,
            UUID branch,
            LocalDate checkIn,
            LocalDate checkOut,
            int adults,
            int children,
            int page,
            int size) {

        scope.branch(
                hotel,
                branch,
                RESERVATION_VIEW
        );


        if (adults < 1
                || children < 0
                || checkIn == null
                || checkOut == null
                || !checkOut.isAfter(
                        checkIn
                )) {

            throw new IllegalArgumentException(
                    "Check-out must be after check-in."
            );
        }


        return PageResponse.from(
                rooms.available(
                        hotel,
                        branch,
                        checkIn,
                        checkOut,
                        adults,
                        children,
                        scope.page(
                                page,
                                size
                        )
                )
                .map(
                        com.hotelmanagement.hms.room.dto.RoomResponse::from
                )
        );
    }


    public PageResponse<ReservationResponse> list(
            UUID hotel,
            UUID branch,
            int page,
            int size) {

        UUID actor =
                scope.branch(
                        hotel,
                        branch,
                        RESERVATION_VIEW
                );

        reconcileExpiredConfirmed(
                hotel,
                branch,
                actor
        );

        if (page < 0
                || size < 1
                || size > 100) {

            throw new IllegalArgumentException(
                    "Invalid page."
            );
        }

        return PageResponse.from(
                reservations
                        .findOperational(
                                hotel,
                                branch,
                                org.springframework.data.domain.PageRequest.of(
                                        page,
                                        size
                                )
                        )
                        .map(
                                reservation ->
                                        ReservationResponse.from(
                                                reservation,
                                                allocations
                                                        .findByReservationId(
                                                                reservation.getId()
                                                        )
                                        )
                        )
        );
    }


    private void reconcileExpiredConfirmed(
            UUID hotel,
            UUID branch,
            UUID actor) {

        String timezone =
                db.queryForObject(
                        """
                        select timezone
                        from hotels
                        where id = ?
                        """,
                        String.class,
                        hotel
                );

        if (timezone == null
                || timezone.isBlank()) {

            throw new IllegalStateException(
                    "Hotel timezone is missing."
            );
        }

        LocalDate today =
                LocalDate.now(
                        java.time.ZoneId.of(
                                timezone
                        )
                );

        var expired =
                reservations
                        .findByHotelIdAndBranchIdAndStatusAndCheckOutLessThanEqual(
                                hotel,
                                branch,
                                ReservationStatus.CONFIRMED,
                                today
                        );

        for (var reservation : expired) {

            reservation.noShow();

            allocations
                    .findByReservationIdAndHotelIdAndBranchIdAndActiveTrueOrderByRoomId(
                            reservation.getId(),
                            hotel,
                            branch
                    )
                    .forEach(
                            ReservationRoom::release
                    );

            audit.record(
                    hotel,
                    branch,
                    actor,
                    "RESERVATION_AUTO_NO_SHOW",
                    "RESERVATION",
                    reservation.getId()
            );
        }
    }


    @Transactional(readOnly = true)
    public ReservationResponse get(
            UUID hotel,
            UUID branch,
            UUID id) {

        scope.branch(
                hotel,
                branch,
                RESERVATION_VIEW
        );


        var reservation =
                reservations
                        .findByIdAndHotelIdAndBranchId(
                                id,
                                hotel,
                                branch
                        )
                        .orElseThrow(
                                ApiException::notFound
                        );


        return ReservationResponse.from(
                reservation,
                allocations.findByReservationId(
                        id
                )
        );
    }


    /*
     * Returns the financial position of a reservation.
     *
     * We derive the payment status from real posted transactions instead
     * of storing a separate "deposit paid" flag.
     *
     * This means voided/refunded payments can automatically stop counting
     * toward the reservation advance.
     */
    @Transactional(readOnly = true)
    public ReservationPaymentSummary paymentSummary(
            UUID hotel,
            UUID branch,
            UUID id) {

        scope.branch(
                hotel,
                branch,
                RESERVATION_VIEW
        );


        var reservation =
                reservations
                        .findByIdAndHotelIdAndBranchId(
                                id,
                                hotel,
                                branch
                        )
                        .orElseThrow(
                                ApiException::notFound
                        );


        long numberOfNights =
                java.time.temporal.ChronoUnit.DAYS.between(
                        reservation.getCheckIn(),
                        reservation.getCheckOut()
                );


        if (numberOfNights <= 0) {

            throw new IllegalStateException(
                    "Reservation has invalid stay dates."
            );
        }


        BigDecimal nights =
                BigDecimal.valueOf(
                        numberOfNights
                );


        /*
         * Each reservation allocation permanently stores its booked
         * nightly rate. That historical booked rate is used instead of
         * today's room rate.
         */
        BigDecimal reservationTotal =
                allocations
                        .findByReservationId(
                                id
                        )
                        .stream()
                        .map(
                                allocation ->
                                        allocation
                                                .getNightlyRate()
                                                .multiply(
                                                        nights
                                                )
                        )
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        )
                        .setScale(
                                4,
                                RoundingMode.HALF_UP
                        );


        /*
         * Only currently POSTED reservation payments count toward the
         * advance amount.
         *
         * Normal ROOM and FOOD payments are deliberately excluded.
         */
        BigDecimal advancePaid =
                db.queryForObject(
                        """
                        select coalesce(
                            sum(base_amount),
                            0
                        )
                        from payments
                        where hotel_id = ?
                          and branch_id = ?
                          and reservation_id = ?
                          and payment_purpose = 'RESERVATION_ADVANCE'
                          and status = 'POSTED'
                        """,
                        BigDecimal.class,
                        hotel,
                        branch,
                        reservation.getId()
                );


        if (advancePaid == null) {

            advancePaid =
                    BigDecimal.ZERO;
        }


        advancePaid =
                advancePaid.setScale(
                        4,
                        RoundingMode.HALF_UP
                );


        BigDecimal remaining =
                reservationTotal.subtract(
                        advancePaid
                );


        if (remaining.signum() < 0) {

            remaining =
                    BigDecimal.ZERO;
        }


        remaining =
                remaining.setScale(
                        4,
                        RoundingMode.HALF_UP
                );


        String paymentStatus;


        if (advancePaid.signum() == 0) {

            paymentStatus =
                    "NO_PAYMENT";

        } else if (advancePaid.compareTo(
                reservationTotal
        ) >= 0) {

            paymentStatus =
                    "FULLY_PREPAID";

        } else {

            paymentStatus =
                    "PARTIALLY_PREPAID";
        }


        String currency =
                db.queryForObject(
                        """
                        select currency
                        from folios
                        where id = ?
                          and hotel_id = ?
                          and branch_id = ?
                        """,
                        String.class,
                        reservation.getFolioId(),
                        hotel,
                        branch
                );


        if (currency == null
                || currency.isBlank()) {

            throw new IllegalStateException(
                    "Reservation folio currency is missing."
            );
        }


        return new ReservationPaymentSummary(
                reservation.getId(),
                reservation.getReservationReference(),
                reservation.getFolioId(),
                currency,
                reservationTotal,
                advancePaid,
                remaining,
                paymentStatus
        );
    }


    public ReservationResponse update(
            UUID hotel,
            UUID branch,
            UUID id,
            ReservationUpdateRequest request) {

        UUID actor =
                scope.branch(
                        hotel,
                        branch,
                        RESERVATION_MODIFY
                );

        if (request == null
                || request.checkIn() == null
                || request.checkOut() == null
                || !request.checkOut().isAfter(
                        request.checkIn()
                )) {

            throw new IllegalArgumentException(
                    "Check-out must be after check-in."
            );
        }

        var reservation =
                reservations
                        .lock(
                                id,
                                hotel,
                                branch
                        )
                        .orElseThrow(
                                ApiException::notFound
                        );

        reservation.confirmEditable();

        String timezone =
                db.queryForObject(
                        """
                        select timezone
                        from hotels
                        where id = ?
                        """,
                        String.class,
                        hotel
                );

        if (timezone == null
                || timezone.isBlank()) {

            throw new IllegalStateException(
                    "Hotel timezone is missing."
            );
        }

        LocalDate today =
                LocalDate.now(
                        java.time.ZoneId.of(
                                timezone
                        )
                );

        if (request.checkIn().isBefore(
                today
        )) {

            throw new ApiException(
                    409,
                    "RESERVATION_DATE_IN_PAST",
                    "A confirmed reservation cannot be moved to a past check-in date."
            );
        }

        var assigned =
                allocations
                        .findByReservationIdAndHotelIdAndBranchIdAndActiveTrueOrderByRoomId(
                                id,
                                hotel,
                                branch
                        );

        if (assigned.isEmpty()) {

            throw new ApiException(
                    409,
                    "NO_ROOM_ALLOCATION",
                    "This reservation has no active room allocation."
            );
        }

        /*
         * Lock every assigned room in stable order.
         *
         * This reduces the chance of concurrent reservation edits racing
         * against each other.
         */
        var roomIds =
                assigned
                        .stream()
                        .map(
                                ReservationRoom::getRoomId
                        )
                        .sorted()
                        .toList();

        for (UUID roomId : roomIds) {

            var room =
                    rooms
                            .lock(
                                    roomId,
                                    hotel,
                                    branch
                            )
                            .orElseThrow(
                                    ApiException::notFound
                            );

            if (reservations.overlapsOther(
                    hotel,
                    branch,
                    roomId,
                    id,
                    request.checkIn(),
                    request.checkOut()
            )) {

                throw new ApiException(
                        409,
                        "ROOM_UNAVAILABLE_FOR_DATES",
                        "Room "
                                + room.getCode()
                                + " is not available for the selected dates."
                );
            }
        }

        long nights =
                java.time.temporal.ChronoUnit.DAYS.between(
                        request.checkIn(),
                        request.checkOut()
                );

        BigDecimal newReservationTotal =
                assigned
                        .stream()
                        .map(
                                allocation ->
                                        allocation
                                                .getNightlyRate()
                                                .multiply(
                                                        BigDecimal.valueOf(
                                                                nights
                                                        )
                                                )
                        )
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        )
                        .setScale(
                                4,
                                RoundingMode.HALF_UP
                        );

        BigDecimal advancePaid =
                db.queryForObject(
                        """
                        select coalesce(
                            sum(base_amount),
                            0
                        )
                        from payments
                        where hotel_id = ?
                          and branch_id = ?
                          and reservation_id = ?
                          and payment_purpose =
                              'RESERVATION_ADVANCE'
                          and status = 'POSTED'
                        """,
                        BigDecimal.class,
                        hotel,
                        branch,
                        id
                );

        if (advancePaid == null) {
            advancePaid =
                    BigDecimal.ZERO;
        }

        if (advancePaid.compareTo(
                newReservationTotal
        ) > 0) {

            throw new ApiException(
                    409,
                    "RESERVATION_ADVANCE_EXCEEDS_NEW_TOTAL",
                    "The new reservation total is below the advance already received. Refund the excess advance before shortening this reservation."
            );
        }

        reservation.reschedule(
                request.checkIn(),
                request.checkOut()
        );

        assigned.forEach(
                allocation ->
                        allocation.reschedule(
                                request.checkIn(),
                                request.checkOut()
                        )
        );

        audit.record(
                hotel,
                branch,
                actor,
                "RESERVATION_RESCHEDULED",
                "RESERVATION",
                id
        );

        return ReservationResponse.from(
                reservation,
                assigned
        );
    }


    public void cancel(
            UUID hotel,
            UUID branch,
            UUID id) {

        UUID actor =
                scope.branch(
                        hotel,
                        branch,
                        RESERVATION_CANCEL
                );


        var reservation =
                reservations.lock(
                        id,
                        hotel,
                        branch
                )
                .orElseThrow(
                        ApiException::notFound
                );


        BigDecimal liveAdvance =
                db.queryForObject(
                        """
                        select coalesce(
                            sum(base_amount),
                            0
                        )
                        from payments
                        where hotel_id = ?
                          and branch_id = ?
                          and reservation_id = ?
                          and payment_purpose = 'RESERVATION_ADVANCE'
                          and status = 'POSTED'
                        """,
                        BigDecimal.class,
                        hotel,
                        branch,
                        id
                );

        if (liveAdvance != null
                && liveAdvance.signum() > 0) {

            throw new ApiException(
                    409,
                    "RESERVATION_ADVANCE_OUTSTANDING",
                    "Refund or void the reservation advance before cancelling this reservation."
            );
        }

        reservation.cancel();


        allocations
                .findByReservationId(
                        id
                )
                .forEach(
                        ReservationRoom::release
                );


        audit.record(
                hotel,
                branch,
                actor,
                "RESERVATION_CANCELLED",
                "RESERVATION",
                id
        );
    }
}