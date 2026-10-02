package com.hotelmanagement.hms.stay.service;

import com.hotelmanagement.hms.audit.service.AuditService;
import com.hotelmanagement.hms.folio.model.EntryKind;
import com.hotelmanagement.hms.folio.service.FolioService;
import com.hotelmanagement.hms.platform.repository.HotelRepository;
import com.hotelmanagement.hms.reservation.model.ReservationRoom;
import com.hotelmanagement.hms.reservation.model.ReservationStatus;
import com.hotelmanagement.hms.reservation.repository.ReservationRepository;
import com.hotelmanagement.hms.reservation.repository.ReservationRoomRepository;
import com.hotelmanagement.hms.room.model.HousekeepingState;
import com.hotelmanagement.hms.room.repository.RoomRepository;
import com.hotelmanagement.hms.shared.service.OperationScope;
import com.hotelmanagement.hms.shared.web.ApiException;
import com.hotelmanagement.hms.stay.model.Stay;
import com.hotelmanagement.hms.stay.repository.StayRepository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static com.hotelmanagement.hms.identity.authorization.model.PermissionCode.CHECKIN_PERFORM;
import static com.hotelmanagement.hms.identity.authorization.model.PermissionCode.CHECKOUT_PERFORM;

@Service
@Transactional
public class StayService {

    private final ReservationRepository reservations;
    private final ReservationRoomRepository allocations;
    private final RoomRepository rooms;
    private final StayRepository stays;
    private final FolioService folios;
    private final HotelRepository hotels;
    private final OperationScope scope;
    private final AuditService audit;
    private final JdbcTemplate db;

    public StayService(
            ReservationRepository reservations,
            ReservationRoomRepository allocations,
            RoomRepository rooms,
            StayRepository stays,
            FolioService folios,
            HotelRepository hotels,
            OperationScope scope,
            AuditService audit,
            JdbcTemplate db) {

        this.reservations = reservations;
        this.allocations = allocations;
        this.rooms = rooms;
        this.stays = stays;
        this.folios = folios;
        this.hotels = hotels;
        this.scope = scope;
        this.audit = audit;
        this.db = db;
    }

    public record StayResponse(
            UUID id,
            UUID roomId,
            UUID folioId,
            Instant checkedInAt,
            Instant checkedOutAt) {
    }

    private StayResponse response(
            Stay stay) {

        return new StayResponse(
                stay.getId(),
                stay.getRoomId(),
                stay.getFolioId(),
                stay.getCheckedInAt(),
                stay.getCheckedOutAt()
        );
    }

