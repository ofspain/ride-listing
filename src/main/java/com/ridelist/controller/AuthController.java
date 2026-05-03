package com.ridelist.controller;

import com.ridelist.dto.request.ForgotPasswordRequest;
import com.ridelist.dto.request.LoginRequest;
import com.ridelist.dto.request.RefreshTokenRequest;
import com.ridelist.dto.request.RegisterRequest;
import com.ridelist.dto.request.ResetPasswordRequest;
import com.ridelist.dto.request.ValidateResetTokenRequest;
import com.ridelist.dto.response.ApiResponse;
import com.ridelist.dto.response.AuthResponse;
import com.ridelist.dto.response.TokenResponse;
import com.ridelist.service.AuthService;
import com.ridelist.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User authentication endpoints")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @Operation(summary = "Register a new user")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration successful", response));
    }

    @Operation(summary = "Login with email and password")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @Operation(summary = "Refresh access token using refresh token")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        TokenResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Request password reset email")
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.initiatePasswordReset(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success(
                "If an account exists with this email, you will receive a password reset link shortly.",
                null
        ));
    }

    @Operation(summary = "Validate password reset token")
    @PostMapping("/validate-reset-token")
    public ResponseEntity<ApiResponse<Void>> validateResetToken(@Valid @RequestBody ValidateResetTokenRequest request) {
        passwordResetService.validateToken(request.getToken());
        return ResponseEntity.ok(ApiResponse.success("Token is valid", null));
    }

    @Operation(summary = "Reset password with token")
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success(
                "Password reset successfully. You can now log in with your new password.",
                null
        ));
    }
}
