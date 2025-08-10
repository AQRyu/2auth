package com.aqryuz.auth.config;

import java.io.IOException;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.aqryuz.auth.service.JwtService;
import com.aqryuz.auth.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserService userService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);

        try {
            username = jwtService.extractUsername(jwt);

            if (username != null
                    && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userService.loadUserByUsername(username);

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    // Try to refresh token for sliding window activity
                    String refreshedToken = jwtService.refreshTokenForActivity(jwt, userDetails);

                    if (refreshedToken == null) {
                        // Session expired due to max duration
                        log.info("Session expired for user: {}", username);
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.getWriter().write(
                                "{\"error\":\"Session expired\",\"code\":\"SESSION_EXPIRED\"}");
                        response.setContentType("application/json");
                        return;
                    }

                    // Set authentication
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(userDetails, null,
                                    userDetails.getAuthorities());
                    authToken
                            .setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    // If token was refreshed, add it to response header
                    if (!refreshedToken.equals(jwt)) {
                        response.setHeader("Authorization", "Bearer " + refreshedToken);
                        response.setHeader("X-Token-Refreshed", "true");
                        log.debug("Token refreshed for sliding window activity for user: {}",
                                username);
                    }
                }
            }
        } catch (Exception e) {
            log.error("JWT authentication failed: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
