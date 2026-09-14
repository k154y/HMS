package com.hotelmanagement.hms.shared.model;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@MappedSuperclass
public abstract class HotelEntity {
    @Id @GeneratedValue(strategy=GenerationType.UUID) protected UUID id;
    @Column(name="hotel_id",nullable=false,updatable=false) protected UUID hotelId;
    @Column(name="created_at",nullable=false,updatable=false) protected Instant createdAt = Instant.now();
    @Version protected long version;
    public UUID getId() { return id; }
    public UUID getHotelId() { return hotelId; }
    public Instant getCreatedAt() { return createdAt; }
}
