package com.crustcrunch.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(min = 8, max = 100, message = "must be at least 8 characters") String password,
            @NotBlank @Size(max = 120) String fullName,
            @Pattern(regexp = "^[+0-9 ()-]{7,30}$", message = "is not a valid phone number") String phone) {
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
    }

    public record AuthResponse(String token, long expiresInSeconds, UserDtos.UserResponse user) {
    }
}
