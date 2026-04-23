package com.ridelist.controller;

import com.ridelist.dto.response.ApiResponse;
import com.ridelist.security.UserPrincipal;
import com.ridelist.service.AccountService;
import com.ridelist.util.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/account")
@RequiredArgsConstructor
@Slf4j
public class AccountController {

    private final AccountService accountService;

    /**
     * Delete the authenticated user's account (soft delete).
     *
     * This endpoint:
     * - Disables the user account
     * - Sets deletedAt timestamp
     * - Marks all user's listings as DELETED
     * - Deletes all user's favorites
     * - Preserves messages for historical records
     *
     * @param principal The authenticated user
     * @return Success response
     */
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteAccount(@CurrentUser UserPrincipal principal) {
        log.info("Account deletion requested by user: {}", principal.getId());
        accountService.deleteAccount(principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Account deleted successfully", null));
    }
}
