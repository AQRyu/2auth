package com.aqryuz.auth.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.aqryuz.auth.dto.LoginRequest;
import com.aqryuz.auth.dto.LoginResponse;
import com.aqryuz.auth.dto.UserInfo;
import com.aqryuz.auth.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtService jwtService;
    private final UserSessionService userSessionService;

    public LoginResponse authenticate(LoginRequest request, HttpServletRequest httpRequest) {
        try {
            // First, authenticate username/password
            Authentication authentication =
                    authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                            request.getUsernameOrEmail(), request.getPassword()));

            User user = (User) authentication.getPrincipal();

            // Check if TOTP is enabled
            if (user.isTotpEnabled()) {
                if (request.getTotpCode() == null || request.getTotpCode().isBlank()) {
                    // TOTP is required but not provided
                    return LoginResponse.builder().requireTotp(true).user(UserInfo.fromUser(user))
                            .build();
                }

                // Verify TOTP code
                if (!userService.verifyTotpCode(user.getUsername(), request.getTotpCode())) {
                    userService.incrementFailedLoginAttempts(user.getUsername());
                    throw new BadCredentialsException("Invalid TOTP code");
                }
            }

            // Generate tokens with session ID
            String accessToken = jwtService.generateToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);

            // Extract session ID from access token
            String sessionId = jwtService.extractClaim(accessToken,
                    claims -> (String) claims.get(JwtService.CLAIM_SESSION_ID));

            // Create user session for multi-device tracking
            userSessionService.createSession(user, sessionId, httpRequest);

            // Update last login
            userService.updateLastLogin(user.getUsername());

            log.info("User authenticated successfully: {}", user.getUsername());

            return LoginResponse.builder().accessToken(accessToken).refreshToken(refreshToken)
                    .tokenType("Bearer").expiresIn(86400) // 24 hours in seconds
                    .user(UserInfo.fromUser(user)).requireTotp(false).build();

        } catch (AuthenticationException e) {
            // Only increment failed attempts if user exists
            userService.findByUsernameOrEmail(request.getUsernameOrEmail()).ifPresent(
                    user -> userService.incrementFailedLoginAttempts(user.getUsername()));
            log.error("Authentication failed for user: {}", request.getUsernameOrEmail());
            throw new BadCredentialsException("Invalid credentials");
        }
    }

    public LoginResponse refreshToken(String refreshToken) {
        try {
            String username = jwtService.extractUsername(refreshToken);
            User user = (User) userService.loadUserByUsername(username);

            if (jwtService.isTokenValid(refreshToken, user)) {
                String newAccessToken = jwtService.generateToken(user);
                String newRefreshToken = jwtService.generateRefreshToken(user);

                return LoginResponse.builder().accessToken(newAccessToken)
                        .refreshToken(newRefreshToken).tokenType("Bearer").expiresIn(86400)
                        .user(UserInfo.fromUser(user)).requireTotp(false).build();
            } else {
                throw new BadCredentialsException("Invalid refresh token");
            }
        } catch (Exception e) {
            log.error("Token refresh failed: {}", e.getMessage());
            throw new BadCredentialsException("Invalid refresh token");
        }
    }
}
