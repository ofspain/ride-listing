package com.ridelist.controller;

import com.ridelist.BaseIntegrationTest;
import com.ridelist.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("FavoriteController Integration Tests")
class FavoriteControllerIntegrationTest extends BaseIntegrationTest {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Nested
    @DisplayName("POST /api/v1/account/favorites/{listingId}")
    class AddToFavoritesTests {

        @Test
        @DisplayName("Should add listing to favorites successfully")
        void shouldAddToFavorites() throws Exception {
            // Create seller and listing
            User seller = createTestUser("seller@test.com", passwordEncoder.encode("password123"));
            Listing listing = createTestListing(seller, ListingStatus.ACTIVE);

            // Create buyer and get token
            String buyerToken = registerAndGetToken("buyer@test.com", "password123");

            mockMvc.perform(post("/api/v1/account/favorites/{listingId}", listing.getId())
                            .header("Authorization", "Bearer " + buyerToken))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Added to favorites"));
        }

        @Test
        @DisplayName("Should fail to favorite own listing")
        void shouldFailToFavoriteOwnListing() throws Exception {
            String sellerToken = registerAndGetToken("seller@test.com", "password123");
            User seller = userRepository.findByEmail("seller@test.com").orElseThrow();
            Listing listing = createTestListing(seller, ListingStatus.ACTIVE);

            mockMvc.perform(post("/api/v1/account/favorites/{listingId}", listing.getId())
                            .header("Authorization", "Bearer " + sellerToken))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Cannot favorite your own listing"));
        }

        @Test
        @DisplayName("Should fail to favorite deleted listing")
        void shouldFailToFavoriteDeletedListing() throws Exception {
            User seller = createTestUser("seller@test.com", passwordEncoder.encode("password123"));
            Listing listing = createTestListing(seller, ListingStatus.DELETED);

            String buyerToken = registerAndGetToken("buyer@test.com", "password123");

            mockMvc.perform(post("/api/v1/account/favorites/{listingId}", listing.getId())
                            .header("Authorization", "Bearer " + buyerToken))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Cannot favorite a deleted listing"));
        }

        @Test
        @DisplayName("Should fail to favorite same listing twice")
        void shouldFailToFavoriteTwice() throws Exception {
            User seller = createTestUser("seller@test.com", passwordEncoder.encode("password123"));
            Listing listing = createTestListing(seller, ListingStatus.ACTIVE);

            String buyerToken = registerAndGetToken("buyer@test.com", "password123");

            // First favorite
            mockMvc.perform(post("/api/v1/account/favorites/{listingId}", listing.getId())
                            .header("Authorization", "Bearer " + buyerToken))
                    .andExpect(status().isCreated());

            // Second favorite (should fail)
            mockMvc.perform(post("/api/v1/account/favorites/{listingId}", listing.getId())
                            .header("Authorization", "Bearer " + buyerToken))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Listing is already in favorites"));
        }

