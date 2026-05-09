package com.ridelist.dto.response;

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
public class DealerInquiryStatsResponse {
    private Long total;
    private Long unread;

    private Long thisWeek;
    private Long thisMonth;
    private Long lastMonth;

    private TrendDirection weeklyTrend;
    private Double weeklyTrendPercent;

    private List<InquiryByListing> byListing;

    public enum TrendDirection {
        UP, DOWN, STABLE
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InquiryByListing {
        private UUID listingId;
        private String listingTitle;
        private Long count;
    }
}
