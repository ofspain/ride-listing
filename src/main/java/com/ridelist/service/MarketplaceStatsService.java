package com.ridelist.service;

import com.ridelist.cache.InMemoryCache;
import com.ridelist.dto.response.MarketplaceStatsResponse;
import com.ridelist.repository.ListingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class MarketplaceStatsService {

    private final ListingRepository listingRepository;
    private final InMemoryCache cache;

    private static final String CACHE_KEY = "marketplace:stats";

    public MarketplaceStatsResponse getMarketplaceStats() {
        return cache.get(CACHE_KEY, this::computeStats);
    }

    private MarketplaceStatsResponse computeStats() {
        log.info("Computing marketplace stats (cache miss)");

        LocalDateTime yesterday = LocalDateTime.now().minusHours(24);
        Object[] result = listingRepository.computeMarketplaceStats(yesterday);

        if (result == null || result.length == 0) {
            return MarketplaceStatsResponse.builder()
                    .totalListings(0L)
                    .activeListings(0L)
                    .totalSellers(0L)
                    .totalStates(0L)
                    .totalMakes(0L)
                    .newListingsToday(0L)
                    .build();
        }

        Object[] row = (result[0] instanceof Object[]) ? (Object[]) result[0] : result;

        return MarketplaceStatsResponse.builder()
                .totalListings(toLong(row[0]))
                .activeListings(toLong(row[1]))
                .totalSellers(toLong(row[2]))
                .totalStates(toLong(row[3]))
                .totalMakes(toLong(row[4]))
                .newListingsToday(toLong(row[5]))
                .build();
    }

    private Long toLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Number) return ((Number) value).longValue();
        return 0L;
    }

    @Scheduled(fixedRate = 1800000)
    public void evictStatsCache() {
        cache.evict(CACHE_KEY);
        log.debug("Marketplace stats cache evicted");
    }
}
