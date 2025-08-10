package com.aqryuz.auth.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import com.aqryuz.auth.config.AppProperties;
import com.aqryuz.auth.entity.User;
import com.aqryuz.auth.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@Testcontainers
@Transactional
class SessionTimeoutIntegrationTest {

    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("auth_test_db").withUsername("testuser").withPassword("testpass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("app.jwt.sliding-window-minutes", () -> "10");
        registry.add("app.jwt.enable-sliding-window", () -> "true");
        registry.add("app.jwt.max-session-duration-minutes", () -> "120");
    }

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AppProperties appProperties;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void shouldConfigureSessionTimeoutProperties() {
        AppProperties.JwtProperties jwtProps = appProperties.jwt();

        assertThat(jwtProps.slidingWindowMinutes()).isEqualTo(10);
        assertThat(jwtProps.enableSlidingWindow()).isTrue();
        assertThat(jwtProps.maxSessionDurationMinutes()).isEqualTo(120);
    }

    @Test
    void shouldProvideSessionInfoEndpoint() throws Exception {
        // Create test user
        User testUser = User.builder().username("sessiontestuser").email("session@test.com")
                .password("$2a$10$Yt2A.lKbZiOx8eBbv6CQJO0vDq4gNj4iH1BjFV6F6J6ByqoHfJI1K") // testpass
                .accountLocked(false).accountEnabled(true).build();
        userRepository.save(testUser);

        // Login to get token
        String loginRequest = """
                {
                    "usernameOrEmail": "sessiontestuser",
                    "password": "testpass"
                }
                """;

        MvcResult loginResult = mockMvc
                .perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest))
                .andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").exists())
                .andReturn();

        JsonNode loginResponse =
                objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String token = loginResponse.get("accessToken").asText();

        // Test session info endpoint
        mockMvc.perform(get("/api/auth/session/info").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("sessiontestuser"))
                .andExpect(jsonPath("$.issuedAt").exists())
                .andExpect(jsonPath("$.expiresAt").exists())
                .andExpect(jsonPath("$.firstIssued").exists())
                .andExpect(jsonPath("$.lastActivity").exists())
                .andExpect(jsonPath("$.sessionId").exists())
                .andExpect(jsonPath("$.remainingMinutes").exists())
                .andExpect(jsonPath("$.maxSessionMinutes").value(120))
                .andExpect(jsonPath("$.slidingWindowEnabled").value(true));
    }

    @Test
    void shouldRefreshSessionEndpoint() throws Exception {
        // Create test user
        User testUser = User.builder().username("refreshtestuser").email("refresh@test.com")
                .password("$2a$10$Yt2A.lKbZiOx8eBbv6CQJO0vDq4gNj4iH1BjFV6F6J6ByqoHfJI1K") // testpass
                .accountLocked(false).accountEnabled(true).build();
        userRepository.save(testUser);

        // Login to get token
        String loginRequest = """
                {
                    "usernameOrEmail": "refreshtestuser",
                    "password": "testpass"
                }
                """;

        MvcResult loginResult = mockMvc
                .perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest))
                .andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").exists())
                .andReturn();

        JsonNode loginResponse =
                objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String token = loginResponse.get("accessToken").asText();

        // Test session refresh endpoint
        mockMvc.perform(
                post("/api/auth/session/refresh").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Session refreshed successfully"));
    }

    @Test
    void shouldValidateSessionEndpoint() throws Exception {
        // Create test user
        User testUser = User.builder().username("validatetestuser").email("validate@test.com")
                .password("$2a$10$Yt2A.lKbZiOx8eBbv6CQJO0vDq4gNj4iH1BjFV6F6J6ByqoHfJI1K") // testpass
                .accountLocked(false).accountEnabled(true).build();
        userRepository.save(testUser);

        // Login to get token
        String loginRequest = """
                {
                    "usernameOrEmail": "validatetestuser",
                    "password": "testpass"
                }
                """;

        MvcResult loginResult = mockMvc
                .perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest))
                .andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").exists())
                .andReturn();

        JsonNode loginResponse =
                objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String token = loginResponse.get("accessToken").asText();

        // Test session validation endpoint
        mockMvc.perform(
                post("/api/auth/session/validate").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.username").value("validatetestuser"))
                .andExpect(jsonPath("$.sessionInfo").exists());
    }
}
