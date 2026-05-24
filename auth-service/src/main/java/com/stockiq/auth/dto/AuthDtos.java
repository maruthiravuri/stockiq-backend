package com.stockiq.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthDtos {

    public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 3, max = 50) String username,
        @NotBlank @Size(min = 8, max = 100) String password
    ) {}

    public record LoginRequest(
        @NotBlank String usernameOrEmail,
        @NotBlank String password
    ) {}

    public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UserInfo user
    ) {}

    public record RefreshRequest(@NotBlank String refreshToken) {}

    public record UserInfo(
        String id,
        String email,
        String username,
        String role
    ) {}

    public record MessageResponse(String message) {}
}
