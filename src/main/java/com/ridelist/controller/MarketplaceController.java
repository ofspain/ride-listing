package com.ridelist.controller;

import com.ridelist.dto.response.ApiResponse;
import com.ridelist.dto.response.MarketplaceStatsResponse;
import com.ridelist.service.MarketplaceStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/marketplace")
@RequiredArgsConstructor
@Tag(name = "Marketplace", description = "Public marketplace endpoints")
public class MarketplaceController {

    private final MarketplaceStatsService statsService;

    @Operation(summary = "Get marketplace statistics for homepage")
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<MarketplaceStatsResponse>> getMarketplaceStats() {
        return ResponseEntity.ok(ApiResponse.success(statsService.getMarketplaceStats()));
    }
}
