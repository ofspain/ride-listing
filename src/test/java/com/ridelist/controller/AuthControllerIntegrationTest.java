package com.ridelist.controller;

import com.ridelist.BaseIntegrationTest;
import com.ridelist.dto.request.LoginRequest;
import com.ridelist.dto.request.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("AuthController Integration Tests")
class AuthControllerIntegrationTest extends BaseIntegrationTest {

    @Nested
    @DisplayName("POST /api/v1/auth/register")
    class RegisterTests {

        @Test
        @DisplayName("Should register new user successfully")
        void shouldRegisterNewUser() throws Exception {
            RegisterRequest request = RegisterRequest.builder()
                    .email("newuser@test.com")
                    .password("password123")
                    .firstName("John")
                    .lastName("Doe")
                    .phoneNumber("08012345678")
                    .build();

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Registration successful"))
                    .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                    .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.data.user.email").value("newuser@test.com"))
                    .andExpect(jsonPath("$.data.user.firstName").value("John"))
                    .andExpect(jsonPath("$.data.user.lastName").value("Doe"));
        }

        @Test
        @DisplayName("Should fail when email already exists")
        void shouldFailWhenEmailExists() throws Exception {
            // First registration
            RegisterRequest request = RegisterRequest.builder()
                    .email("existing@test.com")
                    .password("password123")
                    .firstName("John")
                    .lastName("Doe")
                    .build();

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            // Second registration with same email
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Email is already registered"));
        }

        @Test
        @DisplayName("Should fail with invalid email format")
        void shouldFailWithInvalidEmail() throws Exception {
            RegisterRequest request = RegisterRequest.builder()
                    .email("invalid-email")
                    .password("password123")
                    .firstName("John")
                    .lastName("Doe")
                    .build();

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Should fail with password too short")
        void shouldFailWithShortPassword() throws Exception {
            RegisterRequest request = RegisterRequest.builder()
                    .email("test@test.com")
                    .password("short")
                    .firstName("John")
                    .lastName("Doe")
                    .build();

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Should fail with missing required fields")
        void shouldFailWithMissingFields() throws Exception {
            RegisterRequest request = RegisterRequest.builder()
                    .email("test@test.com")
                    .build();

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class LoginTests {

        @Test
        @DisplayName("Should login successfully with valid credentials")
        void shouldLoginSuccessfully() throws Exception {
            // Register first
            RegisterRequest registerRequest = RegisterRequest.builder()
                    .email("login@test.com")
                    .password("password123")
                    .firstName("John")
                    .lastName("Doe")
                    .build();

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequest)))
                    .andExpect(status().isCreated());

            // Login
            LoginRequest loginRequest = LoginRequest.builder()
                    .email("login@test.com")
                    .password("password123")
                    .build();

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Login successful"))
                    .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                    .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.data.user.email").value("login@test.com"));
        }

        @Test
        @DisplayName("Should fail with invalid email")
        void shouldFailWithInvalidEmail() throws Exception {
            LoginRequest request = LoginRequest.builder()
                    .email("nonexistent@test.com")
                    .password("password123")
                    .build();

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Invalid email or password"));
        }

        @Test
        @DisplayName("Should fail with wrong password")
        void shouldFailWithWrongPassword() throws Exception {
            // Register first
            RegisterRequest registerRequest = RegisterRequest.builder()
                    .email("wrongpass@test.com")
                    .password("password123")
                    .firstName("John")
                    .lastName("Doe")
                    .build();

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequest)))
                    .andExpect(status().isCreated());

            // Login with wrong password
            LoginRequest loginRequest = LoginRequest.builder()
                    .email("wrongpass@test.com")
                    .password("wrongpassword")
                    .build();

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Invalid email or password"));
        }

        @Test
        @DisplayName("Should fail with disabled account")
        void shouldFailWithDisabledAccount() throws Exception {
            // Create disabled user directly in database
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            var user = createTestUser("disabled@test.com", encoder.encode("password123"));
            user.setEnabled(false);
            userRepository.save(user);

            LoginRequest loginRequest = LoginRequest.builder()
                    .email("disabled@test.com")
                    .password("password123")
                    .build();

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Account is disabled"));
        }
    }
}
