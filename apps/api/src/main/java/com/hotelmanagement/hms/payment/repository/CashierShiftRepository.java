package com.hotelmanagement.hms.payment.repository; import com.hotelmanagement.hms.payment.model.*; import org.springframework.data.jpa.repository.*; import java.util.*;
public interface CashierShiftRepository extends JpaRepository<CashierShift,UUID>{@Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE) @Query("select s from CashierShift s where s.id=:id") Optional<CashierShift> lock(UUID id); Optional<CashierShift> findByHotelIdAndBranchIdAndCashierUserIdAndStatus(UUID h,UUID b,UUID u,CashierShiftStatus s);}

