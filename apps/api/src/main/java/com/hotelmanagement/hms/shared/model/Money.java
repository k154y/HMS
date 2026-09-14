package com.hotelmanagement.hms.shared.model;
import java.math.*;
public final class Money {
    private Money() {}
    public static BigDecimal amount(BigDecimal value) {
        if(value==null || value.precision()-value.scale()>15) throw new IllegalArgumentException("Invalid amount.");
        return value.setScale(4,RoundingMode.HALF_UP);
    }
    public static BigDecimal positive(BigDecimal value) {
        var result=amount(value);
        if(result.signum()<=0) throw new IllegalArgumentException("Amount must be positive.");
        return result;
    }
    public static BigDecimal nonnegative(BigDecimal value) {
        var result=amount(value);
        if(result.signum()<0) throw new IllegalArgumentException("Amount must not be negative.");
        return result;
    }
}
