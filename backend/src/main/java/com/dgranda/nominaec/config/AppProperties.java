package com.dgranda.nominaec.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/** Application settings; secrets come only from environment variables (see .env.example). */
@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @NotBlank String companyName,
        @Valid Jwt jwt,
        @Valid Cors cors,
        @Valid Bootstrap bootstrap,
        @Valid LoginRateLimit loginRateLimit,
        boolean demoData) {

    public record Jwt(@NotBlank @Size(min = 32, message = "JWT_SECRET debe tener al menos 32 caracteres (256 bits)") String secret,
                      @Min(5) @Max(240) int expirationMinutes,
                      @NotBlank String issuer) {
    }

    public record Cors(@NotEmpty List<String> allowedOrigins) {
    }

    /** Initial users created on first start when the table is empty. Passwords are hashed with BCrypt. */
    public record Bootstrap(@NotBlank String adminUsername,
                            @NotBlank @Size(min = 12, message = "ADMIN_PASSWORD debe tener al menos 12 caracteres") String adminPassword,
                            String demoUsername,
                            String demoPassword) {
    }

    public record LoginRateLimit(@Min(1) int maxAttempts, @Min(1) int windowSeconds) {
    }
}
