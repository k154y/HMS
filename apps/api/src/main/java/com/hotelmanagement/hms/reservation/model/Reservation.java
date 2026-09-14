package com.hotelmanagement.hms.reservation.model;
import jakarta.persistence.*; import java.time.*; import java.util.*;
@Entity @Table(name="reservations")
public class Reservation extends com.hotelmanagement.hms.shared.model.BranchEntity {
 @Column(name="reference") private String reservationReference; @Column(name="customer_id") private UUID bookingCustomerId; @Column(name="folio_id") private UUID folioId;
 @Column(name="check_in") private LocalDate checkIn; @Column(name="check_out") private LocalDate checkOut;
 private int adults; private int children; @Enumerated(EnumType.STRING) private ReservationStatus status;
 @Column(columnDefinition="text") private String notes; @Column(name="actor_id") private UUID createdBy;
 protected Reservation(){}
 public static Reservation create(UUID hotel,UUID branch,String ref,UUID customer,UUID folio,LocalDate in,LocalDate out,int adults,int children,String notes,UUID actor) {
  if(!out.isAfter(in)) throw new IllegalArgumentException("Check-out must be after check-in.");
  var e=new Reservation();e.hotelId=hotel;e.branchId=branch;e.reservationReference=ref;e.bookingCustomerId=customer;e.folioId=folio;e.checkIn=in;e.checkOut=out;e.adults=adults;e.children=children;e.notes=notes;e.createdBy=actor;e.status=ReservationStatus.CONFIRMED;return e;
 }
 public UUID getBookingCustomerId(){return bookingCustomerId;} public LocalDate getCheckIn(){return checkIn;} public LocalDate getCheckOut(){return checkOut;}
 public String getReservationReference(){return reservationReference;} public int getAdults(){return adults;} public int getChildren(){return children;} public ReservationStatus getStatus(){return status;} public String getNotes(){return notes;}
 public UUID getFolioId(){return folioId;}
 public void confirmEditable(){if(status!=ReservationStatus.CONFIRMED)throw new IllegalStateException("Reservation is not confirmed.");}
 public void checkIn(){if(status!=ReservationStatus.CONFIRMED)throw new IllegalStateException("Reservation is not confirmed.");status=ReservationStatus.CHECKED_IN;}
 public void checkOut(){if(status!=ReservationStatus.CHECKED_IN)throw new IllegalStateException("Reservation is not checked in.");status=ReservationStatus.CHECKED_OUT;}
 public void cancel(){if(status==ReservationStatus.CHECKED_IN||status==ReservationStatus.CHECKED_OUT)throw new IllegalStateException("Stay cannot be cancelled.");status=ReservationStatus.CANCELLED;}
}
