package com.ridelist.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.ridelist.dto.request.ChangePasswordRequest;
import com.ridelist.dto.request.LoginRequest;
import com.ridelist.dto.request.UpdateProfileRequest;
import com.ridelist.dto.response.ApiResponse;
import com.ridelist.dto.response.UserResponse;
import com.ridelist.model.State;
import com.ridelist.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for GAP 2: Profile update endpoints.
 *
 * Endpoints tested:
 * - GET /api/v1/account/me
 * - PUT /api/v1/account/me
 * - PUT /api/v1/account/me/password
 * - DELETE /api/v1/account/me
 */
@DisplayName("Profile Endpoints Integration Tests (GAP 2)")
class ProfileEndpointsIntegrationTest extends BaseIntegrationTest {

    // ==================== GET PROFILE TESTS ====================

    @Nested
    @DisplayName("GET /api/v1/account/me")
    class GetProfileTests {

        @Test
        @DisplayName("PROF-001: Get profile returns authenticated user's data")
        void getProfile_authenticated_returnsUserData() throws Exception {
            String token = registerAndGetToken("profile@test.com", "Password123!");

            mockMvc.perform(get("/api/v1/account/me")
                            .header("Authorization", authHeader(token)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.email").value("profile@test.com"))
                    .andExpect(jsonPath("$.data.firstName").value("Test"))
                    .andExpect(jsonPath("$.data.lastName").value("User"))
                    .andExpect(jsonPath("$.data.accountType").exists())
                    .andExpect(jsonPath("$.data.role").exists())
                    .andExpect(jsonPath("$.data.createdAt").exists());
        }

        @Test
        @DisplayName("PROF-002: Get profile without authentication returns 401")
        void getProfile_unauthenticated_returns401() throws Exception {
            mockMvc.perform(get("/api/v1/account/me"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("PROF-003: Get profile returns state when set")
        void getProfile_withState_returnsState() throws Exception {
            String token = registerAndGetToken("stateuser@test.com", "Password123!");
            User user = userRepository.findByEmail("stateuser@test.com").orElseThrow();
            user.setState("Lagos");
            userRepository.save(user);

            mockMvc.perform(get("/api/v1/account/me")
                            .header("Authorization", authHeader(token)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.state").value("Lagos"));
        }
    }

    // ==================== UPDATE PROFILE TESTS ====================

    @Nested
    @DisplayName("PUT /api/v1/account/me")
    class UpdateProfileTests {

        @Test
        @DisplayName("PROF-010: Update profile with firstName only")
        void updateProfile_firstNameOnly_updatesFirstName() throws Exception {
            String token = registerAndGetToken("update@test.com", "Password123!");

            UpdateProfileRequest request = UpdateProfileRequest.builder()
                    .firstName("NewFirstName")
                    .build();

            mockMvc.perform(put("/api/v1/account/me")
                            .header("Authorization", authHeader(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.firstName").value("NewFirstName"))
                    .andExpect(jsonPath("$.data.lastName").value("User"));
        }

        @Test
        @DisplayName("PROF-011: Update profile with lastName only")
        void updateProfile_lastNameOnly_updatesLastName() throws Exception {
            String token = registerAndGetToken("lastname@test.com", "Password123!");

            UpdateProfileRequest request = UpdateProfileRequest.builder()
                    .lastName("NewLastName")
                    .build();

            mockMvc.perform(put("/api/v1/account/me")
                            .header("Authorization", authHeader(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.lastName").value("NewLastName"));
        }

        @Test
        @DisplayName("PROF-012: Update profile with stateId sets state name")
        void updateProfile_withStateId_setsStateName() throws Exception {
            String token = registerAndGetToken("stateupdate@test.com", "Password123!");
            State state = createTestState("Lagos");

            UpdateProfileRequest request = UpdateProfileRequest.builder()
                    .stateId(state.getId())
                    .build();

            mockMvc.perform(put("/api/v1/account/me")
                            .header("Authorization", authHeader(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.state").value("Lagos"));
        }

        @Test
        @DisplayName("PROF-013: Update profile with invalid stateId returns 404")
        void updateProfile_invalidStateId_returns404() throws Exception {
            String token = registerAndGetToken("invalidstate@test.com", "Password123!");

            UpdateProfileRequest request = UpdateProfileRequest.builder()
                    .stateId(java.util.UUID.randomUUID())
                    .build();

            mockMvc.perform(put("/api/v1/account/me")
                            .header("Authorization", authHeader(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("PROF-014: Update profile with all fields")
        void updateProfile_allFields_updatesAll() throws Exception {
            String token = registerAndGetToken("allfields@test.com", "Password123!");
            State state = createTestState("Abuja");

            UpdateProfileRequest request = UpdateProfileRequest.builder()
                    .firstName("John")
                    .lastName("Doe")
                    .stateId(state.getId())
                    .build();

            mockMvc.perform(put("/api/v1/account/me")
                            .header("Authorization", authHeader(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.firstName").value("John"))
                    .andExpect(jsonPath("$.data.lastName").value("Doe"))
                    .andExpect(jsonPath("$.data.state").value("Abuja"));
        }

        @Test
        @DisplayName("PROF-015: Update profile without authentication returns 401")
        void updateProfile_unauthenticated_returns401() throws Exception {
            UpdateProfileRequest request = UpdateProfileRequest.builder()
                    .firstName("Hacker")
                    .build();

            mockMvc.perform(put("/api/v1/account/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("PROF-016: Update profile ignores null fields")
        void updateProfile_nullFields_keepsExisting() throws Exception {
            String token = registerAndGetToken("keepexisting@test.com", "Password123!");

            UpdateProfileRequest request = UpdateProfileRequest.builder().build();

            mockMvc.perform(put("/api/v1/account/me")
                            .header("Authorization", authHeader(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.firstName").value("Test"))
                    .andExpect(jsonPath("$.data.lastName").value("User"));
        }
    }

    // ==================== CHANGE PASSWORD TESTS ====================

    @Nested
    @DisplayName("PUT /api/v1/account/me/password")
    class ChangePasswordTests {

        @Test
        @DisplayName("PROF-020: Change password with valid data succeeds")
        void changePassword_validData_succeeds() throws Exception {
            String token = registerAndGetToken("changepass@test.com", "OldPassword123!");

            ChangePasswordRequest request = ChangePasswordRequest.builder()
                    .currentPassword("OldPassword123!")
                    .newPassword("NewPassword123!")
                    .confirmPassword("NewPassword123!")
                    .build();

            mockMvc.perform(put("/api/v1/account/me/password")
                            .header("Authorization", authHeader(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Password updated successfully"));

            LoginRequest loginRequest = LoginRequest.builder()
                    .email("changepass@test.com")
                    .password("NewPassword123!")
                    .build();

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("PROF-021: Change password with wrong current password returns 400")
        void changePassword_wrongCurrentPassword_returns400() throws Exception {
            String token = registerAndGetToken("wrongcurrent@test.com", "CorrectPassword123!");

            ChangePasswordRequest request = ChangePasswordRequest.builder()
                    .currentPassword("WrongPassword123!")
                    .newPassword("NewPassword123!")
                    .confirmPassword("NewPassword123!")
                    .build();

            mockMvc.perform(put("/api/v1/account/me/password")
                            .header("Authorization", authHeader(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("PROF-022: Change password with mismatched confirm returns 400")
        void changePassword_mismatchedConfirm_returns400() throws Exception {
            String token = registerAndGetToken("mismatch@test.com", "CurrentPassword123!");

            ChangePasswordRequest request = ChangePasswordRequest.builder()
                    .currentPassword("CurrentPassword123!")
                    .newPassword("NewPassword123!")
                    .confirmPassword("DifferentPassword123!")
                    .build();

            mockMvc.perform(put("/api/v1/account/me/password")
                            .header("Authorization", authHeader(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("PROF-023: Change password with weak new password returns 400")
        void changePassword_weakNewPassword_returns400() throws Exception {
            String token = registerAndGetToken("weaknew@test.com", "CurrentPassword123!");

            ChangePasswordRequest request = ChangePasswordRequest.builder()
                    .currentPassword("CurrentPassword123!")
                    .newPassword("weak")
                    .confirmPassword("weak")
                    .build();

            mockMvc.perform(put("/api/v1/account/me/password")
                            .header("Authorization", authHeader(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("PROF-024: Change password without authentication returns 401")
        void changePassword_unauthenticated_returns401() throws Exception {
            ChangePasswordRequest request = ChangePasswordRequest.builder()
                    .currentPassword("CurrentPassword123!")
                    .newPassword("NewPassword123!")
                    .confirmPassword("NewPassword123!")
                    .build();

            mockMvc.perform(put("/api/v1/account/me/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("PROF-025: Old password no longer works after change")
        void changePassword_oldPasswordNoLongerWorks() throws Exception {
            String email = "oldpassfail@test.com";
            String oldPassword = "OldPassword123!";
            String newPassword = "NewPassword123!";
            String token = registerAndGetToken(email, oldPassword);

            ChangePasswordRequest request = ChangePasswordRequest.builder()
                    .currentPassword(oldPassword)
                    .newPassword(newPassword)
                    .confirmPassword(newPassword)
                    .build();

            mockMvc.perform(put("/api/v1/account/me/password")
                            .header("Authorization", authHeader(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            LoginRequest loginRequest = LoginRequest.builder()
                    .email(email)
                    .password(oldPassword)
                    .build();

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ==================== DELETE ACCOUNT TESTS ====================

    @Nested
    @DisplayName("DELETE /api/v1/account/me")
    class DeleteAccountTests {

        @Test
        @DisplayName("PROF-030: Delete account returns success")
        void deleteAccount_authenticated_returnsSuccess() throws Exception {
            String token = registerAndGetToken("deleteuser@test.com", "Password123!");

            mockMvc.perform(delete("/api/v1/account/me")
                            .header("Authorization", authHeader(token)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Account deleted"));
        }

        @Test
        @DisplayName("PROF-031: Delete account without authentication returns 401")
        void deleteAccount_unauthenticated_returns401() throws Exception {
            mockMvc.perform(delete("/api/v1/account/me"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("PROF-032: Deleted user cannot login")
        void deleteAccount_userCannotLogin() throws Exception {
            String email = "cantlogin@test.com";
            String password = "Password123!";
            String token = registerAndGetToken(email, password);

            mockMvc.perform(delete("/api/v1/account/me")
                            .header("Authorization", authHeader(token)))
                    .andExpect(status().isOk());

            LoginRequest loginRequest = LoginRequest.builder()
                    .email(email)
                    .password(password)
                    .build();

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isUnauthorized());
        }
    }
}
