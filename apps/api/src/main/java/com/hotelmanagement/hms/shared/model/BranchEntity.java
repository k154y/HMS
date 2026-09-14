package com.hotelmanagement.hms.shared.model;
import jakarta.persistence.*;
import java.util.UUID;
@MappedSuperclass
public abstract class BranchEntity extends HotelEntity {
    @Column(name="branch_id",nullable=false,updatable=false) protected UUID branchId;
    public UUID getBranchId() { return branchId; }
}
