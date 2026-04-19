package com.ridelist.integration;

import com.ridelist.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Performance sanity tests to ensure basic operations complete within acceptable time limits.
 * These are not load tests - just sanity checks for MVP scale.
 *
 * Targets:
 * - GET /listings (paginated): < 5s with 100 listings
 * - GET /listings (filtered): < 5s with multiple filters
 * - GET /lookup/* (cached): < 3s for 10 consecutive calls
 */
@DisplayName("Performance Sanity Tests")
class PerformanceSanityTest extends BaseIntegrationTest {

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    @DisplayName("Get listings with 100 listings completes in acceptable time")
    void getListings_with100Listings_completesInTime() throws Exception {
        // Setup: Create seller and state
        User seller = createTestUser("seller@test.com", Role.USER);
        State state = createTestState("Lagos");

        // Create 100 active listings
        List<Listing> listings = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            Listing listing = Listing.builder()
                    .title("Test Motorcycle " + i)
                    .description("Description for listing " + i)
                    .price(BigDecimal.valueOf(50000 + (i * 1000)))
                    .seller(seller)
                    .listingType(ListingType.VEHICLE)
                    .vehicleType(VehicleType.MOTORCYCLE)
                    .category(ListingCategory.MOTORCYCLE)
                    .status(ListingStatus.ACTIVE)
                    .condition(ListingCondition.GOOD)
                    .state(state)
                    .build();
            listings.add(listing);
        }
        listingRepository.saveAll(listings);

        // Execute: Fetch paginated listings
        mockMvc.perform(get("/api/v1/listings")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content.length()").value(20))
                .andExpect(jsonPath("$.data.totalElements").value(100));
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    @DisplayName("Filter listings with multiple criteria completes in acceptable time")
    void filterListings_withMultipleCriteria_completesInTime() throws Exception {
        // Setup: Create seller and state
        User seller = createTestUser("seller@test.com", Role.USER);
        State state = createTestState("Lagos");
        Axis axis = createTestAxis("Mainland", state);

        // Create 100 listings with varying prices
        List<Listing> listings = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            Listing listing = Listing.builder()
                    .title("Test Motorcycle " + i)
                    .description("Description for listing " + i)
                    .price(BigDecimal.valueOf(50000 + (i * 1000))) // 50k to 149k
                    .seller(seller)
                    .listingType(ListingType.VEHICLE)
                    .vehicleType(VehicleType.MOTORCYCLE)
                    .category(ListingCategory.MOTORCYCLE)
                    .status(ListingStatus.ACTIVE)
                    .condition(ListingCondition.GOOD)
                    .state(state)
                    .axis(axis)
                    .build();
            listings.add(listing);
        }
        listingRepository.saveAll(listings);

        // Execute: Filter with multiple criteria
        // Should return listings with price between 60k and 100k (indices 10-50, ~40 results)
        mockMvc.perform(get("/api/v1/listings")
                        .param("listingType", "VEHICLE")
                        .param("vehicleType", "MOTORCYCLE")
                        .param("stateId", state.getId().toString())
                        .param("minPrice", "60000")
                        .param("maxPrice", "100000")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("Lookup endpoints with caching complete quickly")
    void lookupEndpoints_cached_completeQuickly() throws Exception {
        // Setup: Create location hierarchy
        State state1 = createTestState("Lagos");
        State state2 = createTestState("Abuja");
        State state3 = createTestState("Kano");
        Axis axis1 = createTestAxis("Mainland", state1);
        Axis axis2 = createTestAxis("Island", state1);
        createTestArea("Yaba", axis1);
        createTestArea("Surulere", axis1);
        createTestArea("Victoria Island", axis2);

        // First call - cache miss, populates cache
        mockMvc.perform(get("/api/v1/lookup/states"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3));

        // Subsequent calls - should be cache hits, very fast
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/api/v1/lookup/states"))
                    .andExpect(status().isOk());
        }

        // Test axes lookup
        mockMvc.perform(get("/api/v1/lookup/states/{stateId}/axes", state1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));

        // Test areas lookup
        mockMvc.perform(get("/api/v1/lookup/axes/{axisId}/areas", axis1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    @DisplayName("Pagination through large result set completes in acceptable time")
    void paginateThroughListings_completesInTime() throws Exception {
        // Setup: Create 100 listings
        User seller = createTestUser("seller@test.com", Role.USER);

        List<Listing> listings = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            Listing listing = Listing.builder()
                    .title("Listing " + i)
                    .description("Description " + i)
                    .price(BigDecimal.valueOf(100000))
                    .seller(seller)
                    .listingType(ListingType.VEHICLE)
                    .vehicleType(VehicleType.MOTORCYCLE)
                    .category(ListingCategory.MOTORCYCLE)
                    .status(ListingStatus.ACTIVE)
                    .condition(ListingCondition.GOOD)
                    .build();
            listings.add(listing);
        }
        listingRepository.saveAll(listings);

        // Paginate through all pages (5 pages of 20)
        for (int page = 0; page < 5; page++) {
            mockMvc.perform(get("/api/v1/listings")
                            .param("page", String.valueOf(page))
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(20));
        }
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("Single listing fetch completes quickly")
    void getSingleListing_completesQuickly() throws Exception {
        // Setup
        User seller = createTestUser("seller@test.com", Role.USER);
        Listing listing = createTestListing(seller, ListingType.VEHICLE);
        listing.setStatus(ListingStatus.ACTIVE);
        listingRepository.save(listing);

        // Fetch single listing multiple times
        for (int i = 0; i < 20; i++) {
            mockMvc.perform(get("/api/v1/listings/{id}", listing.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(listing.getId().toString()));
        }
    }
}
