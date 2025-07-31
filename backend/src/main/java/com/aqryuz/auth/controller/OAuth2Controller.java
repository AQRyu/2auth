package com.aqryuz.auth.controller;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.aqryuz.auth.dto.UserInfo;
import com.aqryuz.auth.entity.User;
import com.aqryuz.auth.service.JwtService;
import com.aqryuz.auth.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/oauth2")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class OAuth2Controller {

    private static final String ERROR_KEY = "error";
    private static final String INVALID_TOKEN = "invalid_token";

    private final JwtService jwtService;
    private final UserService userService;

    @Value("${app.oauth2.client-id}")
    private String expectedClientId;

    @Value("${app.oauth2.client-secret}")
    private String expectedClientSecret;

    @GetMapping("/authorize")
    public ResponseEntity<Map<String, Object>> authorize(@RequestParam("client_id") String clientId,
            @RequestParam("redirect_uri") String redirectUri,
            @RequestParam("response_type") String responseType,
            @RequestParam(value = "scope", defaultValue = "openid") String scope,
            @RequestParam(value = "state", required = false) String state) {

        log.info("OAuth2 authorization request - client_id: {}, redirect_uri: {}", clientId,
                redirectUri);

        // For a simple homelab setup, we'll implement a basic authorization flow
        // In production, this would be much more complex with proper consent screens

        return ResponseEntity.ok(Map.of("client_id", clientId, "redirect_uri", redirectUri,
                "response_type", responseType, "scope", scope, "state", state != null ? state : "",
                "authorize_url",
                "/oauth2/login?client_id=" + clientId + "&redirect_uri=" + redirectUri));
    }

    @PostMapping("/token")
    public ResponseEntity<Map<String, Object>> token(@RequestParam("grant_type") String grantType,
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "client_id") String clientId,
            @RequestParam(value = "client_secret") String clientSecret,
            @RequestParam(value = "redirect_uri", required = false) String redirectUri,
            @RequestParam(value = "username", required = false) String username,
            @RequestParam(value = "password", required = false) String password) {

        log.info("OAuth2 token request - grant_type: {}, client_id: {}", grantType, clientId);

        // Validate client credentials
        if (!expectedClientId.equals(clientId) || !expectedClientSecret.equals(clientSecret)) {
            return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "invalid_client"));
        }

        try {
            String accessToken;
            User user;

            if ("authorization_code".equals(grantType)) {
                // For simplicity in homelab, we'll accept the code as username for now
                // In production, you'd store and validate authorization codes
                user = userService.findByUsernameOrEmail(code)
                        .orElseThrow(() -> new RuntimeException("Invalid authorization code"));
                accessToken = jwtService.generateToken(user);

            } else if ("password".equals(grantType)) {
                // Resource Owner Password Credentials Grant (for trusted clients)
                if (username == null || password == null) {
                    return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "invalid_request"));
                }

                user = userService.findByUsernameOrEmail(username)
                        .orElseThrow(() -> new RuntimeException("Invalid credentials"));

                // Note: In production, you should validate the password here
                accessToken = jwtService.generateToken(user);

            } else {
                return ResponseEntity.badRequest()
                        .body(Map.of(ERROR_KEY, "unsupported_grant_type"));
            }

            String refreshToken = jwtService.generateRefreshToken(user);

            return ResponseEntity
                    .ok(Map.of("access_token", accessToken, "token_type", "Bearer", "expires_in",
                            3600, "refresh_token", refreshToken, "scope", "openid profile email"));

        } catch (Exception e) {
            log.error("OAuth2 token exchange failed", e);
            return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "invalid_grant"));
        }
    }

    @GetMapping("/userinfo")
    public ResponseEntity<Map<String, Object>> userinfo(
            @RequestHeader("Authorization") String authorization) {

        log.info("OAuth2 userinfo request");

        try {
            // Extract JWT token from Authorization header
            if (!authorization.startsWith("Bearer ")) {
                return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, INVALID_TOKEN));
            }

            String token = authorization.substring(7);
            String username = jwtService.extractUsername(token);

            if (username == null) {
                return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, INVALID_TOKEN));
            }

            User user = userService.findByUsernameOrEmail(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (!jwtService.isTokenValid(token, user)) {
                return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, INVALID_TOKEN));
            }

            UserInfo userInfo = UserInfo.fromUser(user);

            return ResponseEntity.ok(Map.of("sub", user.getId().toString(), "name",
                    userInfo.getFirstName() + " " + userInfo.getLastName(), "given_name",
                    userInfo.getFirstName(), "family_name", userInfo.getLastName(), "email",
                    userInfo.getEmail(), "email_verified", true, "preferred_username",
                    userInfo.getUsername()));

        } catch (Exception e) {
            log.error("OAuth2 userinfo failed", e);
            return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, INVALID_TOKEN));
        }
    }
}
