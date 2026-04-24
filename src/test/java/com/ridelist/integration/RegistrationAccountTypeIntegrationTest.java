package com.ridelist.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.ridelist.dto.request.LoginRequest;
import com.ridelist.dto.request.RegisterRequest;
import com.ridelist.dto.response.ApiResponse;
import com.ridelist.dto.response.AuthResponse;
import com.ridelist.model.AccountType;
import com.ridelist.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for GAP 1: Registration accountType + auto-upgrade flow.
 *
 * Tests:
 * - REG-AT-001: Register without accountType defaults to INDIVIDUAL
 * - REG-AT-002: Register with accountType=INDIVIDUAL sets INDIVIDUAL
 * - REG-AT-003: Register with accountType=DEALER sets DEALER
 * - REG-AT-004: AuthResponse contains accountType field
 * - REG-AT-005: UserResponse contains accountType field after login
 * - REG-AT-006: Invalid accountType returns 400 Bad Request
 */
@DisplayName("Registration AccountType Integration Tests (GAP 1)")
class RegistrationAccountTypeIntegrationTest extends BaseIntegrationTest {

    // ==================== REGISTRATION WITH ACCOUNT TYPE ====================

    @Nested
    @DisplayName("Registration with AccountType")
    class RegistrationWithAccountType {

        @Test
        @DisplayName("REG-AT-001: Register without accountType defaults to INDIVIDUAL")
        void register_withoutAccountType_defaultsToIndividual() throws Exception {
            RegisterRequest request = RegisterRequest.builder()
                    .email("user1@test.com")
                    .password("SecurePass123!")
                    .firstName("Test")
                    .lastName("User")
                    .phoneNumber("08012345678")
                    .build();

            MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.user.accountType").value("INDIVIDUAL"))
                    .andReturn();

            User savedUser = userRepository.findByEmail("user1@test.com").orElseThrow();
            assertThat(savedUser.getAccountType()).isEqualTo(AccountType.INDIVIDUAL);
        }

        @Test
        @DisplayName("REG-AT-002: Register with accountType=INDIVIDUAL sets INDIVIDUAL")
        void register_withIndividualAccountType_setsIndividual() throws Exception {
            RegisterRequest request = RegisterRequest.builder()
                    .email("individual@test.com")
                    .password("SecurePass123!")
                    .firstName("Individual")
                    .lastName("User")
                    .phoneNumber("08012345678")
                    .accountType(AccountType.INDIVIDUAL)
                    .build();

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.user.accountType").value("INDIVIDUAL"));

            User savedUser = userRepository.findByEmail("individual@test.com").orElseThrow();
            assertThat(savedUser.getAccountType()).isEqualTo(AccountType.INDIVIDUAL);
        }

        @Test
        @DisplayName("REG-AT-003: Register with accountType=DEALER sets DEALER")
        void register_withDealerAccountType_setsDealer() throws Exception {
            RegisterRequest request = RegisterRequest.builder()
                    .email("dealer@test.com")
                    .password("SecurePass123!")
                    .firstName("Dealer")
                    .lastName("User")
                    .phoneNumber("08012345678")
                    .accountType(AccountType.DEALER)
                    .build();

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.user.accountType").value("DEALER"));

            User savedUser = userRepository.findByEmail("dealer@test.com").orElseThrow();
            assertThat(savedUser.getAccountType()).isEqualTo(AccountType.DEALER);
        }
    }

    // ==================== AUTH RESPONSE VERIFICATION ====================

    @Nested
    @DisplayName("AuthResponse Contains AccountType")
    class AuthResponseVerification {

        @Test
        @DisplayName("REG-AT-004: AuthResponse contains accountType field on registration")
        void authResponse_onRegistration_containsAccountType() throws Exception {
            RegisterRequest request = RegisterRequest.builder()
                    .email("response@test.com")
                    .password("SecurePass123!")
                    .firstName("Response")
                    .lastName("User")
                    .accountType(AccountType.DEALER)
                    .build();

            MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andReturn();

            ApiResponse<AuthResponse> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {});

            assertThat(response.getData().getUser()).isNotNull();
            assertThat(response.getData().getUser().getAccountType()).isEqualTo(AccountType.DEALER);
        }

        @Test
        @DisplayName("REG-AT-005: UserResponse contains accountType field after login")
        void userResponse_afterLogin_containsAccountType() throws Exception {
            String email = "login@test.com";
            String password = "SecurePass123!";

            RegisterRequest registerRequest = RegisterRequest.builder()
                    .email(email)
                    .password(password)
                    .firstName("Login")
                    .lastName("User")
                    .accountType(AccountType.DEALER)
                    .build();

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequest)))
                    .andExpect(status().isCreated());

            LoginRequest loginRequest = LoginRequest.builder()
                    .email(email)
                    .password(password)
                    .build();

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.user.accountType").value("DEALER"));
        }
    }

    // ==================== VALIDATION TESTS ====================

    @Nested
    @DisplayName("AccountType Validation")
    class AccountTypeValidation {

        @Test
        @DisplayName("REG-AT-006: Invalid accountType value returns 400 Bad Request")
        void register_withInvalidAccountType_returns400() throws Exception {
            String requestBody = """
                {
                    "email": "invalid@test.com",
                    "password": "SecurePass123!",
                    "firstName": "Test",
                    "lastName": "User",
                    "accountType": "INVALID_TYPE"
                }
                """;

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("REG-AT-007: Null accountType is accepted (defaults to INDIVIDUAL)")
        void register_withNullAccountType_defaultsToIndividual() throws Exception {
            String requestBody = """
                {
                    "email": "nulltype@test.com",
                    "password": "SecurePass123!",
                    "firstName": "Null",
                    "lastName": "Type",
                    "accountType": null
                }
                """;

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.user.accountType").value("INDIVIDUAL"));
        }
    }
}
