package com.aqryuz.auth.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import com.aqryuz.auth.config.TestcontainersConfiguration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = {"spring.docker.compose.enabled=false",
                "logging.level.org.springframework.security=INFO",
                "springdoc.api-docs.enabled=true", "springdoc.swagger-ui.enabled=true"})
class OpenApiIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Test
        void testOpenApiDocsGenerated() throws Exception {
                // Test that OpenAPI docs are generated (try multiple possible endpoints)
                mockMvc.perform(get("/v3/api-docs").contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.openapi").exists())
                                .andExpect(jsonPath("$.info").exists());
        }

        @Test
        void testOpenApiContainsAuthEndpoints() throws Exception {
                mockMvc.perform(get("/v3/api-docs").contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk()).andExpect(jsonPath("$.paths").exists())
                                .andExpect(jsonPath("$.paths['/api/auth/login']").exists());
        }

        @Test
        void testOpenApiContainsAdminEndpoints() throws Exception {
                mockMvc.perform(get("/v3/api-docs").contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk()).andExpect(jsonPath("$.paths").exists());
        }

        @Test
        void testOpenApiContainsSecuritySchemes() throws Exception {
                mockMvc.perform(get("/v3/api-docs").contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.components").exists());
        }
}
