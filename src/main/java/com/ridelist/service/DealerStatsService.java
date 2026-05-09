package com.ridelist.service;

import com.ridelist.cache.InMemoryCache;
import com.ridelist.dto.response.DealerInquiryStatsResponse;
import com.ridelist.dto.response.DealerInquiryStatsResponse.InquiryByListing;
import com.ridelist.dto.response.DealerInquiryStatsResponse.TrendDirection;
import com.ridelist.dto.response.DealerListingStatsResponse;
import com.ridelist.dto.response.DealerListingStatsResponse.StatusCount;
import com.ridelist.dto.response.DealerProfileStatsResponse;
import com.ridelist.model.ListingStatus;
import com.ridelist.repository.ContactRequestRepository;
import com.ridelist.repository.FavoriteRepository;
import com.ridelist.repository.ListingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class DealerStatsService {

    private final ListingRepository listingRepository;
    private final ContactRequestRepository contactRequestRepository;
    private final FavoriteRepository favoriteRepository;
    private final InMemoryCache cache;

    private static final String LISTING_STATS_PREFIX = "dealer:listing-stats:";
    private static final String INQUIRY_STATS_PREFIX = "dealer:inquiry-stats:";
    private static final String PROFILE_STATS_PREFIX = "dealer:profile-stats:";

    public DealerListingStatsResponse getListingStats(UUID sellerId) {
        String cacheKey = LISTING_STATS_PREFIX + sellerId;
        return cache.get(cacheKey, () -> computeListingStats(sellerId));
    }

    public DealerInquiryStatsResponse getInquiryStats(UUID sellerId) {
        String cacheKey = INQUIRY_STATS_PREFIX + sellerId;
        return cache.get(cacheKey, () -> computeInquiryStats(sellerId));
    }

    public DealerProfileStatsResponse getProfileStats(UUID sellerId) {
        String cacheKey = PROFILE_STATS_PREFIX + sellerId;
        return cache.get(cacheKey, () -> computeProfileStats(sellerId));
    }

    public void invalidateListingStats(UUID sellerId) {
        cache.evict(LISTING_STATS_PREFIX + sellerId);
        cache.evict(PROFILE_STATS_PREFIX + sellerId);
    }

    public void invalidateInquiryStats(UUID sellerId) {
        cache.evict(INQUIRY_STATS_PREFIX + sellerId);
    }

    private DealerListingStatsResponse computeListingStats(UUID sellerId) {
        log.debug("Computing listing stats for seller: {}", sellerId);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime weekStart = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toLocalDate().atStartOfDay();
        LocalDateTime monthStart = now.withDayOfMonth(1).toLocalDate().atStartOfDay();

        Object[] result = listingRepository.computeDealerListingStats(sellerId, weekStart, monthStart);

        if (result == null || result.length == 0) {
            return buildEmptyListingStats();
        }

        Object[] row = (result[0] instanceof Object[]) ? (Object[]) result[0] : result;

        Long total = toLong(row[0]);
        Long published = toLong(row[1]);
        Long active = toLong(row[2]);
        Long draft = toLong(row[3]);
        Long sold = toLong(row[4]);
        Long expired = toLong(row[5]);

        List<StatusCount> breakdown = buildStatusBreakdown(sellerId, total);

        return DealerListingStatsResponse.builder()
                .total(total)
                .published(published)
                .active(active)
                .draft(draft)
                .sold(sold)
                .expired(expired)
                .newThisWeek(toLong(row[6]))
                .soldThisWeek(toLong(row[7]))
                .newThisMonth(toLong(row[8]))
                .soldThisMonth(toLong(row[9]))
                .statusBreakdown(breakdown)
                .mostViewedListing(null)
                .build();
    }

    private List<StatusCount> buildStatusBreakdown(UUID sellerId, Long total) {
        List<Object[]> statusCounts = listingRepository.countBySellerGroupByStatus(sellerId);
        List<StatusCount> breakdown = new ArrayList<>();

        for (Object[] row : statusCounts) {
            ListingStatus status = (ListingStatus) row[0];
            Long count = (Long) row[1];
            Double percentage = total > 0 ? (count * 100.0) / total : 0.0;
            percentage = Math.round(percentage * 10.0) / 10.0;

            breakdown.add(StatusCount.builder()
                    .status(status)
                    .count(count)
                    .percentage(percentage)
                    .build());
        }

        return breakdown;
    }

    private DealerListingStatsResponse buildEmptyListingStats() {
        return DealerListingStatsResponse.builder()
                .total(0L)
                .published(0L)
                .active(0L)
                .draft(0L)
                .sold(0L)
                .expired(0L)
                .newThisWeek(0L)
                .soldThisWeek(0L)
                .newThisMonth(0L)
                .soldThisMonth(0L)
                .statusBreakdown(List.of())
                .mostViewedListing(null)
                .build();
    }

    private DealerInquiryStatsResponse computeInquiryStats(UUID sellerId) {
        log.debug("Computing inquiry stats for seller: {}", sellerId);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime weekStart = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toLocalDate().atStartOfDay();
        LocalDateTime lastWeekStart = weekStart.minusWeeks(1);
        LocalDateTime monthStart = now.withDayOfMonth(1).toLocalDate().atStartOfDay();
        LocalDateTime lastMonthStart = monthStart.minusMonths(1);

        Object[] result = contactRequestRepository.computeDealerInquiryStats(
                sellerId, weekStart, lastWeekStart, monthStart, lastMonthStart);

        if (result == null || result.length == 0) {
            return buildEmptyInquiryStats();
        }

        Object[] row = (result[0] instanceof Object[]) ? (Object[]) result[0] : result;

        Long total = toLong(row[0]);
        Long unread = toLong(row[1]);
        Long thisWeek = toLong(row[2]);
        Long lastWeek = toLong(row[3]);
        Long thisMonth = toLong(row[4]);
        Long lastMonth = toLong(row[5]);

        TrendDirection trend;
        Double trendPercent = null;

        if (thisWeek > lastWeek) {
            trend = TrendDirection.UP;
            trendPercent = lastWeek > 0 ? ((thisWeek - lastWeek) * 100.0) / lastWeek : 100.0;
        } else if (thisWeek < lastWeek) {
            trend = TrendDirection.DOWN;
            trendPercent = lastWeek > 0 ? ((lastWeek - thisWeek) * 100.0) / lastWeek : 0.0;
        } else {
            trend = TrendDirection.STABLE;
        }

        if (trendPercent != null) {
            trendPercent = Math.round(trendPercent * 10.0) / 10.0;
        }

        List<InquiryByListing> byListing = buildInquiryByListing(sellerId);

        return DealerInquiryStatsResponse.builder()
                .total(total)
                .unread(unread)
                .thisWeek(thisWeek)
                .thisMonth(thisMonth)
                .lastMonth(lastMonth)
                .weeklyTrend(trend)
                .weeklyTrendPercent(trendPercent)
                .byListing(byListing)
                .build();
    }

    private List<InquiryByListing> buildInquiryByListing(UUID sellerId) {
        List<Object[]> top5 = contactRequestRepository.findTop5ListingsByInquiryCount(sellerId);
        List<InquiryByListing> result = new ArrayList<>();

        for (Object[] row : top5) {
            result.add(InquiryByListing.builder()
                    .listingId((UUID) row[0])
                    .listingTitle((String) row[1])
                    .count(toLong(row[2]))
                    .build());
        }

        return result;
    }

    private DealerInquiryStatsResponse buildEmptyInquiryStats() {
        return DealerInquiryStatsResponse.builder()
                .total(0L)
                .unread(0L)
                .thisWeek(0L)
                .thisMonth(0L)
                .lastMonth(0L)
                .weeklyTrend(TrendDirection.STABLE)
                .weeklyTrendPercent(null)
                .byListing(List.of())
                .build();
    }

    private DealerProfileStatsResponse computeProfileStats(UUID sellerId) {
        log.debug("Computing profile stats for seller: {}", sellerId);

        long favoritesReceived = favoriteRepository.countFavoritesReceivedBySeller(sellerId);

        return DealerProfileStatsResponse.builder()
                .totalListingViews(0L)
                .viewsThisWeek(0L)
                .viewsLastWeek(0L)
                .totalFavoritesReceived(favoritesReceived)
                .build();
    }

    private Long toLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Number) return ((Number) value).longValue();
        return 0L;
    }
}
