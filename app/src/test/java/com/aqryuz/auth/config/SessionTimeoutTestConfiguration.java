package com.aqryuz.auth.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
@TestPropertySource(properties = {"app.jwt.sliding-window-minutes=10",
        "app.jwt.enable-sliding-window=true", "app.jwt.max-session-duration-minutes=120"})
public class SessionTimeoutTestConfiguration {

    @Bean
    @ServiceConnection
    @SuppressWarnings("resource")
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
                .withDatabaseName("auth_test_db").withUsername("test_user")
                .withPassword("test_password").withReuse(true);
    }
}
