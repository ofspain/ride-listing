package com.ridelist.controller;

import com.ridelist.dto.response.AdminDashboardStatsResponse;
import com.ridelist.dto.response.ApiResponse;
import com.ridelist.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;

    @GetMapping("/stats")
    public ApiResponse<AdminDashboardStatsResponse> getDashboardStats() {
        AdminDashboardStatsResponse stats = dashboardService.getDashboardStats();
        return ApiResponse.success(stats);
    }

    @PostMapping("/stats/refresh")
    public ApiResponse<AdminDashboardStatsResponse> refreshDashboardStats() {
        dashboardService.evictDashboardCache();
        AdminDashboardStatsResponse stats = dashboardService.getDashboardStats();
        return ApiResponse.success("Dashboard stats refreshed", stats);
    }
}
