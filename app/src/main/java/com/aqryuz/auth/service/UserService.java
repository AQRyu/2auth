package com.aqryuz.auth.service;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.aqryuz.auth.config.AppProperties;
import com.aqryuz.auth.dto.UserCreateRequest;
import com.aqryuz.auth.dto.UserInfo;
import com.aqryuz.auth.dto.UserUpdateRequest;
import com.aqryuz.auth.entity.User;
import com.aqryuz.auth.exception.UserNotFoundException;
import com.aqryuz.auth.exception.UserUpdateException;
import com.aqryuz.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService implements UserDetailsService {

    private static final String USER_NOT_FOUND = "User not found";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TotpService totpService;
    private final AppProperties appProperties;

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        return userRepository.findByUsernameOrEmail(usernameOrEmail).orElseThrow(
                () -> new UsernameNotFoundException("User not found: " + usernameOrEmail));
    }

    public UserInfo createUser(UserCreateRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = User.builder().username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword())).email(request.getEmail())
                .firstName(request.getFirstName()).lastName(request.getLastName())
                .accountEnabled(request.isAccountEnabled()).roles(request.getRoles()).build();

        User savedUser = userRepository.save(user);
        log.info("Created new user: {}", savedUser.getUsername());

        return UserInfo.fromUser(savedUser);
    }

    public UserInfo updateUser(Long userId, UserUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(USER_NOT_FOUND));

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new IllegalArgumentException("Email already exists");
            }
            user.setEmail(request.getEmail());
        }

        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }

        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getAccountEnabled() != null) {
            user.setAccountEnabled(request.getAccountEnabled());
        }

        if (request.getAccountLocked() != null) {
            user.setAccountLocked(request.getAccountLocked());
        }

        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            user.setRoles(request.getRoles());
        }

        User savedUser = userRepository.save(user);
        log.info("Updated user: {}", savedUser.getUsername());

        return UserInfo.fromUser(savedUser);
    }

    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(USER_NOT_FOUND));

        userRepository.delete(user);
        log.info("Deleted user: {}", user.getUsername());
    }

    public UserInfo getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(USER_NOT_FOUND));

        return UserInfo.fromUser(user);
    }

    public UserInfo getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException(USER_NOT_FOUND));

        return UserInfo.fromUser(user);
    }

    public Page<UserInfo> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(UserInfo::fromUser);
    }

    public String enableTotp(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException(USER_NOT_FOUND));

        if (user.isTotpEnabled()) {
            throw new IllegalStateException("TOTP is already enabled for this user");
        }

        String secret = totpService.generateSecret();
        user.setTotpSecret(secret);
        userRepository.save(user);

        String qrCodeData = totpService.generateQrCodeData(secret, username);
        return totpService.generateQrCodeImage(qrCodeData);
    }

    public void confirmTotp(String username, String totpCode) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException(USER_NOT_FOUND));

        if (user.getTotpSecret() == null) {
            throw new IllegalStateException("TOTP secret not set. Please enable TOTP first.");
        }

        if (!totpService.verifyCode(user.getTotpSecret(), totpCode)) {
            throw new IllegalArgumentException("Invalid TOTP code");
        }

        user.setTotpEnabled(true);
        userRepository.save(user);
        log.info("TOTP enabled for user: {}", username);
    }

    public void disableTotp(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException(USER_NOT_FOUND));

        user.setTotpEnabled(false);
        user.setTotpSecret(null);
        userRepository.save(user);
        log.info("TOTP disabled for user: {}", username);
    }

    public boolean verifyTotpCode(String username, String totpCode) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException(USER_NOT_FOUND));

        if (!user.isTotpEnabled() || user.getTotpSecret() == null) {
            return false;
        }

        return totpService.verifyCode(user.getTotpSecret(), totpCode);
    }

    @Transactional
    public void updateLastLogin(String username) {
        LocalDateTime now = LocalDateTime.now();
        int updated = userRepository.updateLastLogin(username, now, now);
        if (updated == 0) {
            throw new UserUpdateException("Failed to update last login for user: " + username);
        }
    }

    @Transactional
    public void incrementFailedLoginAttempts(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));

        // Check if user is currently locked and if lockout period has expired
        if (user.isAccountLocked() && user.getLockoutTime() != null) {
            LocalDateTime now = LocalDateTime.now();
            long lockoutDurationMinutes = appProperties.security().lockoutDurationMinutes();

            if (now.isAfter(user.getLockoutTime().plusMinutes(lockoutDurationMinutes))) {
                // Lockout period has expired, unlock the account
                LocalDateTime unlockTime = LocalDateTime.now();
                int updated = userRepository.unlockAccount(username, unlockTime);
                if (updated > 0) {
                    log.info("Account automatically unlocked due to expired lockout period: {}",
                            username);
                }
                return;
            }
        }

        int newAttempts = user.getFailedLoginAttempts() + 1;
        int maxAttempts = appProperties.security().maxFailedAttempts();
        LocalDateTime now = LocalDateTime.now();

        if (newAttempts >= maxAttempts) {
            // Calculate lockout duration (progressive lockout if enabled)
            long lockoutDuration = calculateLockoutDuration(newAttempts);
            LocalDateTime lockoutTime = now.plusMinutes(lockoutDuration);

            userRepository.lockAccount(username, newAttempts, lockoutTime, now);
            log.warn("Account locked after {} failed attempts: {} (lockout duration: {} minutes)",
                    newAttempts, username, lockoutDuration);
        } else {
            userRepository.updateFailedLoginAttempts(username, newAttempts, now);
            log.warn("Failed login attempt #{} for user: {}", newAttempts, username);
        }
    }

    @Transactional
    public void unlockAccount(String username) {
        LocalDateTime now = LocalDateTime.now();
        int updated = userRepository.unlockAccount(username, now);
        if (updated > 0) {
            log.info("Account unlocked: {}", username);
        }
    }

    private long calculateLockoutDuration(int failedAttempts) {
        long baseDuration = appProperties.security().lockoutDurationMinutes();

        if (!appProperties.security().progressiveLockout()) {
            return baseDuration;
        }

        // Progressive lockout: increase duration exponentially
        int maxAttempts = appProperties.security().maxFailedAttempts();
        int excessAttempts = failedAttempts - maxAttempts + 1;

        // Base duration * 2^(excess attempts), max 24 hours
        double multiplier = Math.pow(2.0, excessAttempts - 1.0);
        long progressiveDuration = (long) (baseDuration * multiplier);
        return Math.min(progressiveDuration, 24L * 60L); // Cap at 24 hours
    }

    public Optional<User> findByUsernameOrEmail(String usernameOrEmail) {
        return userRepository.findByUsernameOrEmail(usernameOrEmail);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}
