package com.aqryuz.auth.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import com.aqryuz.auth.config.TestcontainersConfiguration;
import com.aqryuz.auth.dto.LoginRequest;
import com.aqryuz.auth.dto.LoginResponse;
import com.aqryuz.auth.dto.UserCreateRequest;
import com.aqryuz.auth.entity.User;
import com.aqryuz.auth.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class AuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        // Create a test user with unique email/username
        User testUser = User.builder().username("integrationtestuser")
                .password(passwordEncoder.encode("password123"))
                .email("integrationtest@example.com").firstName("Test").lastName("User")
                .accountEnabled(true).accountLocked(false).totpEnabled(false)
                .roles(Set.of(User.Role.USER)).build();

        userRepository.save(testUser);
    }

    @Test
    void shouldAuthenticateValidUser() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder().usernameOrEmail("integrationtestuser")
                .password("password123").build();

        MvcResult result = mockMvc
                .perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.username").value("integrationtestuser"))
                .andExpect(jsonPath("$.requireTotp").value(false)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        LoginResponse loginResponse = objectMapper.readValue(responseBody, LoginResponse.class);

        assertThat(loginResponse.getAccessToken()).isNotBlank();
        assertThat(loginResponse.getRefreshToken()).isNotBlank();
        assertThat(loginResponse.getUser().getUsername()).isEqualTo("integrationtestuser");
    }

    @Test
    void shouldRejectInvalidCredentials() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder().usernameOrEmail("integrationtestuser")
                .password("wrongpassword").build();

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectNonExistentUser() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder().usernameOrEmail("nonexistent")
                .password("password123").build();

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldCreateUserAsAdmin() throws Exception {
        // First, create an admin user with unique credentials
        User adminUser = User.builder().username("integrationadmin")
                .password(passwordEncoder.encode("admin123")).email("integrationadmin@example.com")
                .firstName("Admin").lastName("User").accountEnabled(true).accountLocked(false)
                .totpEnabled(false).roles(Set.of(User.Role.ADMIN, User.Role.USER)).build();

        userRepository.save(adminUser);

        // Login as admin to get token
        LoginRequest adminLogin = LoginRequest.builder().usernameOrEmail("integrationadmin")
                .password("admin123").build();

        MvcResult loginResult = mockMvc
                .perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLogin)))
                .andExpect(status().isOk()).andReturn();

        LoginResponse loginResponse = objectMapper
                .readValue(loginResult.getResponse().getContentAsString(), LoginResponse.class);

        // Create new user as admin
        UserCreateRequest createRequest = UserCreateRequest.builder().username("integrationnewuser")
                .password("newpassword123").email("integrationnewuser@example.com").firstName("New")
                .lastName("User").accountEnabled(true).roles(Set.of(User.Role.USER)).build();

        mockMvc.perform(post("/api/admin/users")
                .header("Authorization", "Bearer " + loginResponse.getAccessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("integrationnewuser"))
                .andExpect(jsonPath("$.email").value("integrationnewuser@example.com"));

        // Verify user was created in database
        assertThat(userRepository.findByUsername("integrationnewuser")).isPresent();
    }

    @Test
    void shouldRejectUserCreationWithoutAdminRole() throws Exception {
        // Login as regular user
        LoginRequest userLogin = LoginRequest.builder().usernameOrEmail("integrationtestuser")
                .password("password123").build();

        MvcResult loginResult = mockMvc
                .perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userLogin)))
                .andExpect(status().isOk()).andReturn();

        LoginResponse loginResponse = objectMapper
                .readValue(loginResult.getResponse().getContentAsString(), LoginResponse.class);

        // Try to create user without admin role
        UserCreateRequest createRequest = UserCreateRequest.builder().username("unauthorized")
                .password("password123").email("unauthorized@example.com").build();

        mockMvc.perform(post("/api/admin/users")
                .header("Authorization", "Bearer " + loginResponse.getAccessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isForbidden());
    }
}
