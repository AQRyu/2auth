package com.aqryuz.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Login request containing user credentials")
public class LoginRequest {

    @NotBlank(message = "Username or email is required")
    @Schema(description = "Username or email address", example = "admin@homelab.local")
    private String usernameOrEmail;

    @NotBlank(message = "Password is required")
    @Schema(description = "User password", example = "admin123")
    private String password;

    @Schema(description = "TOTP code for two-factor authentication", example = "123456")
    private String totpCode;
}
