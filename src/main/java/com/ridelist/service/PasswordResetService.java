package com.ridelist.service;

import com.ridelist.dto.request.ResetPasswordRequest;
import com.ridelist.email.EmailMessage;
import com.ridelist.email.EmailTemplateService;
import com.ridelist.email.sender.EmailSenderFactory;
import com.ridelist.exception.BadRequestException;
import com.ridelist.model.PasswordResetToken;
import com.ridelist.model.User;
import com.ridelist.repository.PasswordResetTokenRepository;
import com.ridelist.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailSenderFactory emailSenderFactory;
    private final EmailTemplateService emailTemplateService;

    private static final int TOKEN_EXPIRY_MINUTES = 30;
    private static final int TOKEN_LENGTH_BYTES = 32;

    @Async
    @Transactional
    public void initiatePasswordReset(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email.toLowerCase());

        if (userOpt.isEmpty()) {
            log.info("Password reset requested for non-existent email: {} (not revealed to caller)", email);
            return;
        }

        User user = userOpt.get();

        tokenRepository.invalidateAllForUser(user);

        byte[] tokenBytes = new byte[TOKEN_LENGTH_BYTES];
        new SecureRandom().nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .token(token)
                .expiresAt(LocalDateTime.now().plusMinutes(TOKEN_EXPIRY_MINUTES))
                .used(false)
                .build();

        tokenRepository.save(resetToken);

        try {
            EmailMessage message = emailTemplateService.buildPasswordRecoveryEmail(
                    user.getEmail(),
                    user.getFirstName(),
                    token
            );

            emailSenderFactory.getActiveSender().send(message);

            log.info("Password reset email sent to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}: {}", email, e.getMessage(), e);
        }
    }

    public void validateToken(String token) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException(
                        "Invalid or expired reset link. Please request a new one."
                ));

        if (!resetToken.isValid()) {
            throw new BadRequestException(
                    "This reset link has expired or already been used. Please request a new one."
            );
        }
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }

        PasswordResetToken resetToken = tokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset link"));

        if (!resetToken.isValid()) {
            throw new BadRequestException("This reset link has expired or already been used");
        }

        User user = resetToken.getUser();

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);

        tokenRepository.invalidateAllForUser(user);

        log.info("Password reset successful for user: {}", user.getId());
    }

    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupExpiredTokens() {
        tokenRepository.deleteExpiredTokens(LocalDateTime.now());
        log.info("Expired password reset tokens cleaned up");
    }
}
