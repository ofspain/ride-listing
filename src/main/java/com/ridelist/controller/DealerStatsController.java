package com.ridelist.controller;

import com.ridelist.dto.response.ApiResponse;
import com.ridelist.dto.response.DealerInquiryStatsResponse;
import com.ridelist.dto.response.DealerListingStatsResponse;
import com.ridelist.dto.response.DealerProfileStatsResponse;
import com.ridelist.security.UserPrincipal;
import com.ridelist.service.DealerStatsService;
import com.ridelist.util.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/account/stats")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Dealer Stats", description = "Dealer account statistics endpoints")
public class DealerStatsController {

    private final DealerStatsService statsService;

    @Operation(summary = "Get dealer listing statistics")
    @GetMapping("/listings")
    public ResponseEntity<ApiResponse<DealerListingStatsResponse>> getListingStats(
            @CurrentUser UserPrincipal currentUser
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                statsService.getListingStats(currentUser.getId())
        ));
    }

    @Operation(summary = "Get dealer inquiry statistics")
    @GetMapping("/inquiries")
    public ResponseEntity<ApiResponse<DealerInquiryStatsResponse>> getInquiryStats(
            @CurrentUser UserPrincipal currentUser
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                statsService.getInquiryStats(currentUser.getId())
        ));
    }

    @Operation(summary = "Get dealer profile statistics")
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<DealerProfileStatsResponse>> getProfileStats(
            @CurrentUser UserPrincipal currentUser
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                statsService.getProfileStats(currentUser.getId())
        ));
    }
}
