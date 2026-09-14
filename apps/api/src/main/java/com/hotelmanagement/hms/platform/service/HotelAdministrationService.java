package com.hotelmanagement.hms.platform.service;

import com.hotelmanagement.hms.audit.service.AuditService;
import com.hotelmanagement.hms.identity.authorization.model.PermissionCode;
import com.hotelmanagement.hms.identity.authorization.service.AuthorizationGuard;
import com.hotelmanagement.hms.platform.dto.*;
import com.hotelmanagement.hms.platform.currency.dto.*;
import com.hotelmanagement.hms.platform.currency.repository.HotelExchangeRateRepository;
import com.hotelmanagement.hms.platform.currency.service.ExchangeRateService;
import com.hotelmanagement.hms.platform.repository.*;
import com.hotelmanagement.hms.shared.web.ApiException;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.UUID;

@Service
@Transactional
public class HotelAdministrationService {
    private final AuthorizationGuard guard;
    private final HotelRepository hotels;
    private final BranchRepository branches;
    private final BranchService branchService;
    private final ExchangeRateService rates;
    private final HotelExchangeRateRepository rateRepository;
    private final AuditService audit;
    public HotelAdministrationService(AuthorizationGuard guard, HotelRepository hotels, BranchRepository branches,
            BranchService branchService, ExchangeRateService rates, HotelExchangeRateRepository rateRepository, AuditService audit) {
        this.guard=guard; this.hotels=hotels; this.branches=branches; this.branchService=branchService;
        this.rates=rates; this.rateRepository=rateRepository; this.audit=audit;
    }
    @Transactional(readOnly=true)
    public HotelResponse get(UUID actor, UUID hotel) {
        guard.requireHotelPermission(actor,hotel,PermissionCode.HOTEL_SETTINGS_VIEW);
        return HotelResponse.from(hotels.findById(hotel).orElseThrow(ApiException::notFound));
    }
    public HotelResponse update(UUID actor, UUID hotel, CreateHotelRequest request) {
        guard.requireHotelPermission(actor,hotel,PermissionCode.HOTEL_SETTINGS_MANAGE);
        var entity = hotels.findLockedById(hotel).orElseThrow(ApiException::notFound);
        // Changing an accounting currency needs a separate migration workflow once transactions exist.
        if(!entity.getCode().equals(request.code().trim().toUpperCase(java.util.Locale.ROOT)) ||
                (request.currencyCode()!=null && !entity.getCurrencyCode().equals(request.currencyCode())))
            throw new ApiException(409,"IMMUTABLE_HOTEL_SETTING","Hotel code and accounting currency cannot be changed.");
        if(request.timezone()!=null) { ZoneId.of(request.timezone()); entity.setTimezone(request.timezone()); }
        if(request.defaultLanguage()!=null) entity.setDefaultLanguage(request.defaultLanguage());
        entity.setLegalName(request.legalName().trim()); entity.setDisplayName(request.displayName().trim());
        entity.setTin(request.tin()); entity.setPhone(request.phone()); entity.setEmail(request.email()); entity.setAddress(request.address());
        entity.setUpdatedAt(now());
        audit.record(hotel,null,actor,"HOTEL_SETTINGS_UPDATED","HOTEL",hotel);
        return HotelResponse.from(entity);
    }
    @Transactional(readOnly=true)
    public Page<BranchResponse> branches(UUID actor, UUID hotel, int page, int size) {
        guard.requireHotelPermission(actor,hotel,PermissionCode.BRANCH_VIEW);
        return branches.findAccessible(hotel,actor,page(page,size)).map(BranchResponse::from);
    }
    @Transactional(readOnly=true)
    public BranchResponse branch(UUID actor, UUID hotel, UUID branch) {
        guard.requireBranchPermission(actor,hotel,branch,PermissionCode.BRANCH_VIEW);
        return BranchResponse.from(branches.findByIdAndHotel_Id(branch,hotel).orElseThrow(ApiException::notFound));
    }
    public BranchResponse createBranch(UUID actor, UUID hotel, CreateBranchRequest request) {
        guard.requireHotelPermission(actor,hotel,PermissionCode.BRANCH_MANAGE);
        var result = branchService.createBranch(hotel,request);
        audit.record(hotel,result.id(),actor,"BRANCH_CREATED","BRANCH",result.id());
        return result;
    }
    public BranchResponse updateBranch(UUID actor, UUID hotel, UUID branch, CreateBranchRequest request) {
        guard.requireBranchPermission(actor,hotel,branch,PermissionCode.BRANCH_MANAGE);
        var entity = branches.findLockedInHotel(branch,hotel).orElseThrow(ApiException::notFound);
        entity.setCode(request.code().trim().toUpperCase(java.util.Locale.ROOT));
        entity.setName(request.name().trim()); entity.setPhone(request.phone()); entity.setEmail(request.email());
        entity.setAddress(request.address());
        if(request.timezone()!=null) { ZoneId.of(request.timezone()); entity.setTimezone(request.timezone()); }
        entity.setUpdatedAt(now());
        audit.record(hotel,branch,actor,"BRANCH_UPDATED","BRANCH",branch);
        return BranchResponse.from(entity);
    }
    public BranchResponse branchStatus(UUID actor, UUID hotel, UUID branch, boolean active) {
        guard.requireBranchPermission(actor,hotel,branch,PermissionCode.BRANCH_MANAGE);
        var entity=branches.findLockedInHotel(branch,hotel).orElseThrow(ApiException::notFound);
        entity.setActive(active); entity.setUpdatedAt(now());
        audit.record(hotel,branch,actor,active?"BRANCH_ACTIVATED":"BRANCH_DEACTIVATED","BRANCH",branch);
        return BranchResponse.from(entity);
    }
    public ExchangeRateResponse createRate(UUID actor, UUID hotel, ExchangeRateRequest request) {
        guard.requireHotelPermission(actor,hotel,PermissionCode.EXCHANGE_RATE_MANAGE);
        var result=rates.setExchangeRate(hotel,request);
        audit.record(hotel,null,actor,"EXCHANGE_RATE_CREATED","EXCHANGE_RATE",result.id());
        return result;
    }
    @Transactional(readOnly=true)
    public ExchangeRateResponse rate(UUID actor, UUID hotel, String currency, OffsetDateTime at) {
        guard.requireHotelPermission(actor,hotel,PermissionCode.EXCHANGE_RATE_VIEW);
        return rates.getApplicableRate(hotel,currency,at);
    }
    @Transactional(readOnly=true)
    public Page<ExchangeRateResponse> history(UUID actor, UUID hotel, int page, int size) {
        guard.requireHotelPermission(actor,hotel,PermissionCode.EXCHANGE_RATE_VIEW);
        if(page<0 || size<1 || size>100) throw new IllegalArgumentException("Invalid page.");
        return rateRepository.findByHotel_Id(hotel,PageRequest.of(page,size,Sort.by(
                Sort.Order.desc("effectiveFrom"),Sort.Order.asc("id")))).map(ExchangeRateResponse::from);
    }
    private PageRequest page(int page, int size) {
        if(page<0 || size<1 || size>100) throw new IllegalArgumentException("Invalid page.");
        return PageRequest.of(page,size,Sort.by("createdAt","id"));
    }
    private OffsetDateTime now() { return OffsetDateTime.now(ZoneOffset.UTC); }
}
