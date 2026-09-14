package com.hotelmanagement.hms.identity.administration.dto;
import jakarta.validation.constraints.*;
public record RoleRequest(@NotBlank @Pattern(regexp="[A-Z][A-Z0-9_]{0,99}") String code,
        @NotBlank @Size(max=150) String name, @Size(max=2000) String description, boolean active) {}
