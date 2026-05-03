package com.ridelist.service;

import com.ridelist.dto.mapper.UserMapper;
import com.ridelist.dto.request.LoginRequest;
import com.ridelist.dto.request.RefreshTokenRequest;
import com.ridelist.dto.request.RegisterRequest;
import com.ridelist.dto.response.AuthResponse;
import com.ridelist.dto.response.TokenResponse;
import com.ridelist.email.EmailMessage;
import com.ridelist.email.EmailTemplateService;
import com.ridelist.email.sender.EmailSenderFactory;
import com.ridelist.exception.BadRequestException;
import com.ridelist.exception.DuplicateResourceException;
import com.ridelist.exception.UnauthorizedException;
import com.ridelist.model.AccountType;
import com.ridelist.model.User;
import com.ridelist.repository.UserRepository;
import com.ridelist.security.JwtTokenProvider;
import com.ridelist.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailSenderFactory emailSenderFactory;
    private final EmailTemplateService emailTemplateService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user with email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }

        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        AccountType type = request.getAccountType() != null
                ? request.getAccountType()
                : AccountType.INDIVIDUAL;
        user.setAccountType(type);

        User savedUser = userRepository.save(user);
        log.info("User registered successfully with id: {}", savedUser.getId());

        sendWelcomeEmailAsync(savedUser);

        return generateAuthResponse(savedUser);
    }

    @Async
    public void sendWelcomeEmailAsync(User user) {
        try {
            EmailMessage message = emailTemplateService.buildWelcomeEmail(
                    user.getEmail(),
                    user.getFirstName()
            );
            emailSenderFactory.getActiveSender().send(message);
            log.info("Welcome email sent to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send welcome email to {}: {}", user.getEmail(), e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        if (!user.isEnabled()) {
            throw new BadRequestException("Account is disabled");
        }

        log.info("User logged in successfully: {}", user.getId());

        return generateAuthResponse(user);
    }

    private AuthResponse generateAuthResponse(User user) {
        UserPrincipal userPrincipal = new UserPrincipal(user);

        String accessToken = jwtTokenProvider.generateAccessToken(userPrincipal);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userPrincipal);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenExpiration())
                .user(userMapper.toResponse(user))
                .build();
    }

    @Transactional(readOnly = true)
    public TokenResponse refreshToken(RefreshTokenRequest request) {
        log.info("Processing refresh token request");

        String refreshToken = request.getRefreshToken();

        if (jwtTokenProvider.isTokenExpired(refreshToken)) {
            throw new UnauthorizedException("Refresh token expired. Please log in again.");
        }

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new UnauthorizedException("Invalid refresh token");
        }

        java.util.UUID userId = jwtTokenProvider.getUserIdFromToken(refreshToken);

        User user = userRepository.findByIdIncludingDeleted(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        if (user.getDeletedAt() != null || !user.isEnabled()) {
            throw new UnauthorizedException("Account is disabled or deleted");
        }

        UserPrincipal userPrincipal = new UserPrincipal(user);
        String newAccessToken = jwtTokenProvider.generateAccessToken(userPrincipal);

        log.info("Generated new access token for user: {}", userId);

        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenExpiration())
                .build();
    }
}
