package com.aqryuz.auth.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import javax.crypto.SecretKey;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import com.aqryuz.auth.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class JwtService {

    private final AppProperties appProperties;
    private final TokenBlacklistService tokenBlacklistService;

    // Custom claims for session management
    private static final String CLAIM_FIRST_ISSUED = "firstIssued";
    private static final String CLAIM_LAST_ACTIVITY = "lastActivity";
    public static final String CLAIM_SESSION_ID = "sessionId";

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        // Add session management claims
        extraClaims.put(CLAIM_FIRST_ISSUED, System.currentTimeMillis());
        extraClaims.put(CLAIM_LAST_ACTIVITY, System.currentTimeMillis());
        extraClaims.put(CLAIM_SESSION_ID, java.util.UUID.randomUUID().toString());

        // Use short expiration for access tokens (15 minutes) for security
        return buildToken(extraClaims, userDetails, 900000L); // 15 minutes in milliseconds
    }

    public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_FIRST_ISSUED, System.currentTimeMillis());
        claims.put(CLAIM_SESSION_ID, java.util.UUID.randomUUID().toString());

        return buildToken(claims, userDetails, appProperties.jwt().refreshExpiration());
    }

    /**
     * Generate a new token with updated activity timestamp for sliding window sessions
     */
    public String refreshTokenForActivity(String token, UserDetails userDetails) {
        if (!appProperties.jwt().enableSlidingWindow()) {
            return token; // Return original token if sliding window is disabled
        }

        try {
            Claims claims = extractAllClaims(token);

            // Check if we're within sliding window threshold
            Long lastActivity = claims.get(CLAIM_LAST_ACTIVITY, Long.class);
            if (lastActivity != null) {
                long minutesSinceActivity = ChronoUnit.MINUTES
                        .between(Instant.ofEpochMilli(lastActivity), Instant.now());

                // Only refresh if activity is within sliding window
                if (minutesSinceActivity <= appProperties.jwt().slidingWindowMinutes()) {
                    // Check max session duration
                    Long firstIssued = claims.get(CLAIM_FIRST_ISSUED, Long.class);

                    if (firstIssued != null) {
                        long sessionDurationMinutes = ChronoUnit.MINUTES
                                .between(Instant.ofEpochMilli(firstIssued), Instant.now());

                        if (sessionDurationMinutes >= appProperties.jwt()
                                .maxSessionDurationMinutes()) {
                            log.info("Session exceeded maximum duration for user: {}",
                                    userDetails.getUsername());
                            return null; // Session expired due to max duration
                        }
                    }

                    // Create new token with updated activity
                    Map<String, Object> newClaims = new HashMap<>();
                    newClaims.put(CLAIM_FIRST_ISSUED, claims.get(CLAIM_FIRST_ISSUED));
                    newClaims.put(CLAIM_LAST_ACTIVITY, System.currentTimeMillis());
                    newClaims.put(CLAIM_SESSION_ID, claims.get(CLAIM_SESSION_ID));

                    log.debug("Refreshing token for sliding window activity for user: {}",
                            userDetails.getUsername());

                    return buildToken(newClaims, userDetails, appProperties.jwt().expiration());
                }
            }
        } catch (Exception e) {
            log.error("Error refreshing token for activity: {}", e.getMessage());
        }

        return token; // Return original token if refresh not needed or failed
    }

    private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails,
            long expiration) {
        return Jwts.builder().claims(extraClaims).subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey(), Jwts.SIG.HS256).compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        // Check if token is blacklisted first
        if (tokenBlacklistService.isTokenBlacklisted(token)) {
            log.debug("Token is blacklisted");
            return false;
        }

        final String username = extractUsername(token);
        if (!username.equals(userDetails.getUsername())) {
            return false;
        }

        if (isTokenExpired(token)) {
            return false;
        }

        // Check session duration limits
        return isSessionValid(token);
    }

    /**
     * Check if the session is still valid based on maximum session duration
     */
    private boolean isSessionValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            Long firstIssued = claims.get(CLAIM_FIRST_ISSUED, Long.class);

            if (firstIssued != null) {
                long sessionDurationMinutes = ChronoUnit.MINUTES
                        .between(Instant.ofEpochMilli(firstIssued), Instant.now());

                if (sessionDurationMinutes >= appProperties.jwt().maxSessionDurationMinutes()) {
                    log.debug("Session exceeded maximum duration: {} minutes",
                            sessionDurationMinutes);
                    return false;
                }
            }
        } catch (Exception e) {
            log.error("Error checking session validity: {}", e.getMessage());
            return false;
        }

        return true;
    }

    /**
     * Get session information from token
     */
    public Map<String, Object> getSessionInfo(String token) {
        try {
            Claims claims = extractAllClaims(token);
            Map<String, Object> sessionInfo = new HashMap<>();

            sessionInfo.put("username", claims.getSubject());
            sessionInfo.put("sessionId", claims.get(CLAIM_SESSION_ID));
            sessionInfo.put("firstIssued", claims.get(CLAIM_FIRST_ISSUED));
            sessionInfo.put("lastActivity", claims.get(CLAIM_LAST_ACTIVITY));
            sessionInfo.put("issuedAt", claims.getIssuedAt());
            sessionInfo.put("expiration", claims.getExpiration());

            return sessionInfo;
        } catch (Exception e) {
            log.error("Error extracting session info: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parser().verifyWith(getSignInKey()).build().parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            log.error("Error parsing JWT token: {}", e.getMessage());
            throw e;
        }
    }

    private SecretKey getSignInKey() {
        byte[] keyBytes = appProperties.jwt().secret().getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Blacklist a token (for logout functionality)
     */
    public void blacklistToken(String token) {
        tokenBlacklistService.blacklistToken(token);
    }
}
