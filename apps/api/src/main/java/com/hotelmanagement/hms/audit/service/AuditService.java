package com.hotelmanagement.hms.audit.service;

import com.hotelmanagement.hms.audit.dto.AuditEventResponse;
import com.hotelmanagement.hms.audit.model.AuditEvent;
import com.hotelmanagement.hms.audit.repository.AuditEventRepository;
import com.hotelmanagement.hms.identity.authorization.model.PermissionCode;
import com.hotelmanagement.hms.identity.authorization.service.AuthorizationGuard;
import org.slf4j.MDC;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.util.UUID;

@Service
public class AuditService {
    private final AuditEventRepository events;
    private final AuthorizationGuard guard;
    public AuditService(AuditEventRepository events, AuthorizationGuard guard) {
        this.events = events; this.guard = guard;
    }
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(UUID hotel, UUID branch, UUID actor, String action, String type, UUID entity) {
        detailed(hotel,branch,actor,action,type,entity,null,null,null,null);
    }
    @Transactional(propagation = Propagation.MANDATORY)
    public void detailed(UUID hotel,UUID branch,UUID actor,String action,String type,UUID entity,String before,String after,String reason,String status){
        var attributes=org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
        String device=null,address=null;
        if(attributes instanceof org.springframework.web.context.request.ServletRequestAttributes servlet){var request=servlet.getRequest();device=request.getHeader("User-Agent");address=request.getRemoteAddr();}
        if(device!=null&&device.length()>500)device=device.substring(0,500);
        events.save(new AuditEvent(hotel,branch,actor,action,type,entity,MDC.get("requestId")).details(before,after,reason,status,device,address));
    }
    @Transactional(readOnly = true)
    public Page<AuditEventResponse> list(UUID actor, UUID hotel, int page, int size) {
        guard.requireHotelPermission(actor, hotel, PermissionCode.AUDIT_VIEW);
        if (page < 0 || size < 1 || size > 100) throw new IllegalArgumentException("Invalid page.");
        return events.findByHotelId(hotel, PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("occurredAt"), Sort.Order.asc("id")))).map(AuditEventResponse::from);
    }
}
