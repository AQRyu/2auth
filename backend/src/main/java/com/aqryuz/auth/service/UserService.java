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

        int newAttempts = user.getFailedLoginAttempts() + 1;
        LocalDateTime now = LocalDateTime.now();

        if (newAttempts >= 3) {
            userRepository.lockAccount(username, newAttempts, now);
        } else {
            userRepository.updateFailedLoginAttempts(username, newAttempts, now);
        }
    }

    public Optional<User> findByUsernameOrEmail(String usernameOrEmail) {
        return userRepository.findByUsernameOrEmail(usernameOrEmail);
    }
}
