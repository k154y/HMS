package com.hotelmanagement.hms.customer.model;
import jakarta.persistence.*;
import java.util.UUID;
import java.time.*;
import java.math.BigDecimal;
@Entity @Table(name="customers")
public class Customer extends com.hotelmanagement.hms.shared.model.HotelEntity {
    @Column(name="code") private String code;
    @Enumerated(EnumType.STRING)
    @Column(name="kind") private CustomerKind kind;
    @Column(name="name") private String name;
    @Column(name="email") private String email;
    @Column(name="phone") private String phone;
    @Column(name="address",columnDefinition="text") private String address;
    @Column(name="tax_number") private String taxNumber;
    @Column(name="active") private boolean active;
    protected Customer() {}
    public static Customer create(UUID hotelId, String code, CustomerKind kind, String name, String email, String phone, String address, String taxNumber, boolean active) {
        var e=new Customer();
        e.hotelId=hotelId;
        e.code=code;
        e.kind=kind;
        e.name=name;
        e.email=email;
        e.phone=phone;
        e.address=address;
        e.taxNumber=taxNumber;
        e.active=active;
        return e;
    }
    public String getCode() { return code; }
    public CustomerKind getKind() { return kind; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public String getTaxNumber() { return taxNumber; }
    public boolean getActive() { return active; }
    public void update(String name,String email,String phone,String address,String taxNumber,boolean active) {
        this.name=name; this.email=email; this.phone=phone; this.address=address; this.taxNumber=taxNumber; this.active=active;
    }
}
