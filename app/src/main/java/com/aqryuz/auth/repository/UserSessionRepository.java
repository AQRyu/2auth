package com.aqryuz.auth.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.aqryuz.auth.entity.User;
import com.aqryuz.auth.entity.UserSession;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    Optional<UserSession> findBySessionIdAndIsActiveTrue(String sessionId);

    Optional<UserSession> findByJwtTokenIdAndIsActiveTrue(String jwtTokenId);

    List<UserSession> findByUserAndIsActiveTrueOrderByLastActivityDesc(User user);

    List<UserSession> findByUserIdAndIsActiveTrueOrderByLastActivityDesc(Long userId);

    @Query("SELECT COUNT(s) FROM UserSession s WHERE s.user = :user AND s.isActive = true AND s.expiresAt > :now")
    long countActiveSessionsByUser(@Param("user") User user, @Param("now") LocalDateTime now);

    @Query("SELECT COUNT(s) FROM UserSession s WHERE s.user = :user AND s.isActive = true")
    long countActiveSessionsByUser(@Param("user") User user);

    @Query("SELECT s FROM UserSession s WHERE s.user = :user AND s.isActive = true AND s.expiresAt > :now ORDER BY s.lastActivity DESC")
    List<UserSession> findActiveSessionsByUser(@Param("user") User user,
            @Param("now") LocalDateTime now);

    @Query("SELECT s FROM UserSession s WHERE s.user = :user AND s.isActive = true ORDER BY s.lastActivity DESC")
    List<UserSession> findActiveSessionsByUser(@Param("user") User user);

    @Modifying
    @Query("UPDATE UserSession s SET s.isActive = false, s.revokedAt = :revokedAt, s.revokedBy = :revokedBy WHERE s.id = :sessionId")
    void revokeSession(@Param("sessionId") Long sessionId,
            @Param("revokedAt") LocalDateTime revokedAt, @Param("revokedBy") String revokedBy);

    @Modifying
    @Query("UPDATE UserSession s SET s.isActive = false, s.revokedAt = :revokedAt, s.revokedBy = :revokedBy WHERE s.sessionId = :sessionId")
    void revokeSession(@Param("sessionId") String sessionId, @Param("revokedBy") String revokedBy,
            @Param("revokedAt") LocalDateTime revokedAt);

    @Modifying
    @Query("UPDATE UserSession s SET s.isActive = false, s.revokedAt = :revokedAt, s.revokedBy = :revokedBy WHERE s.user = :user AND s.id != :excludeSessionId AND s.isActive = true")
    void revokeAllOtherSessions(@Param("user") User user,
            @Param("excludeSessionId") Long excludeSessionId,
            @Param("revokedAt") LocalDateTime revokedAt, @Param("revokedBy") String revokedBy);

    @Modifying
    @Query("UPDATE UserSession s SET s.isActive = false, s.revokedAt = :revokedAt, s.revokedBy = :revokedBy WHERE s.user = :user AND s.isActive = true")
    void revokeAllUserSessions(@Param("user") User user,
            @Param("revokedAt") LocalDateTime revokedAt, @Param("revokedBy") String revokedBy);

    @Modifying
    @Query("UPDATE UserSession s SET s.lastActivity = :lastActivity WHERE s.sessionId = :sessionId")
    void updateLastActivity(@Param("sessionId") String sessionId,
            @Param("lastActivity") LocalDateTime lastActivity);

    @Modifying
    @Query("UPDATE UserSession s SET s.isActive = false WHERE s.expiresAt < :now AND s.isActive = true")
    void deactivateExpiredSessions(@Param("now") LocalDateTime now);

    @Query("SELECT s FROM UserSession s WHERE s.user = :user AND s.ipAddress = :ipAddress AND s.userAgent = :userAgent AND s.isActive = true")
    List<UserSession> findSimilarActiveSessions(@Param("user") User user,
            @Param("ipAddress") String ipAddress, @Param("userAgent") String userAgent);
}
