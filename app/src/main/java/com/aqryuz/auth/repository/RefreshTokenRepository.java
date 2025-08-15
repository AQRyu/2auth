package com.aqryuz.auth.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.aqryuz.auth.entity.RefreshToken;
import com.aqryuz.auth.entity.User;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenAndActiveTrue(String token);

    Optional<RefreshToken> findByTokenHashAndActiveTrue(String tokenHash);

    List<RefreshToken> findByUserAndActiveTrue(User user);

    List<RefreshToken> findByUserAndRememberMeTrueAndActiveTrue(User user);

    @Modifying
    @Query("UPDATE RefreshToken r SET r.active = false WHERE r.user = :user")
    void deactivateAllTokensForUser(@Param("user") User user);

    @Modifying
    @Query("UPDATE RefreshToken r SET r.active = false WHERE r.expiryDate < :now")
    void deactivateExpiredTokens(@Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE RefreshToken r SET r.active = false WHERE r.id = :id")
    void deactivateToken(@Param("id") Long id);

    @Query("SELECT COUNT(r) FROM RefreshToken r WHERE r.user = :user AND r.active = true")
    long countActiveTokensForUser(@Param("user") User user);

    @Query("SELECT r FROM RefreshToken r WHERE r.user = :user AND r.deviceFingerprint = :fingerprint AND r.active = true")
    List<RefreshToken> findByUserAndDeviceFingerprintAndActiveTrue(@Param("user") User user,
            @Param("fingerprint") String deviceFingerprint);
}
