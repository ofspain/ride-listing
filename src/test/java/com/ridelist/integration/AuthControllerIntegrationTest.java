package com.ridelist.integration;

import com.ridelist.dto.request.LoginRequest;
import com.ridelist.dto.request.RegisterRequest;
import com.ridelist.model.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("AuthController Integration Tests")
public class AuthControllerIntegrationTest extends BaseIntegrationTest {

    // ==================== REGISTRATION TESTS (AUTH-001 to AUTH-005) ====================

    @Nested
    @DisplayName("Registration Tests")
    class RegistrationTests {

        @Test
        @DisplayName("AUTH-001: Register with valid data returns JWT tokens")
        void register_validInput_returnsTokens() throws Exception {
            RegisterRequest request = RegisterRequest.builder()
                    .email("newuser@test.com")
                    .password("SecurePass123!")
                    .firstName("New")
                    .lastName("User")
                    .phoneNumber("08012345678")
                    .build();

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").exists())
                    .andExpect(jsonPath("$.data.refreshToken").exists())
                    .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.data.user.email").value("newuser@test.com"));
        }

        @Test
        @DisplayName("AUTH-002: Register with duplicate email returns 409 Conflict")
        void register_duplicateEmail_returns409() throws Exception {
            // Given: user already exists
            createTestUser("existing@test.com", Role.USER);

            RegisterRequest request = RegisterRequest.builder()
                    .email("existing@test.com")
                    .password("SecurePass123!")
                    .firstName("Another")
                    .lastName("User")
                    .phoneNumber("08012345679")
                    .build();

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("AUTH-003: Register with invalid email format returns 400 Bad Request")
        void register_invalidEmailFormat_returns400() throws Exception {
            RegisterRequest request = RegisterRequest.builder()
                    .email("notanemail")
                    .password("SecurePass123!")
                    .firstName("Test")
                    .lastName("User")
                    .phoneNumber("08012345678")
                    .build();

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("AUTH-004: Register with weak password returns 400 Bad Request")
        void register_weakPassword_returns400() throws Exception {
            RegisterRequest request = RegisterRequest.builder()
                    .email("user@test.com")
                    .password("123")
                    .firstName("Test")
                    .lastName("User")
                    .phoneNumber("08012345678")
                    .build();

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("AUTH-005: Register with missing required fields returns 400 Bad Request")
        void register_missingRequiredFields_returns400() throws Exception {
            RegisterRequest request = RegisterRequest.builder()
                    .email("user@test.com")
                    .password("SecurePass123!")
                    // Missing firstName and lastName
                    .build();

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ==================== LOGIN TESTS (AUTH-010 to AUTH-013) ====================

    @Nested
    @DisplayName("Login Tests")
    class LoginTests {

        @Test
        @DisplayName("AUTH-010: Login with valid credentials returns JWT tokens")
        void login_validCredentials_returnsTokens() throws Exception {
            // Given: registered user
            String email = "user@test.com";
            String password = "SecurePass123!";
            registerAndGetToken(email, password);

            LoginRequest request = LoginRequest.builder()
                    .email(email)
                    .password(password)
                    .build();

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").exists())
                    .andExpect(jsonPath("$.data.refreshToken").exists())
                    .andExpect(jsonPath("$.data.user.email").value(email));
        }

        @Test
        @DisplayName("AUTH-011: Login with wrong password returns 401 Unauthorized")
        void login_wrongPassword_returns401() throws Exception {
            String email = "user@test.com";
            registerAndGetToken(email, "CorrectPassword123!");

            LoginRequest request = LoginRequest.builder()
                    .email(email)
                    .password("WrongPassword123!")
                    .build();

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("AUTH-012: Login with non-existent user returns 401 Unauthorized")
        void login_nonExistentUser_returns401() throws Exception {
            LoginRequest request = LoginRequest.builder()
                    .email("nonexistent@test.com")
                    .password("SomePassword123!")
                    .build();

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("AUTH-013: Login with empty password returns 400 Bad Request")
        void login_emptyPassword_returns400() throws Exception {
            LoginRequest request = LoginRequest.builder()
                    .email("user@test.com")
                    .password("")
                    .build();

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ==================== JWT VALIDATION TESTS (AUTH-020 to AUTH-023) ====================

    @Nested
    @DisplayName("JWT Validation Tests")
    class JwtValidationTests {

        @Test
        @DisplayName("AUTH-020: Access protected endpoint with valid token returns 200")
        void protectedEndpoint_validToken_returns200() throws Exception {
            String token = registerAndGetToken("user@test.com", "Password123!");

            mockMvc.perform(get("/api/v1/account/listings")
                            .header("Authorization", authHeader(token)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("AUTH-021: Access protected endpoint without token returns 401")
        void protectedEndpoint_noToken_returns401() throws Exception {
            mockMvc.perform(get("/api/v1/account/listings"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("AUTH-023: Access protected endpoint with malformed token returns 401")
        void protectedEndpoint_malformedToken_returns401() throws Exception {
            mockMvc.perform(get("/api/v1/account/listings")
                            .header("Authorization", "Bearer invalid-token"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("AUTH-023b: Access protected endpoint with invalid Bearer format returns 401")
        void protectedEndpoint_invalidBearerFormat_returns401() throws Exception {
            mockMvc.perform(get("/api/v1/account/listings")
                            .header("Authorization", "InvalidFormat token"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ==================== ACCESS CONTROL TESTS (AUTH-030 to AUTH-033) ====================

    @Nested
    @DisplayName("Access Control Tests")
    class AccessControlTests {

        @Test
        @DisplayName("AUTH-030: USER accessing USER endpoint returns 200")
        void userEndpoint_userRole_returns200() throws Exception {
            String userToken = registerAndGetToken("user@test.com", "Password123!");

            mockMvc.perform(get("/api/v1/account/listings")
                            .header("Authorization", authHeader(userToken)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("AUTH-031: USER accessing ADMIN endpoint returns 403 Forbidden")
        void adminEndpoint_userRole_returns403() throws Exception {
            String userToken = registerAndGetToken("user@test.com", "Password123!");

            mockMvc.perform(post("/api/v1/admin/locations/states")
                            .header("Authorization", authHeader(userToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Test State\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("AUTH-032: ADMIN accessing ADMIN endpoint returns 200")
        void adminEndpoint_adminRole_returns200() throws Exception {
            // Register admin user and get token
            String adminEmail = "admin@test.com";
            String adminPassword = "AdminPass123!";
            registerAndGetToken(adminEmail, adminPassword);

            // Update user role to ADMIN directly in database
            userRepository.findByEmail(adminEmail).ifPresent(user -> {
                user.setRole(Role.ADMIN);
                userRepository.save(user);
            });

            // Login again to get token with ADMIN role
            String adminToken = loginAndGetToken(adminEmail, adminPassword);

            mockMvc.perform(get("/api/v1/admin/locations/states")
                            .header("Authorization", authHeader(adminToken)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("AUTH-033: Anonymous accessing public endpoint returns 200")
        void publicEndpoint_noToken_returns200() throws Exception {
            mockMvc.perform(get("/api/v1/listings"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("AUTH-033b: Anonymous accessing lookup endpoint returns 200")
        void lookupEndpoint_noToken_returns200() throws Exception {
            mockMvc.perform(get("/api/v1/lookup/states"))
                    .andExpect(status().isOk());
        }
    }
}
