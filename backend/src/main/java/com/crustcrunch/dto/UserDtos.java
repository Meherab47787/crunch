package com.crustcrunch.dto;

import com.crustcrunch.enums.Role;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;

public final class UserDtos {

    private UserDtos() {
    }

    public record UserResponse(Long id, String email, String fullName, String phone, Role role, boolean enabled,
                               Instant createdAt) {
    }

    public record UpdateProfileRequest(
            @NotBlank @Size(max = 120) String fullName,
            @Pattern(regexp = "^[+0-9 ()-]{7,30}$", message = "is not a valid phone number") String phone) {
    }

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 8, max = 100, message = "must be at least 8 characters") String newPassword) {
    }

    public record AddressRequest(
            @NotBlank @Size(max = 50) String label,
            @NotBlank @Size(max = 200) String line1,
            @Size(max = 200) String line2,
            @NotBlank @Size(max = 100) String city,
            @Size(max = 20) String postalCode,
            @Size(max = 300) String instructions,
            boolean defaultAddress) {
    }

    public record AddressResponse(Long id, String label, String line1, String line2, String city, String postalCode,
                                  String instructions, boolean defaultAddress) {
    }

    // ----- admin -----

    public record StaffCreateRequest(
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(max = 120) String fullName,
            @NotBlank @Size(min = 8, max = 100) String password,
            @NotNull Role role,
            String phone) {
    }

    public record UserAdminUpdateRequest(@NotNull Role role, boolean enabled) {
    }

    public record CustomerResponse(UserResponse user, long orderCount, BigDecimal totalSpent) {
    }
}
