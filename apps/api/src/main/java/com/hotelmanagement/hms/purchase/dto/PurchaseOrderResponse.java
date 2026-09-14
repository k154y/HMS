package com.hotelmanagement.hms.purchase.dto; import com.hotelmanagement.hms.purchase.model.*; import java.util.*; public record PurchaseOrderResponse(UUID id,UUID vendorId,String reference,PurchaseOrderStatus status){public static PurchaseOrderResponse from(PurchaseOrder p){return new PurchaseOrderResponse(p.getId(),p.getVendorId(),p.getReference(),p.getStatus());}}

