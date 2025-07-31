package com.aqryuz.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(JwtProperties jwt, TotpProperties totp, OAuth2Properties oauth2,
        AdminProperties admin) {

    public record JwtProperties(String secret, long expiration, long refreshExpiration) {
    }

    public record TotpProperties(String issuer, int period, int digits) {
    }

    public record OAuth2Properties(String clientId, String clientSecret, String redirectUri,
            String scope) {
    }

    public record AdminProperties(String defaultUsername, String defaultPassword,
            String defaultEmail) {
    }
}
