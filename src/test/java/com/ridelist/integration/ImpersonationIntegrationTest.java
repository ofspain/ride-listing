package com.ridelist.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.ridelist.dto.response.ApiResponse;
import com.ridelist.dto.response.ImpersonationResponse;
import com.ridelist.dto.response.ListingSummaryResponse;
import com.ridelist.dto.response.PagedResponse;
import com.ridelist.model.*;
import com.ridelist.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ImpersonationIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String adminToken;
    private String userToken;
    private User adminUser;
    private User regularUser;
    private User anotherAdmin;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = registerAndGetToken("admin@test.com", "password123");
        adminUser = userRepository.findByEmail("admin@test.com").orElseThrow();
        adminUser.setRole(Role.ADMIN);
        userRepository.save(adminUser);
        adminToken = loginAndGetToken("admin@test.com", "password123");

        userToken = registerAndGetToken("user@test.com", "password123");
        regularUser = userRepository.findByEmail("user@test.com").orElseThrow();

        anotherAdmin = createTestUser("admin2@test.com", Role.ADMIN);
    }

    @Nested
    @DisplayName("POST /api/v1/admin/users/{userId}/impersonate")
    class ImpersonateUserTests {

        @Test
        @DisplayName("Admin can impersonate regular user")
        void adminCanImpersonateRegularUser() throws Exception {
            MvcResult result = mockMvc.perform(post("/api/v1/admin/users/{userId}/impersonate", regularUser.getId())
                            .header("Authorization", authHeader(adminToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Impersonation session started"))
                    .andReturn();

            ApiResponse<ImpersonationResponse> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {});

            assertThat(response.getData().getAccessToken()).isNotBlank();
            assertThat(response.getData().getTokenType()).isEqualTo("Bearer");
            assertThat(response.getData().getExpiresIn()).isEqualTo(1800);
            assertThat(response.getData().getTargetUser().getId()).isEqualTo(regularUser.getId());
            assertThat(response.getData().getTargetUser().getEmail()).isEqualTo(regularUser.getEmail());
        }

        @Test
        @DisplayName("Admin cannot impersonate another admin")
        void adminCannotImpersonateAnotherAdmin() throws Exception {
            mockMvc.perform(post("/api/v1/admin/users/{userId}/impersonate", anotherAdmin.getId())
                            .header("Authorization", authHeader(adminToken)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Cannot impersonate an admin account"));
        }

        @Test
        @DisplayName("Admin cannot impersonate themselves")
        void adminCannotImpersonateThemselves() throws Exception {
            mockMvc.perform(post("/api/v1/admin/users/{userId}/impersonate", adminUser.getId())
                            .header("Authorization", authHeader(adminToken)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Cannot impersonate your own account"));
        }

        @Test
        @DisplayName("Non-admin cannot call impersonate endpoint")
        void nonAdminCannotImpersonate() throws Exception {
            mockMvc.perform(post("/api/v1/admin/users/{userId}/impersonate", regularUser.getId())
                            .header("Authorization", authHeader(userToken)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Unauthenticated user cannot call impersonate endpoint")
        void unauthenticatedCannotImpersonate() throws Exception {
            mockMvc.perform(post("/api/v1/admin/users/{userId}/impersonate", regularUser.getId()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Impersonating non-existent user returns 404")
        void impersonatingNonExistentUserReturns404() throws Exception {
            mockMvc.perform(post("/api/v1/admin/users/{userId}/impersonate", UUID.randomUUID())
                            .header("Authorization", authHeader(adminToken)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Impersonation Token Validity")
    class ImpersonationTokenTests {

        @Test
        @DisplayName("Impersonation token is valid for user's own endpoints")
        void impersonationTokenValidForUserEndpoints() throws Exception {
            MvcResult impersonateResult = mockMvc.perform(
                    post("/api/v1/admin/users/{userId}/impersonate", regularUser.getId())
                            .header("Authorization", authHeader(adminToken)))
                    .andExpect(status().isOk())
                    .andReturn();

            ApiResponse<ImpersonationResponse> response = objectMapper.readValue(
                    impersonateResult.getResponse().getContentAsString(),
                    new TypeReference<>() {});

            String impersonationToken = response.getData().getAccessToken();

            mockMvc.perform(get("/api/v1/account/me")
                            .header("Authorization", authHeader(impersonationToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(regularUser.getId().toString()))
                    .andExpect(jsonPath("$.data.email").value(regularUser.getEmail()));
        }

        @Test
        @DisplayName("Impersonation token can access user's listings")
        void impersonationTokenCanAccessUserListings() throws Exception {
            Listing listing = createTestListing(regularUser, ListingType.VEHICLE);

            MvcResult impersonateResult = mockMvc.perform(
                    post("/api/v1/admin/users/{userId}/impersonate", regularUser.getId())
                            .header("Authorization", authHeader(adminToken)))
                    .andExpect(status().isOk())
                    .andReturn();

            ApiResponse<ImpersonationResponse> response = objectMapper.readValue(
                    impersonateResult.getResponse().getContentAsString(),
                    new TypeReference<>() {});

            String impersonationToken = response.getData().getAccessToken();

            MvcResult listingsResult = mockMvc.perform(get("/api/v1/account/listings")
                            .header("Authorization", authHeader(impersonationToken)))
                    .andExpect(status().isOk())
                    .andReturn();

            ApiResponse<PagedResponse<ListingSummaryResponse>> listingsResponse = objectMapper.readValue(
                    listingsResult.getResponse().getContentAsString(),
                    new TypeReference<>() {});

            assertThat(listingsResponse.getData().getContent()).hasSize(1);
            assertThat(listingsResponse.getData().getContent().get(0).getId()).isEqualTo(listing.getId());
        }

        @Test
        @DisplayName("Impersonation token has isImpersonation claim")
        void impersonationTokenHasIsImpersonationClaim() throws Exception {
            MvcResult result = mockMvc.perform(
                    post("/api/v1/admin/users/{userId}/impersonate", regularUser.getId())
                            .header("Authorization", authHeader(adminToken)))
                    .andExpect(status().isOk())
                    .andReturn();

            ApiResponse<ImpersonationResponse> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {});

            String token = response.getData().getAccessToken();

            assertThat(jwtTokenProvider.isImpersonationToken(token)).isTrue();
            assertThat(jwtTokenProvider.getImpersonatedBy(token)).isEqualTo(adminUser.getId().toString());
        }

        @Test
        @DisplayName("Regular token is not an impersonation token")
        void regularTokenIsNotImpersonationToken() {
            assertThat(jwtTokenProvider.isImpersonationToken(adminToken)).isFalse();
            assertThat(jwtTokenProvider.getImpersonatedBy(adminToken)).isNull();
        }

        @Test
        @DisplayName("Impersonation token cannot access admin endpoints")
        void impersonationTokenCannotAccessAdminEndpoints() throws Exception {
            MvcResult impersonateResult = mockMvc.perform(
                    post("/api/v1/admin/users/{userId}/impersonate", regularUser.getId())
                            .header("Authorization", authHeader(adminToken)))
                    .andExpect(status().isOk())
                    .andReturn();

            ApiResponse<ImpersonationResponse> response = objectMapper.readValue(
                    impersonateResult.getResponse().getContentAsString(),
                    new TypeReference<>() {});

            String impersonationToken = response.getData().getAccessToken();

            mockMvc.perform(get("/api/v1/admin/listings")
                            .header("Authorization", authHeader(impersonationToken)))
                    .andExpect(status().isForbidden());
        }
    }
}
