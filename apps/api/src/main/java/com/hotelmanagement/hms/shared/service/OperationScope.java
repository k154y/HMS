package com.hotelmanagement.hms.shared.service;
import com.hotelmanagement.hms.identity.authentication.context.AuthenticatedUserContext;
import com.hotelmanagement.hms.identity.authorization.model.PermissionCode;
import com.hotelmanagement.hms.identity.authorization.service.AuthorizationGuard;
import com.hotelmanagement.hms.platform.repository.BranchRepository;
import com.hotelmanagement.hms.shared.web.ApiException;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Component;
import java.util.UUID;
@Component
public class OperationScope {
    private final AuthenticatedUserContext context;
    private final AuthorizationGuard guard;
    private final BranchRepository branches;
    public OperationScope(AuthenticatedUserContext context, AuthorizationGuard guard, BranchRepository branches) {
        this.context=context; this.guard=guard; this.branches=branches;
    }
    public UUID hotel(UUID hotel, PermissionCode permission) {
        UUID actor=context.requireCurrentUserId(); guard.requireHotelPermission(actor,hotel,permission); return actor;
    }
    public UUID branch(UUID hotel, UUID branch, PermissionCode permission) {
        UUID actor=context.requireCurrentUserId(); guard.requireBranchPermission(actor,hotel,branch,permission);
        if(!branches.findByIdAndHotel_Id(branch,hotel).orElseThrow(ApiException::notFound).isActive())
            throw new ApiException(409,"BRANCH_INACTIVE","Branch is inactive.");
        return actor;
    }
    public PageRequest page(int page,int size) {
        if(page<0 || size<1 || size>100) throw new IllegalArgumentException("Invalid page.");
        return PageRequest.of(page,size,Sort.by("createdAt","id"));
    }
}
