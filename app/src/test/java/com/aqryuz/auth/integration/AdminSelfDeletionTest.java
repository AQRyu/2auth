package com.aqryuz.auth.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.util.Set;
import java.util.UUID;
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
import com.aqryuz.auth.entity.User;
import com.aqryuz.auth.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class AdminSelfDeletionTest {

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
    }

    @Test
    void shouldPreventAdminSelfDeletion() throws Exception {
        // Generate unique username to avoid conflicts
        String uniqueUsername = "admin-self-delete-" + UUID.randomUUID().toString().substring(0, 8);

        // Create an admin user
        User adminUser = User.builder().username(uniqueUsername)
                .password(passwordEncoder.encode("admin123")).email(uniqueUsername + "@example.com")
                .firstName("Test").lastName("Admin").accountEnabled(true).accountLocked(false)
                .totpEnabled(false).roles(Set.of(User.Role.ADMIN, User.Role.USER)).build();

        User savedAdmin = userRepository.save(adminUser);

        // Login as admin to get token
        LoginRequest adminLogin =
                LoginRequest.builder().usernameOrEmail(uniqueUsername).password("admin123").build();

        MvcResult loginResult = mockMvc
                .perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLogin)))
                .andExpect(status().isOk()).andReturn();

        LoginResponse loginResponse = objectMapper
                .readValue(loginResult.getResponse().getContentAsString(), LoginResponse.class);

        // Attempt to delete own account - should return 400 Bad Request
        mockMvc.perform(delete("/api/admin/users/" + savedAdmin.getId()).header("Authorization",
                "Bearer " + loginResponse.getAccessToken())).andExpect(status().isBadRequest());
    }

    @Test
    void shouldAllowAdminToDeleteOtherUsers() throws Exception {
        // Generate unique usernames to avoid conflicts
        String adminUsername = "admin-delete-other-" + UUID.randomUUID().toString().substring(0, 8);
        String userUsername = "user-to-delete-" + UUID.randomUUID().toString().substring(0, 8);

        // Create an admin user
        User adminUser = User.builder().username(adminUsername)
                .password(passwordEncoder.encode("admin123")).email(adminUsername + "@example.com")
                .firstName("Test").lastName("Admin").accountEnabled(true).accountLocked(false)
                .totpEnabled(false).roles(Set.of(User.Role.ADMIN, User.Role.USER)).build();

        userRepository.save(adminUser);

        // Create a regular user
        User regularUser = User.builder().username(userUsername)
                .password(passwordEncoder.encode("user123")).email(userUsername + "@example.com")
                .firstName("Test").lastName("User").accountEnabled(true).accountLocked(false)
                .totpEnabled(false).roles(Set.of(User.Role.USER)).build();

        User savedUser = userRepository.save(regularUser);

        // Login as admin to get token
        LoginRequest adminLogin =
                LoginRequest.builder().usernameOrEmail(adminUsername).password("admin123").build();

        MvcResult loginResult = mockMvc
                .perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLogin)))
                .andExpect(status().isOk()).andReturn();

        LoginResponse loginResponse = objectMapper
                .readValue(loginResult.getResponse().getContentAsString(), LoginResponse.class);

        // Delete another user - should work (204 No Content)
        mockMvc.perform(delete("/api/admin/users/" + savedUser.getId()).header("Authorization",
                "Bearer " + loginResponse.getAccessToken())).andExpect(status().isNoContent());
    }
}
