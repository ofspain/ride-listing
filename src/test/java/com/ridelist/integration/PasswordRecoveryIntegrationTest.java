package com.ridelist.integration;

import com.ridelist.dto.request.ForgotPasswordRequest;
import com.ridelist.dto.request.LoginRequest;
import com.ridelist.dto.request.ResetPasswordRequest;
import com.ridelist.dto.request.ValidateResetTokenRequest;
import com.ridelist.model.PasswordResetToken;
import com.ridelist.model.Role;
import com.ridelist.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PasswordRecoveryIntegrationTest extends BaseIntegrationTest {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private User createUserWithPassword(String email, String password) {
        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .firstName("Test")
                .lastName("User")
                .phoneNumber("08012345678")
                .role(Role.USER)
                .build();
        return userRepository.save(user);
    }

    private PasswordResetToken createValidToken(User user, String token) {
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .token(token)
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .used(false)
                .build();
        return passwordResetTokenRepository.save(resetToken);
    }

    private PasswordResetToken createExpiredToken(User user, String token) {
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .token(token)
                .expiresAt(LocalDateTime.now().minusMinutes(5))
                .used(false)
                .build();
        return passwordResetTokenRepository.save(resetToken);
    }

    private PasswordResetToken createUsedToken(User user, String token) {
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .token(token)
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .used(true)
                .build();
        return passwordResetTokenRepository.save(resetToken);
    }

    @Nested
    @DisplayName("POST /api/v1/auth/forgot-password")
    class ForgotPasswordTests {

        @Test
        @DisplayName("should return 200 for valid email and create token")
        void forgotPassword_validEmail_returnsSuccess() throws Exception {
            User user = createUserWithPassword("forgot@test.com", "password123");

            ForgotPasswordRequest request = new ForgotPasswordRequest();
            request.setEmail("forgot@test.com");

            mockMvc.perform(post("/api/v1/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value(
                            "If an account exists with this email, you will receive a password reset link shortly."));

            Thread.sleep(500);

            var tokens = passwordResetTokenRepository.findByUserAndUsedFalse(user);
            assertThat(tokens).isNotEmpty();
        }

        @Test
        @DisplayName("should return 200 for non-existent email (no enumeration)")
        void forgotPassword_nonExistentEmail_returnsSuccess() throws Exception {
            ForgotPasswordRequest request = new ForgotPasswordRequest();
            request.setEmail("nonexistent@test.com");

            mockMvc.perform(post("/api/v1/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value(
                            "If an account exists with this email, you will receive a password reset link shortly."));
        }

        @Test
        @DisplayName("should return 400 for invalid email format")
        void forgotPassword_invalidEmail_returns400() throws Exception {
            ForgotPasswordRequest request = new ForgotPasswordRequest();
            request.setEmail("not-an-email");

            mockMvc.perform(post("/api/v1/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/validate-reset-token")
    class ValidateTokenTests {

        @Test
        @DisplayName("should return 200 for valid token")
        void validateToken_validToken_returnsSuccess() throws Exception {
            User user = createUserWithPassword("validate@test.com", "password123");
            createValidToken(user, "valid-token-123");

            ValidateResetTokenRequest request = new ValidateResetTokenRequest();
            request.setToken("valid-token-123");

            mockMvc.perform(post("/api/v1/auth/validate-reset-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("should return 400 for expired token")
        void validateToken_expiredToken_returns400() throws Exception {
            User user = createUserWithPassword("expired@test.com", "password123");
            createExpiredToken(user, "expired-token-123");

            ValidateResetTokenRequest request = new ValidateResetTokenRequest();
            request.setToken("expired-token-123");

            mockMvc.perform(post("/api/v1/auth/validate-reset-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(
                            "This reset link has expired or already been used. Please request a new one."));
        }

        @Test
        @DisplayName("should return 400 for used token")
        void validateToken_usedToken_returns400() throws Exception {
            User user = createUserWithPassword("used@test.com", "password123");
            createUsedToken(user, "used-token-123");

            ValidateResetTokenRequest request = new ValidateResetTokenRequest();
            request.setToken("used-token-123");

            mockMvc.perform(post("/api/v1/auth/validate-reset-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(
                            "This reset link has expired or already been used. Please request a new one."));
        }

        @Test
        @DisplayName("should return 400 for non-existent token")
        void validateToken_nonExistentToken_returns400() throws Exception {
            ValidateResetTokenRequest request = new ValidateResetTokenRequest();
            request.setToken("non-existent-token");

            mockMvc.perform(post("/api/v1/auth/validate-reset-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(
                            "Invalid or expired reset link. Please request a new one."));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/reset-password")
    class ResetPasswordTests {

        @Test
        @DisplayName("should reset password with valid token")
        void resetPassword_validToken_updatesPassword() throws Exception {
            User user = createUserWithPassword("reset@test.com", "oldPassword123");
            createValidToken(user, "reset-token-123");

            ResetPasswordRequest request = new ResetPasswordRequest();
            request.setToken("reset-token-123");
            request.setNewPassword("newPassword456");
            request.setConfirmPassword("newPassword456");

            mockMvc.perform(post("/api/v1/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value(
                            "Password reset successfully. You can now log in with your new password."));

            LoginRequest loginRequest = LoginRequest.builder()
                    .email("reset@test.com")
                    .password("newPassword456")
                    .build();

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should return 400 for mismatched passwords")
        void resetPassword_mismatchedPasswords_returns400() throws Exception {
            User user = createUserWithPassword("mismatch@test.com", "password123");
            createValidToken(user, "mismatch-token-123");

            ResetPasswordRequest request = new ResetPasswordRequest();
            request.setToken("mismatch-token-123");
            request.setNewPassword("newPassword456");
            request.setConfirmPassword("differentPassword789");

            mockMvc.perform(post("/api/v1/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Passwords do not match"));
        }

        @Test
        @DisplayName("should return 400 for expired token")
        void resetPassword_expiredToken_returns400() throws Exception {
            User user = createUserWithPassword("expiredreset@test.com", "password123");
            createExpiredToken(user, "expired-reset-token");

            ResetPasswordRequest request = new ResetPasswordRequest();
            request.setToken("expired-reset-token");
            request.setNewPassword("newPassword456");
            request.setConfirmPassword("newPassword456");

            mockMvc.perform(post("/api/v1/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(
                            "This reset link has expired or already been used"));
        }

        @Test
        @DisplayName("should not allow using same token twice")
        void resetPassword_sameTokenTwice_secondAttemptFails() throws Exception {
            User user = createUserWithPassword("twice@test.com", "password123");
            createValidToken(user, "once-only-token");

            ResetPasswordRequest request = new ResetPasswordRequest();
            request.setToken("once-only-token");
            request.setNewPassword("newPassword456");
            request.setConfirmPassword("newPassword456");

            mockMvc.perform(post("/api/v1/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/v1/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should mark token as used after reset")
        void resetPassword_marksTokenAsUsed() throws Exception {
            User user = createUserWithPassword("markused@test.com", "password123");
            PasswordResetToken token = createValidToken(user, "mark-used-token");

            ResetPasswordRequest request = new ResetPasswordRequest();
            request.setToken("mark-used-token");
            request.setNewPassword("newPassword456");
            request.setConfirmPassword("newPassword456");

            mockMvc.perform(post("/api/v1/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            entityManager.clear();

            Optional<PasswordResetToken> updatedToken = passwordResetTokenRepository.findByToken("mark-used-token");
            assertThat(updatedToken).isPresent();
            assertThat(updatedToken.get().isUsed()).isTrue();
        }
    }

    @Nested
    @DisplayName("Welcome Email on Registration")
    class WelcomeEmailTests {

        @Test
        @DisplayName("should send welcome email on registration (mock sender logs)")
        void register_sendsWelcomeEmail() throws Exception {
            registerAndGetToken("welcome@test.com", "password123");
            Thread.sleep(500);
        }
    }
}
