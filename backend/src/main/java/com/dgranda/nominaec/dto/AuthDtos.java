package com.dgranda.nominaec.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(
            @NotBlank @Size(max = 60) String username,
            @NotBlank @Size(max = 100) String password) {
    }

    public record LoginResponse(String token, String tokenType, long expiresInSeconds, String username, String role) {
    }
}
