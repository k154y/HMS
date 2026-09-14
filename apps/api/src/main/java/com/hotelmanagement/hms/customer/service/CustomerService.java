package com.hotelmanagement.hms.customer.service;
import com.hotelmanagement.hms.customer.dto.*;
import com.hotelmanagement.hms.customer.model.*;
import com.hotelmanagement.hms.customer.repository.*;
import com.hotelmanagement.hms.shared.service.OperationScope;
import com.hotelmanagement.hms.shared.web.*;
import com.hotelmanagement.hms.audit.service.AuditService;
import static com.hotelmanagement.hms.identity.authorization.model.PermissionCode.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@Service @Transactional
public class CustomerService {
 private final CustomerRepository customers; private final GuestRepository guests;
 private final OperationScope scope; private final AuditService audit;
 public CustomerService(CustomerRepository customers,GuestRepository guests,OperationScope scope,AuditService audit) {
  this.customers=customers; this.guests=guests; this.scope=scope; this.audit=audit;
 }
 public CustomerResponse create(UUID hotel,CustomerRequest r) {
  UUID actor=scope.hotel(hotel,CUSTOMER_MANAGE);
  var e=customers.saveAndFlush(Customer.create(hotel,r.code().trim().toUpperCase(Locale.ROOT),r.kind(),r.name().trim(),r.email(),r.phone(),r.address(),r.taxNumber(),r.active()));
  audit.record(hotel,null,actor,"CUSTOMER_CREATED","CUSTOMER",e.getId()); return CustomerResponse.from(e);
 }
 public CustomerResponse update(UUID hotel,UUID id,CustomerRequest r) {
  UUID actor=scope.hotel(hotel,CUSTOMER_MANAGE); var e=customers.lock(id,hotel).orElseThrow(ApiException::notFound);
  if(!e.getCode().equals(r.code()) || e.getKind()!=r.kind()) throw new IllegalArgumentException("Customer code and kind are immutable.");
  e.update(r.name().trim(),r.email(),r.phone(),r.address(),r.taxNumber(),r.active());
  audit.record(hotel,null,actor,"CUSTOMER_UPDATED","CUSTOMER",id); return CustomerResponse.from(e);
 }
 @Transactional(readOnly=true) public CustomerResponse get(UUID hotel,UUID id) {
  scope.hotel(hotel,CUSTOMER_VIEW); return CustomerResponse.from(customers.findByIdAndHotelId(id,hotel).orElseThrow(ApiException::notFound));
 }
 @Transactional(readOnly=true) public PageResponse<CustomerResponse> list(UUID hotel,int page,int size) {
  scope.hotel(hotel,CUSTOMER_VIEW); return PageResponse.from(customers.findByHotelId(hotel,scope.page(page,size)).map(CustomerResponse::from));
 }
 public GuestResponse guest(UUID hotel,GuestRequest r) {
  UUID actor=scope.hotel(hotel,CUSTOMER_MANAGE);
  var e=guests.saveAndFlush(Guest.create(hotel,r.fullName().trim(),r.dateOfBirth(),r.nationality(),r.phone()));
  audit.record(hotel,null,actor,"GUEST_CREATED","GUEST",e.getId()); return GuestResponse.from(e);
 }
 @Transactional(readOnly=true) public PageResponse<GuestResponse> guests(UUID hotel,int page,int size) {
  scope.hotel(hotel,CUSTOMER_VIEW); return PageResponse.from(guests.findByHotelId(hotel,scope.page(page,size)).map(GuestResponse::from));
 }
}
