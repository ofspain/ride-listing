package com.ridelist.dto.response;

import com.ridelist.model.ListingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DealerListingStatsResponse {
    private Long total;
    private Long published;
    private Long active;
    private Long draft;
    private Long sold;
    private Long expired;

    private Long newThisWeek;
    private Long soldThisWeek;
    private Long newThisMonth;
    private Long soldThisMonth;

    private List<StatusCount> statusBreakdown;
    private MostViewedListing mostViewedListing;

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
    public static class MostViewedListing {
        private UUID id;
        private String title;
        private Long viewCount;
        private String primaryImageUrl;
        private ListingStatus status;
    }
}
