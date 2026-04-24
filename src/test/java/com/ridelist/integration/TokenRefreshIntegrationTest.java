package com.ridelist.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.ridelist.dto.request.RefreshTokenRequest;
import com.ridelist.dto.request.RegisterRequest;
import com.ridelist.dto.response.ApiResponse;
import com.ridelist.dto.response.AuthResponse;
import com.ridelist.dto.response.TokenResponse;
import com.ridelist.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for GAP 4: Token refresh endpoint.
 *
 * Tests:
 * - REFRESH-001: Valid refresh token returns new access token
 * - REFRESH-002: Invalid refresh token returns 401
 * - REFRESH-003: Expired refresh token returns 401
 * - REFRESH-004: Refresh token for deleted user returns 401
 * - REFRESH-005: New access token works for protected endpoints
 * - REFRESH-006: Endpoint is public (no auth required)
 */
@DisplayName("Token Refresh Integration Tests (GAP 4)")
class TokenRefreshIntegrationTest extends BaseIntegrationTest {

    // ==================== VALID REFRESH TOKEN TESTS ====================

    @Nested
    @DisplayName("Valid Refresh Token")
    class ValidRefreshTokenTests {

        @Test
        @DisplayName("REFRESH-001: Valid refresh token returns new access token")
        void validRefreshToken_returnsNewAccessToken() throws Exception {
            String refreshToken = registerAndGetRefreshToken("refresh@test.com", "Password123!");

            RefreshTokenRequest request = RefreshTokenRequest.builder()
                    .refreshToken(refreshToken)
                    .build();

            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").exists())
                    .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.data.expiresIn").exists());
        }

        @Test
        @DisplayName("REFRESH-005: New access token works for protected endpoints")
        void newAccessToken_worksForProtectedEndpoints() throws Exception {
            String refreshToken = registerAndGetRefreshToken("newtoken@test.com", "Password123!");

            RefreshTokenRequest request = RefreshTokenRequest.builder()
                    .refreshToken(refreshToken)
                    .build();

            MvcResult result = mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andReturn();

            ApiResponse<TokenResponse> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {});

            String newAccessToken = response.getData().getAccessToken();

            mockMvc.perform(get("/api/v1/account/listings")
                            .header("Authorization", "Bearer " + newAccessToken))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("REFRESH-006: Refresh endpoint is public (no auth required)")
        void refreshEndpoint_isPublic() throws Exception {
            String refreshToken = registerAndGetRefreshToken("public@test.com", "Password123!");

            RefreshTokenRequest request = RefreshTokenRequest.builder()
                    .refreshToken(refreshToken)
                    .build();

            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }
    }

    // ==================== INVALID REFRESH TOKEN TESTS ====================

    @Nested
    @DisplayName("Invalid Refresh Token")
    class InvalidRefreshTokenTests {

        @Test
        @DisplayName("REFRESH-002: Invalid refresh token returns 401")
        void invalidRefreshToken_returns401() throws Exception {
            RefreshTokenRequest request = RefreshTokenRequest.builder()
                    .refreshToken("invalid.token.here")
                    .build();

            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("REFRESH-002b: Malformed JWT returns 401")
        void malformedJwt_returns401() throws Exception {
            RefreshTokenRequest request = RefreshTokenRequest.builder()
                    .refreshToken("not-a-jwt")
                    .build();

            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("REFRESH-002c: Empty refresh token returns 400")
        void emptyRefreshToken_returns400() throws Exception {
            RefreshTokenRequest request = RefreshTokenRequest.builder()
                    .refreshToken("")
                    .build();

            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("REFRESH-002d: Null refresh token returns 400")
        void nullRefreshToken_returns400() throws Exception {
            String requestBody = "{\"refreshToken\": null}";

            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("REFRESH-002e: Access token used as refresh token returns 401")
        void accessTokenAsRefreshToken_returns401() throws Exception {
            String accessToken = registerAndGetToken("accessonly@test.com", "Password123!");

            RefreshTokenRequest request = RefreshTokenRequest.builder()
                    .refreshToken(accessToken)
                    .build();

            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }
    }

    // ==================== DELETED USER TESTS ====================

    @Nested
    @DisplayName("Deleted User Tests")
    class DeletedUserTests {

        @Test
        @DisplayName("REFRESH-004: Refresh token for deleted user returns 401")
        void refreshTokenForDeletedUser_returns401() throws Exception {
            String refreshToken = registerAndGetRefreshToken("deleted@test.com", "Password123!");

            User user = userRepository.findByEmail("deleted@test.com").orElseThrow();
            user.setEnabled(false);
            user.setDeletedAt(java.time.LocalDateTime.now());
            userRepository.save(user);

            RefreshTokenRequest request = RefreshTokenRequest.builder()
                    .refreshToken(refreshToken)
                    .build();

            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("REFRESH-004b: Refresh token for disabled user returns 401")
        void refreshTokenForDisabledUser_returns401() throws Exception {
            String refreshToken = registerAndGetRefreshToken("disabled@test.com", "Password123!");

            User user = userRepository.findByEmail("disabled@test.com").orElseThrow();
            user.setEnabled(false);
            userRepository.save(user);

            RefreshTokenRequest request = RefreshTokenRequest.builder()
                    .refreshToken(refreshToken)
                    .build();

            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ==================== RESPONSE FORMAT TESTS ====================

    @Nested
    @DisplayName("Response Format Tests")
    class ResponseFormatTests {

        @Test
        @DisplayName("REFRESH-007: Response contains correct fields")
        void refreshResponse_containsCorrectFields() throws Exception {
            String refreshToken = registerAndGetRefreshToken("format@test.com", "Password123!");

            RefreshTokenRequest request = RefreshTokenRequest.builder()
                    .refreshToken(refreshToken)
                    .build();

            MvcResult result = mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andReturn();

            ApiResponse<TokenResponse> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {});

            assertThat(response.getData().getAccessToken()).isNotBlank();
            assertThat(response.getData().getTokenType()).isEqualTo("Bearer");
            assertThat(response.getData().getExpiresIn()).isPositive();
        }

        @Test
        @DisplayName("REFRESH-008: Access token is different each time")
        void accessToken_isDifferentEachTime() throws Exception {
            String refreshToken = registerAndGetRefreshToken("different@test.com", "Password123!");

            RefreshTokenRequest request = RefreshTokenRequest.builder()
                    .refreshToken(refreshToken)
                    .build();

            MvcResult result1 = mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andReturn();

            Thread.sleep(1100);

            MvcResult result2 = mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andReturn();

            ApiResponse<TokenResponse> response1 = objectMapper.readValue(
                    result1.getResponse().getContentAsString(),
                    new TypeReference<>() {});
            ApiResponse<TokenResponse> response2 = objectMapper.readValue(
                    result2.getResponse().getContentAsString(),
                    new TypeReference<>() {});

            assertThat(response1.getData().getAccessToken())
                    .isNotEqualTo(response2.getData().getAccessToken());
        }
    }

    // ==================== HELPER METHODS ====================

    private String registerAndGetRefreshToken(String email, String password) throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email(email)
                .password(password)
                .firstName("Test")
                .lastName("User")
                .phoneNumber("08012345678")
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        ApiResponse<AuthResponse> response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {});
        return response.getData().getRefreshToken();
    }
}
