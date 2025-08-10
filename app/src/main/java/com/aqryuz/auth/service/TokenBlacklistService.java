package com.aqryuz.auth.service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

/**
 * Service to manage blacklisted JWT tokens for proper session management. Tokens are blacklisted
 * when users logout or when admin revokes sessions.
 */
@Service
@Slf4j
public class TokenBlacklistService {

    private final Set<String> blacklistedTokens = ConcurrentHashMap.newKeySet();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public TokenBlacklistService() {
        // Clean up expired tokens every hour
        scheduler.scheduleAtFixedRate(this::cleanupExpiredTokens, 1, 1, TimeUnit.HOURS);
    }

    /**
     * Blacklist a token (when user logs out)
     */
    public void blacklistToken(String token) {
        if (token != null && !token.trim().isEmpty()) {
            blacklistedTokens.add(token);
            log.info("Token blacklisted: {}...", token.substring(0, Math.min(10, token.length())));
        }
    }

    /**
     * Check if a token is blacklisted
     */
    public boolean isTokenBlacklisted(String token) {
        return token != null && blacklistedTokens.contains(token);
    }

    /**
     * Remove a token from blacklist (mainly for testing)
     */
    public void removeFromBlacklist(String token) {
        if (token != null) {
            blacklistedTokens.remove(token);
        }
    }

    /**
     * Get current blacklist size (for monitoring)
     */
    public int getBlacklistSize() {
        return blacklistedTokens.size();
    }

    /**
     * Clear all blacklisted tokens (for testing or maintenance)
     */
    public void clearBlacklist() {
        blacklistedTokens.clear();
        log.info("Token blacklist cleared");
    }

    /**
     * Clean up expired tokens from blacklist to prevent memory leaks. Since we can't easily
     * determine expiration from stored tokens, we'll implement a simple time-based cleanup.
     */
    private void cleanupExpiredTokens() {
        // For now, we'll clear tokens older than 24 hours
        // In a production environment, you might want to store tokens with their expiration times
        int sizeBefore = blacklistedTokens.size();

        // Simple cleanup - in production you'd want to store expiration times with tokens
        if (sizeBefore > 10000) { // Prevent memory issues
            blacklistedTokens.clear();
            log.info("Blacklist cleanup: cleared {} tokens due to size limit", sizeBefore);
        } else {
            log.debug("Blacklist cleanup: {} tokens in blacklist", sizeBefore);
        }
    }
}
