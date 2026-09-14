package com.hotelmanagement.hms.reservation.model;
import jakarta.persistence.*; import java.time.*; import java.util.*;
@Entity @Table(name="reservation_rooms")
public class ReservationRoom {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id; @Column(name="hotel_id") private UUID hotelId; @Column(name="branch_id") private UUID branchId; @Column(name="reservation_id") private UUID reservationId; @Column(name="room_id") private UUID roomId; @Column(name="check_in") private LocalDate reservationCheckIn; @Column(name="check_out") private LocalDate reservationCheckOut; private boolean active=true;
 @Column(name="nightly_rate",precision=19,scale=4) private java.math.BigDecimal roomRate; private int adults; private int children; @Column(name="created_at") private Instant createdAt=Instant.now();
 protected ReservationRoom(){}
 public static ReservationRoom create(UUID h,UUID b,UUID r,UUID room,LocalDate in,LocalDate out,java.math.BigDecimal rate,int a,int c){var e=new ReservationRoom();e.hotelId=h;e.branchId=b;e.reservationId=r;e.roomId=room;e.reservationCheckIn=in;e.reservationCheckOut=out;e.roomRate=rate;e.adults=a;e.children=c;return e;}
 public UUID getId(){return id;} public UUID getRoomId(){return roomId;} public java.math.BigDecimal getRoomRate(){return roomRate;} public int getAdults(){return adults;} public int getChildren(){return children;}
 public java.math.BigDecimal getNightlyRate(){return roomRate;} public boolean isActive(){return active;} public void release(){active=false;}
}
