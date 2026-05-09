package com.ridelist.dto.response;

import com.ridelist.model.AccountType;
import com.ridelist.model.ListingStatus;
import com.ridelist.model.ListingType;
import com.ridelist.model.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardStatsResponse {

    private Long totalListings;
    private Long activeListings;
    private Long totalUsers;
    private Long pendingInquiries;
    private Long listingsThisWeek;
    private Long usersThisWeek;

    private List<StatusCount> statusBreakdown;

    private List<RecentListing> recentListings;
    private List<RecentUser> recentUsers;

    private LocalDateTime generatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusCount {
        private ListingStatus status;
        private Long count;
        private Double percentage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentListing {
        private UUID id;
        private String title;
        private ListingType listingType;
        private ListingStatus status;
        private String sellerName;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentUser {
        private UUID id;
        private String firstName;
        private String lastName;
        private String email;
        private AccountType accountType;
        private Role role;
        private LocalDateTime createdAt;
    }
}
