package com.aqryuz.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Login response containing authentication tokens and user information")
public class LoginResponse {

    @Schema(description = "JWT access token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String accessToken;

    @Schema(description = "JWT refresh token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String refreshToken;

    @Schema(description = "Token type", example = "Bearer")
    private String tokenType;

    @Schema(description = "Token expiration time in seconds", example = "86400")
    private long expiresIn;

    @Schema(description = "User information")
    private UserInfo user;

    @Schema(description = "Whether TOTP is required for this user", example = "false")
    private boolean requireTotp;

    @Schema(description = "TOTP QR code for setup (if required)",
            example = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA...")
    private String totpQrCode;
}
