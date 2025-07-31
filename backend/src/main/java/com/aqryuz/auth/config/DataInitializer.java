package com.aqryuz.auth.config;

import java.util.Set;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import com.aqryuz.auth.entity.User;
import com.aqryuz.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties appProperties;

    @Override
    public void run(String... args) {
        createDefaultAdminUser();
    }

    private void createDefaultAdminUser() {
        String defaultUsername = appProperties.admin().defaultUsername();

        if (!userRepository.existsByUsername(defaultUsername)) {
            User adminUser = User.builder().username(defaultUsername)
                    .password(passwordEncoder.encode(appProperties.admin().defaultPassword()))
                    .email(appProperties.admin().defaultEmail()).firstName("System")
                    .lastName("Administrator").accountEnabled(true).accountLocked(false)
                    .totpEnabled(false).roles(Set.of(User.Role.ADMIN, User.Role.USER)).build();

            userRepository.save(adminUser);
            log.info("Default admin user created: {}", defaultUsername);
            log.warn("Please change the default admin password: {}",
                    appProperties.admin().defaultPassword());
        } else {
            log.info("Default admin user already exists: {}", defaultUsername);
        }
    }
}
