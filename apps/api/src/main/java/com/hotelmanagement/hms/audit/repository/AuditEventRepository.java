package com.hotelmanagement.hms.audit.repository;
import com.hotelmanagement.hms.audit.model.AuditEvent;
import org.springframework.data.domain.*;
import org.springframework.data.repository.Repository;
import java.util.UUID;
public interface AuditEventRepository extends Repository<AuditEvent, UUID> {
    AuditEvent save(AuditEvent event);
    Page<AuditEvent> findByHotelId(UUID hotelId, Pageable pageable);
}
