package com.aqryuz.auth.controller;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.aqryuz.auth.dto.LoginRequest;
import com.aqryuz.auth.dto.LoginResponse;
import com.aqryuz.auth.service.AuthenticationService;
import com.aqryuz.auth.service.CookieService;
import com.aqryuz.auth.service.JwtService;
import com.aqryuz.auth.service.RefreshTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Authentication and token management endpoints")
public class AuthController {

        private final AuthenticationService authenticationService;
        private final JwtService jwtService;
        private final CookieService cookieService;
        private final RefreshTokenService refreshTokenService;

        @PostMapping("/login")
        @Operation(summary = "User Login",
                        description = "Authenticate user with username/email and password")
        @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Login successful",
                        content = @Content(mediaType = "application/json",
                                        schema = @Schema(implementation = LoginResponse.class))),
                        @ApiResponse(responseCode = "401", description = "Invalid credentials",
                                        content = @Content(mediaType = "application/json")),
                        @ApiResponse(responseCode = "400", description = "Invalid request format",
                                        content = @Content(mediaType = "application/json"))})
        public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                        HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
                log.info("Login attempt for user: {}", request.getUsernameOrEmail());
                LoginResponse response = authenticationService.authenticate(request, httpRequest,
                                httpResponse);
                return ResponseEntity.ok(response);
        }

        @PostMapping("/logout")
        @Operation(summary = "User Logout",
                        description = "Logout user and invalidate refresh token")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Logout successful"),
                        @ApiResponse(responseCode = "400",
                                        description = "No active session found")})
        public ResponseEntity<Map<String, String>> logout(HttpServletRequest request,
                        HttpServletResponse response) {
                try {
                        String refreshToken = cookieService.getRefreshTokenFromCookie(request);

                        if (refreshToken != null) {
                                // Invalidate the refresh token
                                refreshTokenService.revokeToken(refreshToken);
                                log.info("Refresh token invalidated during logout");
                        }

                        // Clear the refresh token cookie
                        cookieService.clearRefreshTokenCookie(response);

                        // Also blacklist the access token if provided
                        String authHeader = request.getHeader("Authorization");
                        if (authHeader != null && authHeader.startsWith("Bearer ")) {
                                String accessToken = authHeader.substring(7);
                                jwtService.blacklistToken(accessToken);
                                log.info("Access token blacklisted during logout");
                        }

                        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));

                } catch (Exception e) {
                        log.error("Error during logout: {}", e.getMessage());
                        return ResponseEntity.status(500).body(
                                        Map.of("error", "Logout failed", "code", "LOGOUT_ERROR"));
                }
        }
}
