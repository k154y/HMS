package com.hotelmanagement.hms.audit.web;
import com.hotelmanagement.hms.audit.dto.AuditEventResponse;
import com.hotelmanagement.hms.audit.service.AuditService;
import com.hotelmanagement.hms.identity.authentication.context.AuthenticatedUserContext;
import com.hotelmanagement.hms.shared.web.PageResponse;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
@RestController
@RequestMapping("/api/v1/hotels/{hotelId}/audit-events")
public class AuditController {
    private final AuditService service;
    private final AuthenticatedUserContext user;
    public AuditController(AuditService service, AuthenticatedUserContext user) { this.service = service; this.user = user; }
    @GetMapping public PageResponse<AuditEventResponse> list(@PathVariable UUID hotelId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
        return PageResponse.from(service.list(user.requireCurrentUserId(), hotelId, page, size));
    }
}
