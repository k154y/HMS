package com.hotelmanagement.hms.vendor.model;
import jakarta.persistence.*;
import java.util.UUID;
import java.time.*;
import java.math.BigDecimal;
@Entity @Table(name="vendors")
public class Vendor extends com.hotelmanagement.hms.shared.model.HotelEntity {
    @Column(name="code") private String code;
    @Column(name="name") private String name;
    @Column(name="email") private String email;
    @Column(name="phone") private String phone;
    @Column(name="address",columnDefinition="text") private String address;
    @Column(name="payment_terms_days") private int paymentTermsDays;
    @Column(name="active") private boolean active;
    protected Vendor() {}
    public static Vendor create(UUID hotelId, String code, String name, String email, String phone, String address, int paymentTermsDays, boolean active) {
        var e=new Vendor();
        e.hotelId=hotelId;
        e.code=code;
        e.name=name;
        e.email=email;
        e.phone=phone;
        e.address=address;
        e.paymentTermsDays=paymentTermsDays;
        e.active=active;
        return e;
    }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public int getPaymentTermsDays() { return paymentTermsDays; }
    public boolean getActive() { return active; }

}
