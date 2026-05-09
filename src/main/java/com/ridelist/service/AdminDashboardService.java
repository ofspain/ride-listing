package com.ridelist.service;

import com.ridelist.cache.InMemoryCache;
import com.ridelist.dto.response.AdminDashboardStatsResponse;
import com.ridelist.dto.response.AdminDashboardStatsResponse.RecentListing;
import com.ridelist.dto.response.AdminDashboardStatsResponse.RecentUser;
import com.ridelist.dto.response.AdminDashboardStatsResponse.StatusCount;
import com.ridelist.model.ContactStatus;
import com.ridelist.model.Listing;
import com.ridelist.model.ListingStatus;
import com.ridelist.model.User;
import com.ridelist.repository.ContactRequestRepository;
import com.ridelist.repository.ListingRepository;
import com.ridelist.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdminDashboardService {

    private static final String CACHE_KEY = "admin:dashboard:stats";

    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final ContactRequestRepository contactRequestRepository;
    private final InMemoryCache cache;

    public AdminDashboardStatsResponse getDashboardStats() {
        log.debug("Fetching admin dashboard stats");
        return cache.get(CACHE_KEY, this::computeDashboardStats);
    }

    public void evictDashboardCache() {
        cache.evict(CACHE_KEY);
        log.info("Evicted admin dashboard cache");
    }

    private AdminDashboardStatsResponse computeDashboardStats() {
        log.info("Computing admin dashboard stats");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneWeekAgo = now.minusDays(7);

        long totalListings = listingRepository.count();
        long activeListings = listingRepository.countByStatus(ListingStatus.ACTIVE);
        long totalUsers = userRepository.count();
        long pendingInquiries = contactRequestRepository.countByStatus(ContactStatus.PENDING);
        long listingsThisWeek = listingRepository.countByCreatedAtAfter(oneWeekAgo);
        long usersThisWeek = userRepository.countByCreatedAtAfter(oneWeekAgo);

        List<StatusCount> statusBreakdown = computeStatusBreakdown(totalListings);
        List<RecentListing> recentListings = fetchRecentListings();
        List<RecentUser> recentUsers = fetchRecentUsers();

        return AdminDashboardStatsResponse.builder()
                .totalListings(totalListings)
                .activeListings(activeListings)
                .totalUsers(totalUsers)
                .pendingInquiries(pendingInquiries)
                .listingsThisWeek(listingsThisWeek)
                .usersThisWeek(usersThisWeek)
                .statusBreakdown(statusBreakdown)
                .recentListings(recentListings)
                .recentUsers(recentUsers)
                .generatedAt(now)
                .build();
    }

    private List<StatusCount> computeStatusBreakdown(long total) {
        List<Object[]> results = listingRepository.countGroupByStatus();

        Map<ListingStatus, Long> countMap = new EnumMap<>(ListingStatus.class);
        for (ListingStatus status : ListingStatus.values()) {
            countMap.put(status, 0L);
        }

        for (Object[] row : results) {
            ListingStatus status = (ListingStatus) row[0];
            Long count = (Long) row[1];
            countMap.put(status, count);
        }

        List<StatusCount> breakdown = new ArrayList<>();
        for (ListingStatus status : ListingStatus.values()) {
            Long count = countMap.get(status);
            double percentage = total > 0 ? (count * 100.0) / total : 0.0;
            breakdown.add(StatusCount.builder()
                    .status(status)
                    .count(count)
                    .percentage(Math.round(percentage * 10.0) / 10.0)
                    .build());
        }

        return breakdown;
    }

    private List<RecentListing> fetchRecentListings() {
        List<Listing> listings = listingRepository.findTop10RecentWithSeller(PageRequest.of(0, 10));

        return listings.stream()
                .map(listing -> RecentListing.builder()
                        .id(listing.getId())
                        .title(listing.getTitle())
                        .listingType(listing.getListingType())
                        .status(listing.getStatus())
                        .sellerName(buildSellerName(listing.getSeller()))
                        .createdAt(listing.getCreatedAt())
                        .build())
                .toList();
    }

    private List<RecentUser> fetchRecentUsers() {
        List<User> users = userRepository.findTop10ByOrderByCreatedAtDesc();

        return users.stream()
                .map(user -> RecentUser.builder()
                        .id(user.getId())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .email(user.getEmail())
                        .accountType(user.getAccountType())
                        .role(user.getRole())
                        .createdAt(user.getCreatedAt())
                        .build())
                .toList();
    }

    private String buildSellerName(User seller) {
        if (seller == null) {
            return "Unknown";
        }
        String firstName = seller.getFirstName() != null ? seller.getFirstName() : "";
        String lastName = seller.getLastName() != null ? seller.getLastName() : "";
        return (firstName + " " + lastName).trim();
    }
}
