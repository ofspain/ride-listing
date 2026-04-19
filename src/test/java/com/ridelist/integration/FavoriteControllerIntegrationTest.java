package com.ridelist.integration;

import com.ridelist.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("FavoriteController Integration Tests")
public class FavoriteControllerIntegrationTest extends BaseIntegrationTest {

    // ==================== ADD TO FAVORITES TESTS (FAV-001 to FAV-004) ====================

    @Nested
    @DisplayName("Add to Favorites Tests")
    class AddToFavoritesTests {

        @Test
        @DisplayName("FAV-001: Add active listing to favorites succeeds")
        void addToFavorites_validListing_returns201() throws Exception {
            User seller = createTestUser("seller@test.com", Role.USER);
            State state = createTestState("Lagos");

            Listing listing = Listing.builder()
                    .title("Active Listing")
                    .price(BigDecimal.valueOf(150000))
                    .seller(seller)
                    .listingType(ListingType.VEHICLE)
                    .vehicleType(VehicleType.MOTORCYCLE)
                    .category(ListingCategory.MOTORCYCLE)
                    .status(ListingStatus.ACTIVE)
                    .condition(ListingCondition.GOOD)
                    .state(state)
                    .build();
            listing = listingRepository.save(listing);

            String buyerToken = registerAndGetToken("buyer@test.com", "Password123!");

            mockMvc.perform(post("/api/v1/account/favorites/" + listing.getId())
                            .header("Authorization", authHeader(buyerToken)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Added to favorites"));
        }

        @Test
        @DisplayName("FAV-002: Add duplicate favorite returns 400")
        void addToFavorites_duplicate_returns400() throws Exception {
            User seller = createTestUser("seller@test.com", Role.USER);
            State state = createTestState("Lagos");

            Listing listing = Listing.builder()
                    .title("Active Listing")
                    .price(BigDecimal.valueOf(150000))
                    .seller(seller)
                    .listingType(ListingType.VEHICLE)
                    .vehicleType(VehicleType.MOTORCYCLE)
                    .category(ListingCategory.MOTORCYCLE)
                    .status(ListingStatus.ACTIVE)
                    .condition(ListingCondition.GOOD)
                    .state(state)
                    .build();
            listing = listingRepository.save(listing);

            String buyerToken = registerAndGetToken("buyer@test.com", "Password123!");

            // First add - should succeed
            mockMvc.perform(post("/api/v1/account/favorites/" + listing.getId())
                            .header("Authorization", authHeader(buyerToken)))
                    .andExpect(status().isCreated());

            // Duplicate add - should fail with 400 (BadRequestException)
            mockMvc.perform(post("/api/v1/account/favorites/" + listing.getId())
                            .header("Authorization", authHeader(buyerToken)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("FAV-003: Add own listing to favorites returns 400")
        void addToFavorites_ownListing_returns400() throws Exception {
            String sellerToken = registerAndGetToken("seller@test.com", "Password123!");
            User seller = userRepository.findByEmail("seller@test.com").orElseThrow();
            State state = createTestState("Lagos");

            Listing listing = Listing.builder()
                    .title("My Listing")
                    .price(BigDecimal.valueOf(150000))
                    .seller(seller)
                    .listingType(ListingType.VEHICLE)
                    .vehicleType(VehicleType.MOTORCYCLE)
                    .category(ListingCategory.MOTORCYCLE)
                    .status(ListingStatus.ACTIVE)
                    .condition(ListingCondition.GOOD)
                    .state(state)
                    .build();
            listing = listingRepository.save(listing);

            mockMvc.perform(post("/api/v1/account/favorites/" + listing.getId())
                            .header("Authorization", authHeader(sellerToken)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("FAV-004: Add deleted listing to favorites returns 400")
        void addToFavorites_deletedListing_returns400() throws Exception {
            User seller = createTestUser("seller@test.com", Role.USER);
            State state = createTestState("Lagos");

            Listing listing = Listing.builder()
                    .title("Deleted Listing")
                    .price(BigDecimal.valueOf(150000))
                    .seller(seller)
                    .listingType(ListingType.VEHICLE)
                    .vehicleType(VehicleType.MOTORCYCLE)
                    .category(ListingCategory.MOTORCYCLE)
                    .status(ListingStatus.DELETED)
                    .condition(ListingCondition.GOOD)
                    .state(state)
                    .build();
            listing = listingRepository.save(listing);

            String buyerToken = registerAndGetToken("buyer@test.com", "Password123!");

            mockMvc.perform(post("/api/v1/account/favorites/" + listing.getId())
                            .header("Authorization", authHeader(buyerToken)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("FAV-004b: Add non-existent listing to favorites returns 404")
        void addToFavorites_nonExistentListing_returns404() throws Exception {
            String buyerToken = registerAndGetToken("buyer@test.com", "Password123!");

            mockMvc.perform(post("/api/v1/account/favorites/" + UUID.randomUUID())
                            .header("Authorization", authHeader(buyerToken)))
                    .andExpect(status().isNotFound());
        }
    }

    // ==================== REMOVE FROM FAVORITES TESTS (FAV-005 to FAV-006) ====================

    @Nested
    @DisplayName("Remove from Favorites Tests")
    class RemoveFromFavoritesTests {

        @Test
        @DisplayName("FAV-005: Remove from favorites succeeds")
        void removeFromFavorites_existingFavorite_returns200() throws Exception {
            User seller = createTestUser("seller@test.com", Role.USER);
            State state = createTestState("Lagos");

            Listing listing = Listing.builder()
                    .title("Active Listing")
                    .price(BigDecimal.valueOf(150000))
                    .seller(seller)
                    .listingType(ListingType.VEHICLE)
                    .vehicleType(VehicleType.MOTORCYCLE)
                    .category(ListingCategory.MOTORCYCLE)
                    .status(ListingStatus.ACTIVE)
                    .condition(ListingCondition.GOOD)
                    .state(state)
                    .build();
            listing = listingRepository.save(listing);

            String buyerToken = registerAndGetToken("buyer@test.com", "Password123!");
            User buyer = userRepository.findByEmail("buyer@test.com").orElseThrow();

            // Add to favorites first
            Favorite favorite = Favorite.builder()
                    .user(buyer)
                    .listing(listing)
                    .build();
            favoriteRepository.save(favorite);

            // Now remove
            mockMvc.perform(delete("/api/v1/account/favorites/" + listing.getId())
                            .header("Authorization", authHeader(buyerToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Removed from favorites"));
        }

        @Test
        @DisplayName("FAV-006: Remove non-favorited listing returns 404")
        void removeFromFavorites_notInFavorites_returns404() throws Exception {
            User seller = createTestUser("seller@test.com", Role.USER);
            State state = createTestState("Lagos");

            Listing listing = Listing.builder()
                    .title("Active Listing")
                    .price(BigDecimal.valueOf(150000))
                    .seller(seller)
                    .listingType(ListingType.VEHICLE)
                    .vehicleType(VehicleType.MOTORCYCLE)
                    .category(ListingCategory.MOTORCYCLE)
                    .status(ListingStatus.ACTIVE)
                    .condition(ListingCondition.GOOD)
                    .state(state)
                    .build();
            listing = listingRepository.save(listing);

            String buyerToken = registerAndGetToken("buyer@test.com", "Password123!");

            // Try to remove without having favorited
            mockMvc.perform(delete("/api/v1/account/favorites/" + listing.getId())
                            .header("Authorization", authHeader(buyerToken)))
                    .andExpect(status().isNotFound());
        }
    }

    // ==================== GET FAVORITES TESTS (FAV-007 to FAV-008) ====================

    @Nested
    @DisplayName("Get Favorites Tests")
    class GetFavoritesTests {

        @Test
        @DisplayName("FAV-007: Get user favorites returns paginated list")
        void getUserFavorites_withFavorites_returnsPaginatedList() throws Exception {
            User seller = createTestUser("seller@test.com", Role.USER);
            State state = createTestState("Lagos");

            Listing listing1 = Listing.builder()
                    .title("Listing 1")
                    .price(BigDecimal.valueOf(100000))
                    .seller(seller)
                    .listingType(ListingType.VEHICLE)
                    .vehicleType(VehicleType.MOTORCYCLE)
                    .category(ListingCategory.MOTORCYCLE)
                    .status(ListingStatus.ACTIVE)
                    .condition(ListingCondition.GOOD)
                    .state(state)
                    .build();

            Listing listing2 = Listing.builder()
                    .title("Listing 2")
                    .price(BigDecimal.valueOf(200000))
                    .seller(seller)
                    .listingType(ListingType.PART)
                    .partName("Part")
                    .category(ListingCategory.SPARE_PART)
                    .status(ListingStatus.ACTIVE)
                    .condition(ListingCondition.NEW)
                    .state(state)
                    .build();

            listingRepository.save(listing1);
            listingRepository.save(listing2);

            String buyerToken = registerAndGetToken("buyer@test.com", "Password123!");
            User buyer = userRepository.findByEmail("buyer@test.com").orElseThrow();

            // Add both listings to favorites
            favoriteRepository.save(Favorite.builder().user(buyer).listing(listing1).build());
            favoriteRepository.save(Favorite.builder().user(buyer).listing(listing2).build());

            mockMvc.perform(get("/api/v1/account/favorites")
                            .header("Authorization", authHeader(buyerToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content.length()").value(2))
                    .andExpect(jsonPath("$.data.totalElements").value(2));
        }

        @Test
        @DisplayName("FAV-007b: Get user favorites with no favorites returns empty list")
        void getUserFavorites_noFavorites_returnsEmptyList() throws Exception {
            String buyerToken = registerAndGetToken("buyer@test.com", "Password123!");

            mockMvc.perform(get("/api/v1/account/favorites")
                            .header("Authorization", authHeader(buyerToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content.length()").value(0));
        }

        @Test
        @DisplayName("FAV-008: Access favorites without auth returns 401")
        void getUserFavorites_noAuth_returns401() throws Exception {
            mockMvc.perform(get("/api/v1/account/favorites"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("FAV-008b: Add to favorites without auth returns 401")
        void addToFavorites_noAuth_returns401() throws Exception {
            User seller = createTestUser("seller@test.com", Role.USER);
            Listing listing = createTestListing(seller, ListingType.VEHICLE);

            mockMvc.perform(post("/api/v1/account/favorites/" + listing.getId()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("FAV-008c: Remove from favorites without auth returns 401")
        void removeFromFavorites_noAuth_returns401() throws Exception {
            User seller = createTestUser("seller@test.com", Role.USER);
            Listing listing = createTestListing(seller, ListingType.VEHICLE);

            mockMvc.perform(delete("/api/v1/account/favorites/" + listing.getId()))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ==================== PAGINATION TESTS ====================

    @Nested
    @DisplayName("Favorites Pagination Tests")
    class FavoritesPaginationTests {

        @Test
        @DisplayName("Favorites pagination returns correct page info")
        void getUserFavorites_pagination_returnsCorrectPage() throws Exception {
            User seller = createTestUser("seller@test.com", Role.USER);
            State state = createTestState("Lagos");

            String buyerToken = registerAndGetToken("buyer@test.com", "Password123!");
            User buyer = userRepository.findByEmail("buyer@test.com").orElseThrow();

            // Create 15 listings and favorite them
            for (int i = 0; i < 15; i++) {
                Listing listing = Listing.builder()
                        .title("Listing " + i)
                        .price(BigDecimal.valueOf(100000 + i))
                        .seller(seller)
                        .listingType(ListingType.VEHICLE)
                        .vehicleType(VehicleType.MOTORCYCLE)
                        .category(ListingCategory.MOTORCYCLE)
                        .status(ListingStatus.ACTIVE)
                        .condition(ListingCondition.GOOD)
                        .state(state)
                        .build();
                listingRepository.save(listing);
                favoriteRepository.save(Favorite.builder().user(buyer).listing(listing).build());
            }

            // Request page 0 with size 10
            mockMvc.perform(get("/api/v1/account/favorites")
                            .header("Authorization", authHeader(buyerToken))
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(10))
                    .andExpect(jsonPath("$.data.page").value(0))
                    .andExpect(jsonPath("$.data.size").value(10))
                    .andExpect(jsonPath("$.data.totalElements").value(15))
                    .andExpect(jsonPath("$.data.totalPages").value(2))
                    .andExpect(jsonPath("$.data.first").value(true))
                    .andExpect(jsonPath("$.data.last").value(false));

            // Request page 1 with size 10
            mockMvc.perform(get("/api/v1/account/favorites")
                            .header("Authorization", authHeader(buyerToken))
                            .param("page", "1")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(5))
                    .andExpect(jsonPath("$.data.page").value(1))
                    .andExpect(jsonPath("$.data.first").value(false))
                    .andExpect(jsonPath("$.data.last").value(true));
        }
    }

    // ==================== EDGE CASES ====================

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Can favorite DRAFT listing (service allows it)")
        void addToFavorites_draftListing_succeeds() throws Exception {
            User seller = createTestUser("seller@test.com", Role.USER);

            // DRAFT listing (not DELETED)
            Listing listing = createTestListing(seller, ListingType.VEHICLE);
            // listing is DRAFT by default

            String buyerToken = registerAndGetToken("buyer@test.com", "Password123!");

            // Should succeed since only DELETED is blocked
            mockMvc.perform(post("/api/v1/account/favorites/" + listing.getId())
                            .header("Authorization", authHeader(buyerToken)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("Can favorite SOLD listing")
        void addToFavorites_soldListing_succeeds() throws Exception {
            User seller = createTestUser("seller@test.com", Role.USER);
            State state = createTestState("Lagos");

            Listing listing = Listing.builder()
                    .title("Sold Listing")
                    .price(BigDecimal.valueOf(150000))
                    .seller(seller)
                    .listingType(ListingType.VEHICLE)
                    .vehicleType(VehicleType.MOTORCYCLE)
                    .category(ListingCategory.MOTORCYCLE)
                    .status(ListingStatus.SOLD)
                    .condition(ListingCondition.GOOD)
                    .state(state)
                    .build();
            listing = listingRepository.save(listing);

            String buyerToken = registerAndGetToken("buyer@test.com", "Password123!");

            // Should succeed since only DELETED is blocked
            mockMvc.perform(post("/api/v1/account/favorites/" + listing.getId())
                            .header("Authorization", authHeader(buyerToken)))
                    .andExpect(status().isCreated());
        }
    }
}
