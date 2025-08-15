package com.aqryuz.auth.controller;

import java.util.Map;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.aqryuz.auth.dto.LoginResponse;
import com.aqryuz.auth.dto.UserInfo;
import com.aqryuz.auth.entity.RefreshToken;
import com.aqryuz.auth.service.CookieService;
import com.aqryuz.auth.service.JwtService;
import com.aqryuz.auth.service.RefreshTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Token Refresh", description = "Secure token refresh using HTTP-only cookies")
public class RefreshController {

    private final RefreshTokenService refreshTokenService;
    private final CookieService cookieService;
    private final JwtService jwtService;

    @PostMapping("/refresh")
    @Operation(summary = "Refresh Access Token",
            description = "Get a new access token using the refresh token stored in HTTP-only cookie")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token refreshed successfully",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token",
                    content = @Content(mediaType = "application/json"))})
    public ResponseEntity<?> refreshToken(HttpServletRequest request,
            HttpServletResponse response) {
        try {
            // Get refresh token from HTTP-only cookie
            String refreshToken = cookieService.getRefreshTokenFromCookie(request);

            if (refreshToken == null) {
                log.warn("No refresh token found in cookie");
                return ResponseEntity.status(401).body(
                        Map.of("error", "No refresh token found", "code", "NO_REFRESH_TOKEN"));
            }

            // Validate and rotate the refresh token
            Optional<RefreshToken> validatedTokenOpt =
                    refreshTokenService.validateAndRefreshToken(refreshToken, request);

            if (validatedTokenOpt.isEmpty()) {
                log.warn("Invalid or expired refresh token");
                cookieService.clearRefreshTokenCookie(response);
                return ResponseEntity.status(401).body(Map.of("error",
                        "Invalid or expired refresh token", "code", "INVALID_REFRESH_TOKEN"));
            }

            RefreshToken validatedToken = validatedTokenOpt.get();

            // Generate new access token
            String newAccessToken = jwtService.generateToken(validatedToken.getUser());

            // Update the refresh token cookie with the new rotated token
            cookieService.createRefreshTokenCookie(response, validatedToken.getToken(),
                    validatedToken.isRememberMe());

            log.info("Successfully refreshed token for user: {}",
                    validatedToken.getUser().getUsername());

            return ResponseEntity.ok(LoginResponse.builder().accessToken(newAccessToken)
                    .refreshToken(null) // Don't send refresh token in response
                    .tokenType("Bearer").expiresIn(900) // 15 minutes
                    .user(UserInfo.fromUser(validatedToken.getUser())).requireTotp(false).build());

        } catch (Exception e) {
            log.error("Error refreshing token: {}", e.getMessage());
            cookieService.clearRefreshTokenCookie(response);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Internal server error", "code", "REFRESH_ERROR"));
        }
    }
}
