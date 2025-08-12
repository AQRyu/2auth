package com.aqryuz.auth.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.aqryuz.auth.entity.User;
import com.aqryuz.auth.entity.UserSession;
import com.aqryuz.auth.repository.UserSessionRepository;
import com.aqryuz.auth.service.DeviceDetectionService.DeviceInfo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserSessionService {

    private final UserSessionRepository userSessionRepository;
    private final DeviceDetectionService deviceDetectionService;

    public UserSession createSession(User user, String sessionId, HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        String ipAddress = getClientIpAddress(request);

        DeviceInfo deviceInfo = deviceDetectionService.extractDeviceInfo(userAgent);
        String location = deviceDetectionService.getLocationFromIp(ipAddress);

        UserSession session = UserSession.builder().user(user).sessionId(sessionId)
                .jwtTokenId(sessionId) // Using sessionId as jwtTokenId for now
                .deviceName(deviceInfo.getDeviceName()).deviceType(deviceInfo.getDeviceType())
                .browserName(deviceInfo.getBrowserName())
                .browserVersion(deviceInfo.getBrowserVersion())
                .operatingSystem(deviceInfo.getOperatingSystem()).ipAddress(ipAddress)
                .location(location).userAgent(userAgent).isActive(true)
                .expiresAt(LocalDateTime.now().plusDays(1)) // 24 hours
                .build();

        UserSession savedSession = userSessionRepository.save(session);
        log.info("Created new session for user: {} with session ID: {}", user.getUsername(),
                sessionId);

        return savedSession;
    }

    public Optional<UserSession> findActiveSession(String sessionId) {
        return userSessionRepository.findBySessionIdAndIsActiveTrue(sessionId);
    }

    public List<UserSession> findActiveSessionsByUser(User user) {
        return userSessionRepository.findByUserAndIsActiveTrueOrderByLastActivityDesc(user);
    }

    public List<UserSession> findActiveSessionsByUserId(Long userId) {
        return userSessionRepository.findByUserIdAndIsActiveTrueOrderByLastActivityDesc(userId);
    }

    public void revokeSession(String sessionId, String revokedBy) {
        Optional<UserSession> session =
                userSessionRepository.findBySessionIdAndIsActiveTrue(sessionId);
        if (session.isPresent()) {
            UserSession userSession = session.get();
            userSession.setActive(false);
            userSession.setRevokedAt(LocalDateTime.now());
            userSession.setRevokedBy(revokedBy);
            userSessionRepository.save(userSession);
            log.info("Revoked session: {} by: {}", sessionId, revokedBy);
        }
    }

    public void revokeAllUserSessions(User user, String revokedBy) {
        List<UserSession> activeSessions = findActiveSessionsByUser(user);
        for (UserSession session : activeSessions) {
            session.setActive(false);
            session.setRevokedAt(LocalDateTime.now());
            session.setRevokedBy(revokedBy);
        }
        userSessionRepository.saveAll(activeSessions);
        log.info("Revoked all sessions for user: {} by: {}", user.getUsername(), revokedBy);
    }

    public void updateSessionActivity(String sessionId) {
        Optional<UserSession> session =
                userSessionRepository.findBySessionIdAndIsActiveTrue(sessionId);
        if (session.isPresent()) {
            UserSession userSession = session.get();
            userSession.setLastActivity(LocalDateTime.now());
            userSessionRepository.save(userSession);
        }
    }

    public long countActiveSessionsByUser(User user) {
        return userSessionRepository.countActiveSessionsByUser(user);
    }

    public void cleanupExpiredSessions() {
        // This method can be called by a scheduled task to clean up expired sessions
        log.info("Cleaning up expired sessions");
        // Implementation would query and deactivate expired sessions
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()
                && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}