        @Test
        @DisplayName("Should fail without authentication")
        void shouldFailWithoutAuth() throws Exception {
            User seller = createTestUser("seller@test.com", passwordEncoder.encode("password123"));
            Listing listing = createTestListing(seller, ListingStatus.ACTIVE);

            mockMvc.perform(post("/api/v1/account/favorites/{listingId}", listing.getId()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should fail for non-existent listing")
        void shouldFailForNonExistentListing() throws Exception {
            String buyerToken = registerAndGetToken("buyer@test.com", "password123");

            mockMvc.perform(post("/api/v1/account/favorites/{listingId}", "00000000-0000-0000-0000-000000000000")
                            .header("Authorization", "Bearer " + buyerToken))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/account/favorites/{listingId}")
    class RemoveFromFavoritesTests {

        @Test
        @DisplayName("Should remove listing from favorites successfully")
        void shouldRemoveFromFavorites() throws Exception {
            User seller = createTestUser("seller@test.com", passwordEncoder.encode("password123"));
            Listing listing = createTestListing(seller, ListingStatus.ACTIVE);

            String buyerToken = registerAndGetToken("buyer@test.com", "password123");
            User buyer = userRepository.findByEmail("buyer@test.com").orElseThrow();

            // Add to favorites first
            Favorite favorite = Favorite.builder()
                    .user(buyer)
                    .listing(listing)
                    .build();
            favoriteRepository.save(favorite);

            mockMvc.perform(delete("/api/v1/account/favorites/{listingId}", listing.getId())
                            .header("Authorization", "Bearer " + buyerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Removed from favorites"));
        }

        @Test
        @DisplayName("Should fail to remove non-favorited listing")
        void shouldFailToRemoveNonFavorited() throws Exception {
            User seller = createTestUser("seller@test.com", passwordEncoder.encode("password123"));
            Listing listing = createTestListing(seller, ListingStatus.ACTIVE);

            String buyerToken = registerAndGetToken("buyer@test.com", "password123");

            mockMvc.perform(delete("/api/v1/account/favorites/{listingId}", listing.getId())
                            .header("Authorization", "Bearer " + buyerToken))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should fail without authentication")
        void shouldFailWithoutAuth() throws Exception {
            User seller = createTestUser("seller@test.com", passwordEncoder.encode("password123"));
            Listing listing = createTestListing(seller, ListingStatus.ACTIVE);

            mockMvc.perform(delete("/api/v1/account/favorites/{listingId}", listing.getId()))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/account/favorites")
    class GetFavoritesTests {

        @Test
        @DisplayName("Should get user favorites successfully")
        void shouldGetFavorites() throws Exception {
            User seller = createTestUser("seller@test.com", passwordEncoder.encode("password123"));
            Listing listing1 = createTestListing(seller, ListingStatus.ACTIVE);
            Listing listing2 = createTestListing(seller, ListingStatus.ACTIVE);

            String buyerToken = registerAndGetToken("buyer@test.com", "password123");
            User buyer = userRepository.findByEmail("buyer@test.com").orElseThrow();

            // Add favorites
            favoriteRepository.save(Favorite.builder().user(buyer).listing(listing1).build());
            favoriteRepository.save(Favorite.builder().user(buyer).listing(listing2).build());

            mockMvc.perform(get("/api/v1/account/favorites")
                            .header("Authorization", "Bearer " + buyerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content", hasSize(2)))
                    .andExpect(jsonPath("$.data.totalElements").value(2));
        }

        @Test
        @DisplayName("Should return empty list when no favorites")
        void shouldReturnEmptyList() throws Exception {
            String buyerToken = registerAndGetToken("buyer@test.com", "password123");

            mockMvc.perform(get("/api/v1/account/favorites")
                            .header("Authorization", "Bearer " + buyerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content", hasSize(0)))
                    .andExpect(jsonPath("$.data.totalElements").value(0));
        }

        @Test
        @DisplayName("Should paginate favorites")
        void shouldPaginateFavorites() throws Exception {
            User seller = createTestUser("seller@test.com", passwordEncoder.encode("password123"));

            String buyerToken = registerAndGetToken("buyer@test.com", "password123");
            User buyer = userRepository.findByEmail("buyer@test.com").orElseThrow();

            // Create 15 favorites
            for (int i = 0; i < 15; i++) {
                Listing listing = createTestListing(seller, ListingStatus.ACTIVE);
                favoriteRepository.save(Favorite.builder().user(buyer).listing(listing).build());
            }

            mockMvc.perform(get("/api/v1/account/favorites")
                            .header("Authorization", "Bearer " + buyerToken)
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(10)))
                    .andExpect(jsonPath("$.data.totalElements").value(15))
                    .andExpect(jsonPath("$.data.totalPages").value(2));
        }

        @Test
        @DisplayName("Should fail without authentication")
        void shouldFailWithoutAuth() throws Exception {
            mockMvc.perform(get("/api/v1/account/favorites"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should not show other user's favorites")
        void shouldNotShowOtherUsersFavorites() throws Exception {
            User seller = createTestUser("seller@test.com", passwordEncoder.encode("password123"));
            Listing listing = createTestListing(seller, ListingStatus.ACTIVE);

            // User1 adds a favorite
            User user1 = createTestUser("user1@test.com", passwordEncoder.encode("password123"));
            favoriteRepository.save(Favorite.builder().user(user1).listing(listing).build());

            // User2 checks their favorites (should be empty)
            String user2Token = registerAndGetToken("user2@test.com", "password123");

            mockMvc.perform(get("/api/v1/account/favorites")
                            .header("Authorization", "Bearer " + user2Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(0)));
        }
    }
}
