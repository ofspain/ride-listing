package com.ridelist.controller;

import com.ridelist.dto.response.ApiResponse;
import com.ridelist.dto.response.ImpersonationResponse;
import com.ridelist.dto.response.PagedResponse;
import com.ridelist.dto.response.UserAdminResponse;
import com.ridelist.model.*;
import com.ridelist.security.UserPrincipal;
import com.ridelist.service.ImpersonationService;
import com.ridelist.service.UserService;
import com.ridelist.util.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Users", description = "Admin endpoints for user management")
public class AdminUserController {

    private final ImpersonationService impersonationService;
    private final UserService userService;


    @GetMapping("")
    public ResponseEntity<ApiResponse<PagedResponse<UserAdminResponse>>> getUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) AccountType accountType,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime deletedFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime deletedTo,
            @PageableDefault(
                    size = 20,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {

        PagedResponse<UserAdminResponse> response = userService.getUsers(
                search,
                accountType,
                enabled,
                deletedFrom,
                deletedTo,
                pageable
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{userId}/impersonate")
    @Operation(
            summary = "Impersonate user",
            description = "Generate a short-lived token to act on behalf of a non-admin user. " +
                    "Token expires in 30 minutes with no refresh."
    )
    public ResponseEntity<ApiResponse<ImpersonationResponse>> impersonateUser(
            @PathVariable UUID userId,
            @CurrentUser UserPrincipal principal) {

        ImpersonationResponse response = impersonationService.impersonateUser(userId, principal.getId());

        return ResponseEntity.ok(ApiResponse.success("Impersonation session started", response));
    }
}
