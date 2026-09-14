package com.hotelmanagement.hms.customer.model;
import jakarta.persistence.*;
import java.util.UUID;
import java.time.*;
import java.math.BigDecimal;
@Entity @Table(name="guests")
public class Guest extends com.hotelmanagement.hms.shared.model.HotelEntity {
    @Column(name="full_name") private String fullName;
    @Column(name="date_of_birth") private LocalDate dateOfBirth;
    @Column(name="nationality") private String nationality;
    @Column(name="phone") private String phone;
    protected Guest() {}
    public static Guest create(UUID hotelId, String fullName, LocalDate dateOfBirth, String nationality, String phone) {
        var e=new Guest();
        e.hotelId=hotelId;
        e.fullName=fullName;
        e.dateOfBirth=dateOfBirth;
        e.nationality=nationality;
        e.phone=phone;
        return e;
    }
    public String getFullName() { return fullName; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public String getNationality() { return nationality; }
    public String getPhone() { return phone; }

}
