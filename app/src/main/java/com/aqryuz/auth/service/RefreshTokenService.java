package com.aqryuz.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.aqryuz.auth.config.AppProperties;
import com.aqryuz.auth.entity.RefreshToken;
import com.aqryuz.auth.entity.User;
import com.aqryuz.auth.repository.RefreshTokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final AppProperties appProperties;
    private final SecureRandom secureRandom = new SecureRandom();
    private final DeviceDetectionService deviceDetectionService;

    /**
     * Create a new refresh token for a user
     */
    public RefreshToken createRefreshToken(User user, boolean rememberMe,
            HttpServletRequest request) {
        // Deactivate existing tokens if user has too many active sessions
        long activeTokens = refreshTokenRepository.countActiveTokensForUser(user);
        int maxSessions = appProperties.deviceManagement().maxSessionsPerUser();

        if (activeTokens >= maxSessions) {
            // Deactivate oldest tokens
            List<RefreshToken> userTokens = refreshTokenRepository.findByUserAndActiveTrue(user);
            userTokens.stream().sorted((t1, t2) -> t1.getCreatedAt().compareTo(t2.getCreatedAt()))
                    .limit(activeTokens - maxSessions + 1).forEach(token -> {
                        token.setActive(false);
                        refreshTokenRepository.save(token);
                    });

            log.info("Deactivated old tokens for user: {} (exceeded max sessions: {})",
                    user.getUsername(), maxSessions);
        }

        // Generate secure token
        String token = generateSecureToken();
        String tokenHash = hashToken(token);

        // Calculate expiry based on remember me
        LocalDateTime expiryDate = rememberMe ? LocalDateTime.now().plusDays(30) // 30 days for
                                                                                 // remember me
                : LocalDateTime.now().plusHours(24); // 24 hours for regular session

        // Extract device information
        String userAgent = request.getHeader("User-Agent");
        String ipAddress = getClientIpAddress(request);
        String deviceFingerprint = generateDeviceFingerprint(userAgent, ipAddress);

        RefreshToken refreshToken = RefreshToken.builder().token(token).tokenHash(tokenHash)
                .user(user).expiryDate(expiryDate).createdAt(LocalDateTime.now())
                .lastUsedAt(LocalDateTime.now()).deviceFingerprint(deviceFingerprint)
                .ipAddress(ipAddress).userAgent(userAgent).rememberMe(rememberMe).active(true)
                .build();

        RefreshToken saved = refreshTokenRepository.save(refreshToken);

        log.info("Created refresh token for user: {} (rememberMe: {}, expires: {})",
                user.getUsername(), rememberMe, expiryDate);

        return saved;
    }

    /**
     * Validate and refresh a token
     */
    public Optional<RefreshToken> validateAndRefreshToken(String token,
            HttpServletRequest request) {
        String tokenHash = hashToken(token);

        Optional<RefreshToken> refreshTokenOpt =
                refreshTokenRepository.findByTokenHashAndActiveTrue(tokenHash);

        if (refreshTokenOpt.isEmpty()) {
            log.warn("Invalid or inactive refresh token");
            return Optional.empty();
        }

        RefreshToken refreshToken = refreshTokenOpt.get();

        if (refreshToken.isExpired()) {
            log.warn("Refresh token expired for user: {}", refreshToken.getUser().getUsername());
            refreshToken.setActive(false);
            refreshTokenRepository.save(refreshToken);
            return Optional.empty();
        }

        // Security check: verify device fingerprint hasn't changed significantly
        String currentFingerprint = generateDeviceFingerprint(request.getHeader("User-Agent"),
                getClientIpAddress(request));

        if (!refreshToken.getDeviceFingerprint().equals(currentFingerprint)) {
            log.warn("Device fingerprint mismatch for user: {} - possible token theft",
                    refreshToken.getUser().getUsername());
            // For now, we'll allow it but log it. In production, you might want to:
            // 1. Deactivate the token
            // 2. Send security alert
            // 3. Require re-authentication
        }

        // Update last used timestamp
        refreshToken.setLastUsedAt(LocalDateTime.now());

        // Token rotation: generate new token
        String newToken = generateSecureToken();
        String newTokenHash = hashToken(newToken);

        refreshToken.setToken(newToken);
        refreshToken.setTokenHash(newTokenHash);

        RefreshToken updated = refreshTokenRepository.save(refreshToken);

        log.debug("Refreshed token for user: {}", refreshToken.getUser().getUsername());

        return Optional.of(updated);
    }

    /**
     * Revoke a specific refresh token
     */
    public void revokeToken(String token) {
        String tokenHash = hashToken(token);
        Optional<RefreshToken> refreshTokenOpt =
                refreshTokenRepository.findByTokenHashAndActiveTrue(tokenHash);

        if (refreshTokenOpt.isPresent()) {
            RefreshToken refreshToken = refreshTokenOpt.get();
            refreshToken.setActive(false);
            refreshTokenRepository.save(refreshToken);

            log.info("Revoked refresh token for user: {}", refreshToken.getUser().getUsername());
        }
    }

    /**
     * Revoke all refresh tokens for a user (logout from all devices)
     */
    public void revokeAllTokensForUser(User user) {
        refreshTokenRepository.deactivateAllTokensForUser(user);
        log.info("Revoked all refresh tokens for user: {}", user.getUsername());
    }

    /**
     * Get all active sessions for a user
     */
    public List<RefreshToken> getActiveSessionsForUser(User user) {
        return refreshTokenRepository.findByUserAndActiveTrue(user);
    }

    /**
     * Scheduled cleanup of expired tokens
     */
    @Scheduled(fixedRate = 3600000) // Every hour
    public void cleanupExpiredTokens() {
        refreshTokenRepository.deactivateExpiredTokens(LocalDateTime.now());
        log.debug("Cleaned up expired refresh tokens");
    }

    /**
     * Generate a cryptographically secure token
     */
    private String generateSecureToken() {
        byte[] randomBytes = new byte[32]; // 256 bits
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    /**
     * Hash a token using SHA-256
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashedBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Generate device fingerprint for security
     */
    private String generateDeviceFingerprint(String userAgent, String ipAddress) {
        String combined = (userAgent != null ? userAgent : "unknown") + "|"
                + (ipAddress != null ? ipAddress : "unknown");
        return hashToken(combined).substring(0, 16); // Take first 16 chars of hash
    }

    /**
     * Extract client IP address from request
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIP = request.getHeader("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty()) {
            return xRealIP;
        }

        return request.getRemoteAddr();
    }
}
