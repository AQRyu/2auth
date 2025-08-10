package com.aqryuz.auth.controller;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.aqryuz.auth.service.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/auth/session")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Session Management", description = "Session timeout and management endpoints")
public class SessionController {

    private final JwtService jwtService;
    private static final String BEARER_PREFIX = "Bearer ";

    @GetMapping("/info")
    @Operation(summary = "Get Current Session Information",
            description = "Retrieve detailed information about the current user session")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Session information retrieved",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = "application/json"))})
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Map<String, Object>> getSessionInfo(
            @RequestHeader("Authorization") String authHeader, Authentication authentication) {

        try {
            if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
                return ResponseEntity.badRequest().build();
            }

            String token = authHeader.substring(7);
            Map<String, Object> sessionInfo = jwtService.getSessionInfo(token);

            // Add current user info
            if (authentication != null
                    && authentication.getPrincipal() instanceof UserDetails userDetails) {
                sessionInfo.put("currentUser", userDetails.getUsername());
                sessionInfo.put("authorities", userDetails.getAuthorities());
            }

            log.info("Session info requested for user: {}", sessionInfo.get("username"));
            return ResponseEntity.ok(sessionInfo);

        } catch (Exception e) {
            log.error("Error retrieving session info: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/refresh")
    @Operation(summary = "Manually Refresh Session",
            description = "Manually refresh the current session to extend its lifetime")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Session refreshed successfully",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "401", description = "Unauthorized or session expired",
                    content = @Content(mediaType = "application/json"))})
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Map<String, Object>> refreshSession(
            @RequestHeader("Authorization") String authHeader, Authentication authentication) {

        try {
            if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
                return ResponseEntity.badRequest().build();
            }

            String token = authHeader.substring(7);

            if (authentication != null
                    && authentication.getPrincipal() instanceof UserDetails userDetails) {
                String refreshedToken = jwtService.refreshTokenForActivity(token, userDetails);

                if (refreshedToken == null) {
                    log.info("Session refresh failed - session expired for user: {}",
                            userDetails.getUsername());
                    return ResponseEntity.status(401)
                            .body(Map.of("error", "Session expired", "code", "SESSION_EXPIRED"));
                }

                Map<String, Object> response = Map.of("message", "Session refreshed successfully",
                        "tokenRefreshed", !refreshedToken.equals(token), "newToken",
                        refreshedToken.equals(token) ? null : refreshedToken);

                log.info("Session manually refreshed for user: {}", userDetails.getUsername());
                return ResponseEntity.ok(response);
            }

            return ResponseEntity.badRequest().build();

        } catch (Exception e) {
            log.error("Error refreshing session: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/validate")
    @Operation(summary = "Validate Current Session",
            description = "Check if the current session is still valid and get remaining time")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Session validation result",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = "application/json"))})
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Map<String, Object>> validateSession(
            @RequestHeader("Authorization") String authHeader, Authentication authentication) {

        try {
            if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
                return ResponseEntity.badRequest().build();
            }

            String token = authHeader.substring(7);

            if (authentication != null
                    && authentication.getPrincipal() instanceof UserDetails userDetails) {
                boolean isValid = jwtService.isTokenValid(token, userDetails);
                Map<String, Object> sessionInfo = jwtService.getSessionInfo(token);

                Map<String, Object> response = Map.of("valid", isValid, "username",
                        userDetails.getUsername(), "sessionInfo", sessionInfo);

                return ResponseEntity.ok(response);
            }

            return ResponseEntity.badRequest().build();

        } catch (Exception e) {
            log.error("Error validating session: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}
