package com.hotelmanagement.hms.purchase.repository; import com.hotelmanagement.hms.purchase.model.*; import org.springframework.data.jpa.repository.*; import java.util.*; public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder,UUID>{@Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE) Optional<PurchaseOrder> findByIdAndHotelIdAndBranchId(UUID id,UUID h,UUID b);}

