package com.hotelmanagement.hms.reservation.service;
import com.hotelmanagement.hms.reservation.dto.*; import com.hotelmanagement.hms.reservation.model.*; import com.hotelmanagement.hms.reservation.repository.*; import com.hotelmanagement.hms.customer.repository.CustomerRepository; import com.hotelmanagement.hms.room.repository.*; import com.hotelmanagement.hms.shared.service.OperationScope; import com.hotelmanagement.hms.shared.web.*; import com.hotelmanagement.hms.audit.service.AuditService; import static com.hotelmanagement.hms.identity.authorization.model.PermissionCode.*; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.*; import java.time.*; import java.util.*; import java.math.BigDecimal;
@Service @Transactional public class ReservationService {
 private final org.springframework.jdbc.core.JdbcTemplate db; private final ReservationRepository reservations; private final ReservationRoomRepository allocations; private final CustomerRepository customers; private final RoomRepository rooms; private final RoomTypeRepository roomTypes; private final OperationScope scope; private final AuditService audit; private final com.hotelmanagement.hms.folio.service.FolioService folios;
 public ReservationService(ReservationRepository r,ReservationRoomRepository a,CustomerRepository c,RoomRepository rooms,RoomTypeRepository roomTypes,OperationScope scope,AuditService audit,com.hotelmanagement.hms.folio.service.FolioService folios,org.springframework.jdbc.core.JdbcTemplate db){this.db=db;reservations=r;allocations=a;customers=c;this.rooms=rooms;this.roomTypes=roomTypes;this.scope=scope;this.audit=audit;this.folios=folios;}
 public ReservationResponse create(UUID hotel,UUID branch,ReservationRequest req){
  UUID actor=scope.branch(hotel,branch,RESERVATION_CREATE);
  if(req.checkIn()==null||req.checkOut()==null||!req.checkOut().isAfter(req.checkIn())||req.rooms()==null||req.rooms().isEmpty()||req.rooms().size()>20)throw new IllegalArgumentException("Invalid reservation dates or rooms.");
  int adults=0,children=0;var seen=new HashSet<UUID>();var guests=new HashSet<UUID>();
  for(var a:req.rooms()){
   if(a==null||a.adults()<1||a.children()<0||!seen.add(a.roomId()))throw new IllegalArgumentException("Invalid occupancy or duplicate room.");
   adults+=a.adults();children+=a.children();
   if(a.guestIds()!=null){if(a.guestIds().size()>a.adults()+a.children())throw new IllegalArgumentException("Too many guests.");for(var guest:a.guestIds())if(guest==null||!guests.add(guest))throw new IllegalArgumentException("Duplicate guest.");}
  }
  if(adults!=req.adults()||children!=req.children())throw new IllegalArgumentException("Room occupancy must match reservation totals.");
  customers.findByIdAndHotelId(req.bookingCustomerId(),hotel).orElseThrow(ApiException::notFound);
  String ref="RSV-"+UUID.randomUUID().toString().replace("-","").substring(0,12).toUpperCase(Locale.ROOT);
  var folio=folios.open(hotel,branch,req.bookingCustomerId(),actor);
  var reservation=reservations.saveAndFlush(Reservation.create(hotel,branch,ref,req.bookingCustomerId(),folio.getId(),req.checkIn(),req.checkOut(),req.adults(),req.children(),req.notes(),actor));
  var saved=new ArrayList<ReservationRoom>();var requested=new ArrayList<>(req.rooms());requested.sort(Comparator.comparing(ReservationRequest.RoomBooking::roomId));
  for(var a:requested){
   var room=rooms.lock(a.roomId(),hotel,branch).orElseThrow(ApiException::notFound);
   if(!room.getActive()||room.getOperational()!=com.hotelmanagement.hms.room.model.RoomOperationalState.AVAILABLE)throw new ApiException(409,"ROOM_UNAVAILABLE","Room is not operationally available.");
   if(a.adults()>room.getAdults()||a.children()>room.getChildren())throw new IllegalArgumentException("Occupancy exceeds room capacity.");
   BigDecimal rate=room.getNightlyRate();if(rate==null)rate=roomTypes.findByIdAndHotelIdAndBranchId(room.getRoomTypeId(),hotel,branch).orElseThrow(ApiException::notFound).getDefaultRate();
   if(a.rate()!=null&&a.rate().compareTo(rate)!=0){scope.branch(hotel,branch,ROOM_RATE_MANAGE);rate=com.hotelmanagement.hms.shared.model.Money.nonnegative(a.rate());}
   var allocation=allocations.saveAndFlush(ReservationRoom.create(hotel,branch,reservation.getId(),room.getId(),req.checkIn(),req.checkOut(),rate,a.adults(),a.children()));saved.add(allocation);
   if(a.guestIds()!=null)for(var guest:a.guestIds()){
    if(db.queryForList("select id from guests where id=? and hotel_id=?",guest,hotel).isEmpty())throw ApiException.notFound();
    db.update("insert into reservation_guests(id,hotel_id,branch_id,created_at,version,allocation_id,guest_id) values(?,?,?,CURRENT_TIMESTAMP,0,?,?)",UUID.randomUUID(),hotel,branch,allocation.getId(),guest);
   }
  }
  audit.record(hotel,branch,actor,"RESERVATION_CREATED","RESERVATION",reservation.getId());return ReservationResponse.from(reservation,saved);
 }
 @Transactional(readOnly=true) public PageResponse<com.hotelmanagement.hms.room.dto.RoomResponse> available(UUID hotel,UUID branch,LocalDate in,LocalDate out,int adults,int children,int page,int size){scope.branch(hotel,branch,RESERVATION_VIEW);if(adults<1||children<0||in==null||out==null||!out.isAfter(in))throw new IllegalArgumentException("Check-out must be after check-in.");return PageResponse.from(rooms.available(hotel,branch,in,out,adults,children,scope.page(page,size)).map(com.hotelmanagement.hms.room.dto.RoomResponse::from));}
 @Transactional(readOnly=true) public PageResponse<ReservationResponse> list(UUID hotel,UUID branch,int page,int size){scope.branch(hotel,branch,RESERVATION_VIEW);return PageResponse.from(reservations.findByHotelIdAndBranchId(hotel,branch,scope.page(page,size)).map(r->ReservationResponse.from(r,allocations.findByReservationId(r.getId()))));}
 @Transactional(readOnly=true) public ReservationResponse get(UUID hotel,UUID branch,UUID id){scope.branch(hotel,branch,RESERVATION_VIEW);var r=reservations.findByIdAndHotelIdAndBranchId(id,hotel,branch).orElseThrow(ApiException::notFound);return ReservationResponse.from(r,allocations.findByReservationId(id));}
 public void cancel(UUID hotel,UUID branch,UUID id){UUID actor=scope.branch(hotel,branch,RESERVATION_CANCEL);var r=reservations.lock(id,hotel,branch).orElseThrow(ApiException::notFound);r.cancel();allocations.findByReservationId(id).forEach(ReservationRoom::release);audit.record(hotel,branch,actor,"RESERVATION_CANCELLED","RESERVATION",id);}
}


