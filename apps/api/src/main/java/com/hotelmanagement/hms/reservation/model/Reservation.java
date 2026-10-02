package com.hotelmanagement.hms.reservation.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "reservations")
public class Reservation
        extends com.hotelmanagement.hms.shared.model.BranchEntity {

    @Column(name = "reference")
    private String reservationReference;

    @Column(name = "customer_id")
    private UUID bookingCustomerId;

    @Column(name = "folio_id")
    private UUID folioId;

    @Column(name = "check_in")
    private LocalDate checkIn;

    @Column(name = "check_out")
    private LocalDate checkOut;

    private int adults;
    private int children;

    @Enumerated(EnumType.STRING)
    private ReservationStatus status;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "actor_id")
    private UUID createdBy;

    protected Reservation() {
    }

    public static Reservation create(
            UUID hotel,
            UUID branch,
            String reference,
            UUID customer,
            UUID folio,
            LocalDate checkIn,
            LocalDate checkOut,
            int adults,
            int children,
            String notes,
            UUID actor) {

        if (!checkOut.isAfter(
                checkIn
        )) {

            throw new IllegalArgumentException(
                    "Check-out must be after check-in."
            );
        }

        var reservation =
                new Reservation();

        reservation.hotelId = hotel;
        reservation.branchId = branch;
        reservation.reservationReference = reference;
        reservation.bookingCustomerId = customer;
        reservation.folioId = folio;
        reservation.checkIn = checkIn;
        reservation.checkOut = checkOut;
        reservation.adults = adults;
        reservation.children = children;
        reservation.notes = notes;
        reservation.createdBy = actor;
        reservation.status =
                ReservationStatus.CONFIRMED;

        return reservation;
    }

    public UUID getBookingCustomerId() {
        return bookingCustomerId;
    }

    public LocalDate getCheckIn() {
        return checkIn;
    }

    public LocalDate getCheckOut() {
        return checkOut;
    }

    public String getReservationReference() {
        return reservationReference;
    }

    public int getAdults() {
        return adults;
    }

    public int getChildren() {
        return children;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public String getNotes() {
        return notes;
    }

    public UUID getFolioId() {
        return folioId;
    }

    public void confirmEditable() {

        if (status
                != ReservationStatus.CONFIRMED) {

            throw new IllegalStateException(
                    "Reservation is not confirmed."
            );
        }
    }

    public void checkIn() {

        confirmEditable();

        status =
                ReservationStatus.CHECKED_IN;
    }

    public void checkOut() {

        if (status
                != ReservationStatus.CHECKED_IN) {

            throw new IllegalStateException(
                    "Reservation is not checked in."
            );
        }

        status =
                ReservationStatus.CHECKED_OUT;
    }

    public void noShow() {

        if (status
                != ReservationStatus.CONFIRMED) {

            throw new IllegalStateException(
                    "Only confirmed reservations can become no-show."
            );
        }

        status =
                ReservationStatus.NO_SHOW;
    }

    public void reschedule(
            LocalDate newCheckIn,
            LocalDate newCheckOut) {

        confirmEditable();

        if (newCheckIn == null
                || newCheckOut == null
                || !newCheckOut.isAfter(
                        newCheckIn
                )) {

            throw new IllegalArgumentException(
                    "Check-out must be after check-in."
            );
        }

        checkIn =
                newCheckIn;

        checkOut =
                newCheckOut;
    }

    public void cancel() {

        if (status
                == ReservationStatus.CHECKED_IN
                || status
                == ReservationStatus.CHECKED_OUT) {

            throw new IllegalStateException(
                    "Stay cannot be cancelled."
            );
        }

        status =
                ReservationStatus.CANCELLED;
    }
}
