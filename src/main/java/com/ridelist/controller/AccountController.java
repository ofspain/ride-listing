package com.ridelist.controller;

import com.ridelist.dto.request.ChangePasswordRequest;
import com.ridelist.dto.request.UpdateProfileRequest;
import com.ridelist.dto.response.ApiResponse;
import com.ridelist.dto.response.UserResponse;
import com.ridelist.security.UserPrincipal;
import com.ridelist.service.UserService;
import com.ridelist.util.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/account")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Account", description = "User account management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AccountController {

    private final UserService userService;

    @Operation(summary = "Get current user's profile")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(@CurrentUser UserPrincipal principal) {
        log.debug("Profile requested by user: {}", principal.getId());
        UserResponse response = userService.getProfile(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Update current user's profile")
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @CurrentUser UserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        log.info("Profile update requested by user: {}", principal.getId());
        UserResponse response = userService.updateProfile(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", response));
    }

    @Operation(summary = "Change current user's password")
    @PutMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @CurrentUser UserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        log.info("Password change requested by user: {}", principal.getId());
        userService.changePassword(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Password updated successfully", null));
    }

    @Operation(summary = "Delete current user's account (soft delete)")
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(@CurrentUser UserPrincipal principal) {
        log.info("Account deletion requested by user: {}", principal.getId());
        userService.deleteAccount(principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Account deleted", null));
    }
}
