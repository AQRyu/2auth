package com.aqryuz.auth.service;

import org.springframework.stereotype.Service;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CookieService {

    private static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";
    private static final int REMEMBER_ME_MAX_AGE = 30 * 24 * 60 * 60; // 30 days in seconds
    private static final int SESSION_MAX_AGE = -1; // Session cookie (expires when browser closes)

    /**
     * Create an HTTP-only, secure refresh token cookie
     */
    public void createRefreshTokenCookie(HttpServletResponse response, String refreshToken,
            boolean rememberMe) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken);

        // Security settings
        cookie.setHttpOnly(true); // Prevent XSS attacks
        cookie.setSecure(true); // Only send over HTTPS (set to false for development)
        cookie.setPath("/"); // Available for entire application
        cookie.setAttribute("SameSite", "Strict"); // CSRF protection

        // Set expiry based on remember me
        if (rememberMe) {
            cookie.setMaxAge(REMEMBER_ME_MAX_AGE); // 30 days
            log.debug("Created persistent refresh token cookie (30 days)");
        } else {
            cookie.setMaxAge(SESSION_MAX_AGE); // Session cookie
            log.debug("Created session refresh token cookie");
        }

        response.addCookie(cookie);
    }

    /**
     * Get refresh token from cookie
     */
    public String getRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (REFRESH_TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    /**
     * Clear refresh token cookie (for logout)
     */
    public void clearRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0); // Expire immediately
        response.addCookie(cookie);

        log.debug("Cleared refresh token cookie");
    }

    /**
     * Check if refresh token cookie exists
     */
    public boolean hasRefreshTokenCookie(HttpServletRequest request) {
        return getRefreshTokenFromCookie(request) != null;
    }
}
