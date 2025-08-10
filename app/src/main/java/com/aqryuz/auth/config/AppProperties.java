package com.aqryuz.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(JwtProperties jwt, TotpProperties totp, OAuth2Properties oauth2,
                AdminProperties admin, SecurityProperties security, CorsProperties cors) {

        public record JwtProperties(String secret, long expiration, long refreshExpiration,
                        long slidingWindowMinutes, boolean enableSlidingWindow,
                        long maxSessionDurationMinutes) {
        }

        public record TotpProperties(String issuer, int period, int digits) {
        }

        public record OAuth2Properties(String clientId, String clientSecret, String redirectUri,
                        String scope) {
        }

        public record AdminProperties(String defaultUsername, String defaultPassword,
                        String defaultEmail) {
        }

        public record SecurityProperties(int maxFailedAttempts, long lockoutDurationMinutes,
                        boolean progressiveLockout) {
        }

        public record CorsProperties(String allowedOrigins, String allowedMethods,
                        String allowedHeaders, boolean allowCredentials, long maxAge) {
        }
}
