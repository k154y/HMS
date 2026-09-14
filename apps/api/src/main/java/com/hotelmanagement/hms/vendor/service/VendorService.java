package com.hotelmanagement.hms.vendor.service;
import com.hotelmanagement.hms.vendor.dto.*;
import com.hotelmanagement.hms.vendor.model.*;
import com.hotelmanagement.hms.vendor.repository.*;
import com.hotelmanagement.hms.shared.service.OperationScope;
import com.hotelmanagement.hms.shared.web.*;
import com.hotelmanagement.hms.audit.service.AuditService;
import static com.hotelmanagement.hms.identity.authorization.model.PermissionCode.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@Service @Transactional
public class VendorService {
 private final VendorRepository repository;private final OperationScope scope;private final AuditService audit;
 public VendorService(VendorRepository repository,OperationScope scope,AuditService audit){this.repository=repository;this.scope=scope;this.audit=audit;}
 public VendorResponse create(UUID hotel,VendorRequest r){
  UUID actor=scope.hotel(hotel,VENDOR_MANAGE);var e=repository.saveAndFlush(Vendor.create(hotel,r.code(),r.name(),r.email(),r.phone(),r.address(),r.paymentTermsDays(),r.active()));
  audit.record(hotel,null,actor,"VENDOR_CREATED","VENDOR",e.getId());return VendorResponse.from(e);
 }
 @Transactional(readOnly=true) public VendorResponse get(UUID hotel,UUID id){scope.hotel(hotel,VENDOR_VIEW);return VendorResponse.from(repository.findByIdAndHotelId(id,hotel).orElseThrow(ApiException::notFound));}
 @Transactional(readOnly=true) public PageResponse<VendorResponse> list(UUID hotel,int page,int size){scope.hotel(hotel,VENDOR_VIEW);return PageResponse.from(repository.findByHotelId(hotel,scope.page(page,size)).map(VendorResponse::from));}
}
