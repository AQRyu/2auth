package com.aqryuz.auth.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.aqryuz.auth.config.AppProperties;
import com.aqryuz.auth.dto.DeviceSessionDto;
import com.aqryuz.auth.entity.User;
import com.aqryuz.auth.entity.UserSession;
import com.aqryuz.auth.repository.UserSessionRepository;
import com.aqryuz.auth.service.DeviceDetectionService.DeviceInfo;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserSessionService {

    private final UserSessionRepository sessionRepository;
    private final DeviceDetectionService deviceDetectionService;
    private final AppProperties appProperties;

    @Transactional
    public UserSession createSession(User user, String jwtTokenId, HttpServletRequest request) {
        // Check session limits before creating new session
        checkSessionLimits(user);
        
        DeviceInfo deviceInfo = deviceDetectionService.extractDeviceInfo(
                request.getHeader

        String location = "";
                roperties.deviceManagement().trackLocation()) {
            location = deviceDetectionService.getLocationFromIp(ipAddress);
        }
        
        UserSession session = UserSession.builder()
         

                .jwtTokenId(jwtTokenId).deviceName(deviceInfo.getDeviceName())
                .deviceType.browserName(deviceInfo.browserVersion(deviceInfo.getBrowserVersion())
                .operatingSystem(deviceInfo.getOperatin.ipAddress(ipAddress)
                .location(location)
                .isActive(true).createdAt(LocalDateTime.now())
                .lastActivity(Local.expiresAt(Loca.build();
        
        UserSession savedSession = sessionRepository.save(session);
        log.info("Created

        
        return savedSession; 
                

        nsactional
    public void revokeSession(String sessionId, String reason) {
        sessionRepository.revokeSession(sessionId, reason, LocalDateTime.now());
        log.info("Session {} revoked with reason: {}", sessionId, reason);
    }

    @Transactional
    public void revokeSession(Long sessionId, String username, String reason) {
        UserSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        
        if (!session.getUser().getUsername().equals(username)) {
            throw new RuntimeException("Unauthorized session access");

        
        sessionRepository.revokeSession(sessionId, LocalDateTime.now(), reason);
        l

        
    @Transactional
    public void revokeAllOtherSessions(User user, Long currentSessionId, String revokedBy) {
        sessionRepository.revokeAllOtherSessions(user, currentSessionId, LocalDateTime.now(), revokedBy);
        log.info("All other sessions revoked for user: {} by: {}", user.getUsername(), revokedBy);
    }

                
    @Transactional
    public void revokeAllUserSessions(User user, String revokedBy, String reason) {
        sessionRepository.revokeAllUserSessions(user, LocalDateTime.now(), revokedBy + " - " + reason);
        log.info("All sessions revoked for user: {} by: {} for reason: {}", user.getUsername(), revokedBy, reason);
    }

                
    @Transactional(readOnly = true)
                
    public List<DeviceSessionDto> getUserActiveSessions(User user) {
        List<UserSession> sessions = sessionRepository.findActiveSessionsByUser(user);
        
        return sessions.stream()
                .map(this::convertToDto)

        al(readOnly = true)
    public Optional<UserSession> findActiveSessionBySessionId(String sessionId) {
        return sessionRepository.findBySessionIdAndIsActiveTrue(sessionId);
    }

    @Transactional(readOnly = true)
    public Optional<UserSession> findActiveSessionByJwtTokenId(String jwtTokenId) {
        return sessionRepository.findByJwtTokenIdAndIsActiveTrue(jwtTokenId);
    }

    @Transactional
    public void updateSessionActivity(String sessionId) {
        sessionRepository.updateLastActivity(sessionId, LocalDateTime.now());
    }

    @Scheduled(fixedRate = 300000) // Run every 5 minutes
    @Transactional
    public void cleanupExpiredSessions() {
        sessionRepository.deactivateExpiredSessions(LocalDateTime.now());
        log.debug("Cleaned up expired sessions");
    }

    private void checkSessionLimits(User user) {
        long activeSessionCount = sessionRepository.countActiveSessionsByUser(user);
        
        if (activeSessionCount >= appProperties.deviceManagement().maxSessionsPerUser()) {
            if (!appProperties.deviceManagement().allowMultipleDevices()) {

                sessionRepository.revokeAllUserSessions(user, LocalDateTime.now(), "System - New login - single device policy");
            } else {
                // Find and revoke the oldest session
                List<UserSession> activeSessions = sessionRepository.findActiveSes
                        ionsByUser(user);
                if (!activeSessions.isEmpty()) {
                    UserSession oldestSession = activeSessions.stream()
                            .min((s1, s2) -> s1.getLastActivity().compareTo(s2.getLastActivity()))
                            .orElse(null);
                    
                    if (oldestSession != null) {
                        sessionRepository.

                    
            }
                                
                                
        }
    }

    private DeviceSessionDto convertToDto(UserSession session) {
        return DeviceSessionDto.builder()
                .sessionId(session.getId())
                .deviceName(session.getDeviceName())
                .deviceType(session.getDe.browserName(session.getBrowserName())
                .browserVersion(session.getBrowserVe.operatingSystem(session.getOperatingSystem())
                .ipAddress(session.getIpAddress()).location(session.getLocation())
                .lastActivity(session.getLastActivity()).createdAt(session.getCreatedAt())
                .isCurrentSession(false) // Will.build();
    }
                                                                           // 
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        i

        }
        
        return request.getRemoteAddr();
    }

        
