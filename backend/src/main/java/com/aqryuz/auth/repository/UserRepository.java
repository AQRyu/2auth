package com.aqryuz.auth.repository;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.aqryuz.auth.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.username = ?1 OR u.email = ?1")
    Optional<User> findByUsernameOrEmail(String usernameOrEmail);

    @Modifying
    @Query("UPDATE User u SET u.lastLogin = :lastLogin, u.failedLoginAttempts = 0, u.updatedAt = :updatedAt WHERE u.username = :username")
    int updateLastLogin(@Param("username") String username,
            @Param("lastLogin") LocalDateTime lastLogin,
            @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = :attempts, u.updatedAt = :updatedAt WHERE u.username = :username")
    int updateFailedLoginAttempts(@Param("username") String username,
            @Param("attempts") int attempts, @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying
    @Query("UPDATE User u SET u.accountLocked = true, u.failedLoginAttempts = :attempts, u.updatedAt = :updatedAt WHERE u.username = :username")
    int lockAccount(@Param("username") String username, @Param("attempts") int attempts,
            @Param("updatedAt") LocalDateTime updatedAt);
}