    public List<StayResponse> checkIn(
            UUID hotel,
            UUID branch,
            UUID id) {

        UUID actor =
                scope.branch(
                        hotel,
                        branch,
                        CHECKIN_PERFORM
                );

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

        LocalDate today =
                LocalDate.now(
                        ZoneId.of(
                                hotels
                                        .findById(hotel)
                                        .orElseThrow(
                                                ApiException::notFound
                                        )
                                        .getTimezone()
                        )
                );

        if (today.isBefore(
                reservation.getCheckIn()
        )) {

            throw new ApiException(
                    409,
                    "CHECKIN_TOO_EARLY",
                    "This reservation has not reached its check-in date."
            );
        }

        if (!today.isBefore(
                reservation.getCheckOut()
        )) {

            throw new ApiException(
                    409,
                    "RESERVATION_EXPIRED",
                    "The booked stay dates have already ended."
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

        var result =
                new ArrayList<StayResponse>();

        for (var allocation : assigned) {

            var room =
                    rooms
                            .lock(
                                    allocation.getRoomId(),
                                    hotel,
                                    branch
                            )
                            .orElseThrow(
                                    ApiException::notFound
                            );

            if (!room.canOccupy()) {

                throw new ApiException(
                        409,
                        "ROOM_NOT_READY",
                        "Room "
                                + room.getCode()
                                + " is not ready for check-in."
                );
            }

            /*
             * Operational availability/housekeeping does not mean the room
             * has no active guest.
             *
             * Check the authoritative stays table before attempting INSERT,
             * so users receive a meaningful message instead of a database
             * unique-constraint error.
             */
            var activeStay =
                    db.queryForList(
                            """
                            select
                                s.reservation_id,
                                r.reference
                            from stays s
                            join reservations r
                              on r.id = s.reservation_id
                             and r.hotel_id = s.hotel_id
                             and r.branch_id = s.branch_id
                            where s.hotel_id = ?
                              and s.branch_id = ?
                              and s.room_id = ?
                              and s.checked_out_at is null
                            order by s.checked_in_at
                            limit 1
                            """,
                            hotel,
                            branch,
                            room.getId()
                    );

            if (!activeStay.isEmpty()) {

                UUID existingReservationId =
                        (UUID) activeStay
                                .getFirst()
                                .get(
                                        "reservation_id"
                                );

                String existingReference =
                        String.valueOf(
                                activeStay
                                        .getFirst()
                                        .get(
                                                "reference"
                                        )
                        );

                if (id.equals(
                        existingReservationId
                )) {

                    throw new ApiException(
                            409,
                            "ALREADY_CHECKED_IN",
                            "This reservation already has an active stay."
                    );
                }

                throw new ApiException(
                        409,
                        "ROOM_OCCUPIED",
                        "Room "
                                + room.getCode()
                                + " is still occupied by "
                                + existingReference
                                + ". Complete the previous checkout first."
                );
            }

            var stay =
                    stays.saveAndFlush(
                            Stay.create(
                                    hotel,
                                    branch,
                                    id,
                                    room.getId(),
                                    reservation.getFolioId(),
                                    Instant.now(),
                                    null,
                                    actor,
                                    null
                            )
                    );

            BigDecimal charge =
                    allocation
                            .getNightlyRate()
                            .multiply(
                                    BigDecimal.valueOf(
                                            ChronoUnit.DAYS.between(
                                                    reservation.getCheckIn(),
                                                    reservation.getCheckOut()
                                            )
                                    )
                            );

            if (charge.signum() > 0) {

                folios.post(
                        hotel,
                        branch,
                        reservation.getFolioId(),
                        charge,
                        EntryKind.ACCOMMODATION,
                        allocation.getId(),
                        "Accommodation "
                                + room.getCode(),
                        actor
                );
            }

            result.add(
                    response(
                            stay
                    )
            );
        }

        reservation.checkIn();

        audit.record(
                hotel,
                branch,
                actor,
                "CHECKED_IN",
                "RESERVATION",
                id
        );

        return result;
    }

    public List<StayResponse> checkOut(
            UUID hotel,
            UUID branch,
            UUID id) {

        UUID actor =
                scope.branch(
                        hotel,
                        branch,
                        CHECKOUT_PERFORM
                );

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

        if (reservation.getStatus()
                != ReservationStatus.CHECKED_IN) {

            throw new IllegalStateException(
                    "Reservation is not checked in."
            );
        }

        var active =
                stays
                        .findByReservationIdAndHotelIdAndBranchId(
                                id,
                                hotel,
                                branch
                        )
                        .stream()
                        .filter(
                                stay ->
                                        stay.getCheckedOutAt()
                                                == null
                        )
                        .sorted(
                                Comparator.comparing(
                                        Stay::getRoomId
                                )
                        )
                        .toList();

        if (active.isEmpty()) {

            throw new ApiException(
                    409,
                    "NO_ACTIVE_STAY",
                    "This reservation has no active stay to check out."
            );
        }

        /*
         * Settlement is validated before changing stay/room state.
         */
        folios.closeSettled(
                hotel,
                branch,
                reservation.getFolioId()
        );

        for (var stay : active) {

            rooms
                    .lock(
                            stay.getRoomId(),
                            hotel,
                            branch
                    )
                    .orElseThrow(
                            ApiException::notFound
                    )
                    .housekeeping(
                            HousekeepingState.DIRTY
                    );
        }

        active.forEach(
                stay ->
                        stay.checkOut(
                                actor
                        )
        );

        reservation.checkOut();

        allocations
                .findByReservationIdAndHotelIdAndBranchIdAndActiveTrueOrderByRoomId(
                        id,
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
                "CHECKED_OUT",
                "RESERVATION",
                id
        );

        return active
                .stream()
                .map(
                        this::response
                )
                .toList();
    }
}
