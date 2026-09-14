package com.hotelmanagement.hms.stay.service;
import com.hotelmanagement.hms.stay.model.Stay;
import com.hotelmanagement.hms.stay.repository.StayRepository;
import com.hotelmanagement.hms.reservation.model.*;
import com.hotelmanagement.hms.reservation.repository.*;
import com.hotelmanagement.hms.room.model.HousekeepingState;
import com.hotelmanagement.hms.room.repository.RoomRepository;
import com.hotelmanagement.hms.platform.repository.HotelRepository;
import com.hotelmanagement.hms.folio.service.FolioService;
import com.hotelmanagement.hms.folio.model.EntryKind;
import com.hotelmanagement.hms.shared.service.OperationScope;
import com.hotelmanagement.hms.shared.web.ApiException;
import com.hotelmanagement.hms.audit.service.AuditService;
import static com.hotelmanagement.hms.identity.authorization.model.PermissionCode.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.math.BigDecimal;
@Service @Transactional
public class StayService {
 private final ReservationRepository reservations;private final ReservationRoomRepository allocations;private final RoomRepository rooms;
 private final StayRepository stays;private final FolioService folios;private final HotelRepository hotels;private final OperationScope scope;private final AuditService audit;
 public StayService(ReservationRepository reservations,ReservationRoomRepository allocations,RoomRepository rooms,StayRepository stays,FolioService folios,HotelRepository hotels,OperationScope scope,AuditService audit){
  this.reservations=reservations;this.allocations=allocations;this.rooms=rooms;this.stays=stays;this.folios=folios;this.hotels=hotels;this.scope=scope;this.audit=audit;
 }
 public record StayResponse(UUID id,UUID roomId,UUID folioId,Instant checkedInAt,Instant checkedOutAt){}
 private StayResponse response(Stay s){return new StayResponse(s.getId(),s.getRoomId(),s.getFolioId(),s.getCheckedInAt(),s.getCheckedOutAt());}
 public List<StayResponse> checkIn(UUID hotel,UUID branch,UUID id){
  UUID actor=scope.branch(hotel,branch,CHECKIN_PERFORM);var r=reservations.lock(id,hotel,branch).orElseThrow(ApiException::notFound);r.confirmEditable();
  LocalDate today=LocalDate.now(ZoneId.of(hotels.findById(hotel).orElseThrow(ApiException::notFound).getTimezone()));
  if(today.isBefore(r.getCheckIn()) || !today.isBefore(r.getCheckOut()))throw new IllegalStateException("Outside booked stay dates.");
  var assigned=allocations.findByReservationIdAndHotelIdAndBranchIdAndActiveTrueOrderByRoomId(id,hotel,branch);
  if(assigned.isEmpty())throw new IllegalStateException("No room allocation.");
  var result=new ArrayList<StayResponse>();
  for(var allocation:assigned){
   var room=rooms.lock(allocation.getRoomId(),hotel,branch).orElseThrow(ApiException::notFound);
   if(!room.canOccupy())throw new IllegalStateException("Room is not ready.");
   var stay=stays.saveAndFlush(Stay.create(hotel,branch,id,room.getId(),r.getFolioId(),Instant.now(),null,actor,null));
   BigDecimal charge=allocation.getNightlyRate().multiply(BigDecimal.valueOf(ChronoUnit.DAYS.between(r.getCheckIn(),r.getCheckOut())));
   if(charge.signum()>0)folios.post(hotel,branch,r.getFolioId(),charge,EntryKind.ACCOMMODATION,allocation.getId(),"Accommodation "+room.getCode(),actor);
   result.add(response(stay));
  }
  r.checkIn();audit.record(hotel,branch,actor,"CHECKED_IN","RESERVATION",id);return result;
 }
 public List<StayResponse> checkOut(UUID hotel,UUID branch,UUID id){
  UUID actor=scope.branch(hotel,branch,CHECKOUT_PERFORM);var r=reservations.lock(id,hotel,branch).orElseThrow(ApiException::notFound);
  if(r.getStatus()!=ReservationStatus.CHECKED_IN)throw new IllegalStateException("Reservation is not checked in.");
  var active=stays.findByReservationIdAndHotelIdAndBranchId(id,hotel,branch).stream().sorted(Comparator.comparing(Stay::getRoomId)).toList();
  for(var stay:active)rooms.lock(stay.getRoomId(),hotel,branch).orElseThrow(ApiException::notFound).housekeeping(HousekeepingState.DIRTY);
  folios.closeSettled(hotel,branch,r.getFolioId());
  active.forEach(s->s.checkOut(actor));r.checkOut();
  allocations.findByReservationIdAndHotelIdAndBranchIdAndActiveTrueOrderByRoomId(id,hotel,branch).forEach(ReservationRoom::release);
  audit.record(hotel,branch,actor,"CHECKED_OUT","RESERVATION",id);return active.stream().map(this::response).toList();
 }
}
