package com.ridelist.controller;

import com.ridelist.BaseIntegrationTest;
import com.ridelist.dto.request.CreateListingRequest;
import com.ridelist.dto.request.UpdateListingRequest;
import com.ridelist.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("ListingController Integration Tests")
class ListingControllerIntegrationTest extends BaseIntegrationTest {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Nested
    @DisplayName("GET /api/v1/listings - Public Endpoints")
    class GetListingsTests {

        @Test
        @DisplayName("Should get all active listings without authentication")
        void shouldGetAllActiveListings() throws Exception {
            User seller = createTestUser("seller@test.com", passwordEncoder.encode("password123"));
            createTestListing(seller, ListingStatus.ACTIVE);
            createTestListing(seller, ListingStatus.ACTIVE);
            createTestListing(seller, ListingStatus.DRAFT); // Should not appear

            mockMvc.perform(get("/api/v1/listings"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content", hasSize(2)))
                    .andExpect(jsonPath("$.data.totalElements").value(2));
        }

        @Test
        @DisplayName("Should filter listings by listingType")
        void shouldFilterByListingType() throws Exception {
            User seller = createTestUser("seller@test.com", passwordEncoder.encode("password123"));
            createTestListing(seller, ListingStatus.ACTIVE); // VEHICLE
            createTestPartListing(seller, ListingStatus.ACTIVE); // PART

            mockMvc.perform(get("/api/v1/listings")
                            .param("listingType", "VEHICLE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].listingType").value("VEHICLE"));
        }

        @Test
        @DisplayName("Should filter listings by vehicleType")
        void shouldFilterByVehicleType() throws Exception {
            User seller = createTestUser("seller@test.com", passwordEncoder.encode("password123"));
            createTestListing(seller, ListingStatus.ACTIVE); // MOTORCYCLE

            mockMvc.perform(get("/api/v1/listings")
                            .param("vehicleType", "MOTORCYCLE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].vehicleType").value("MOTORCYCLE"));
        }

        @Test
        @DisplayName("Should filter listings by state")
        void shouldFilterByState() throws Exception {
            User seller = createTestUser("seller@test.com", passwordEncoder.encode("password123"));
            createTestListing(seller, ListingStatus.ACTIVE); // Lagos

            mockMvc.perform(get("/api/v1/listings")
                            .param("state", "Lagos"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].state").value("Lagos"));

            mockMvc.perform(get("/api/v1/listings")
                            .param("state", "Abuja"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(0)));
        }

        @Test
        @DisplayName("Should filter listings by price range")
        void shouldFilterByPriceRange() throws Exception {
            User seller = createTestUser("seller@test.com", passwordEncoder.encode("password123"));
            createTestListing(seller, ListingStatus.ACTIVE); // 500000

            mockMvc.perform(get("/api/v1/listings")
                            .param("minPrice", "400000")
                            .param("maxPrice", "600000"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(1)));

            mockMvc.perform(get("/api/v1/listings")
                            .param("minPrice", "600000"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(0)));
        }

        @Test
        @DisplayName("Should paginate listings")
        void shouldPaginateListings() throws Exception {
            User seller = createTestUser("seller@test.com", passwordEncoder.encode("password123"));
            for (int i = 0; i < 25; i++) {
                createTestListing(seller, ListingStatus.ACTIVE);
            }

            mockMvc.perform(get("/api/v1/listings")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(10)))
                    .andExpect(jsonPath("$.data.totalElements").value(25))
                    .andExpect(jsonPath("$.data.totalPages").value(3))
                    .andExpect(jsonPath("$.data.first").value(true))
                    .andExpect(jsonPath("$.data.last").value(false));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/listings/{id}")
    class GetListingByIdTests {

        @Test
        @DisplayName("Should get listing by ID without authentication")
        void shouldGetListingById() throws Exception {
            User seller = createTestUser("seller@test.com", passwordEncoder.encode("password123"));
            Listing listing = createTestListing(seller, ListingStatus.ACTIVE);

            mockMvc.perform(get("/api/v1/listings/{id}", listing.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(listing.getId().toString()))
                    .andExpect(jsonPath("$.data.title").value("Test Motorcycle"))
                    .andExpect(jsonPath("$.data.listingType").value("VEHICLE"))
                    .andExpect(jsonPath("$.data.vehicleType").value("MOTORCYCLE"));
        }

        @Test
        @DisplayName("Should return 404 for non-existent listing")
        void shouldReturn404ForNonExistent() throws Exception {
            mockMvc.perform(get("/api/v1/listings/{id}", "00000000-0000-0000-0000-000000000000"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Should return 404 for deleted listing")
        void shouldReturn404ForDeletedListing() throws Exception {
            User seller = createTestUser("seller@test.com", passwordEncoder.encode("password123"));
            Listing listing = createTestListing(seller, ListingStatus.DELETED);

            mockMvc.perform(get("/api/v1/listings/{id}", listing.getId()))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/account/listings - Create Listing")
    class CreateListingTests {

        @Test
        @DisplayName("Should create vehicle listing successfully")
        void shouldCreateVehicleListing() throws Exception {
            String token = registerAndGetToken("seller@test.com", "password123");

            CreateListingRequest request = CreateListingRequest.builder()
                    .listingType(ListingType.VEHICLE)
                    .vehicleType(VehicleType.MOTORCYCLE)
                    .title("Honda CBR 600RR")
                    .description("Well maintained motorcycle")
                    .price(BigDecimal.valueOf(750000))
                    .state("Lagos")
                    .category(ListingCategory.MOTORCYCLE)
                    .condition(ListingCondition.GOOD)
                    .make("Honda")
                    .model("CBR 600RR")
                    .year(2021)
                    .location("Victoria Island, Lagos")
                    .build();

            mockMvc.perform(post("/api/v1/account/listings")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Listing created successfully"))
                    .andExpect(jsonPath("$.data.title").value("Honda CBR 600RR"))
                    .andExpect(jsonPath("$.data.status").value("DRAFT"))
                    .andExpect(jsonPath("$.data.listingType").value("VEHICLE"))
                    .andExpect(jsonPath("$.data.vehicleType").value("MOTORCYCLE"));
        }

        @Test
        @DisplayName("Should create part listing successfully")
        void shouldCreatePartListing() throws Exception {
            String token = registerAndGetToken("seller@test.com", "password123");

            CreateListingRequest request = CreateListingRequest.builder()
                    .listingType(ListingType.PART)
                    .title("Brake Pads Set")
                    .description("High quality brake pads")
                    .price(BigDecimal.valueOf(15000))
                    .state("Lagos")
                    .category(ListingCategory.SPARE_PART)
                    .condition(ListingCondition.NEW)
                    .partName("Brake Pads")
                    .partCategory("Brakes")
                    .compatibility("Honda CBR 2020-2023")
                    .location("Ikeja, Lagos")
                    .build();

            mockMvc.perform(post("/api/v1/account/listings")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.listingType").value("PART"))
                    .andExpect(jsonPath("$.data.partName").value("Brake Pads"));
        }

        @Test
        @DisplayName("Should fail without authentication")
        void shouldFailWithoutAuth() throws Exception {
            CreateListingRequest request = CreateListingRequest.builder()
                    .listingType(ListingType.VEHICLE)
                    .title("Test")
                    .price(BigDecimal.valueOf(100000))
                    .state("Lagos")
                    .category(ListingCategory.MOTORCYCLE)
                    .condition(ListingCondition.GOOD)
                    .build();

            mockMvc.perform(post("/api/v1/account/listings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should fail when vehicle listing missing vehicleType")
        void shouldFailWhenVehicleMissingVehicleType() throws Exception {
            String token = registerAndGetToken("seller@test.com", "password123");

            CreateListingRequest request = CreateListingRequest.builder()
                    .listingType(ListingType.VEHICLE)
                    .title("Test Motorcycle")
                    .price(BigDecimal.valueOf(500000))
                    .state("Lagos")
                    .category(ListingCategory.MOTORCYCLE)
                    .condition(ListingCondition.GOOD)
                    .build();

            mockMvc.perform(post("/api/v1/account/listings")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Vehicle type is required for vehicle listings"));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/account/listings/{id} - Update Listing")
    class UpdateListingTests {

        @Test
        @DisplayName("Should update listing successfully")
        void shouldUpdateListing() throws Exception {
            String token = registerAndGetToken("seller@test.com", "password123");
            User seller = userRepository.findByEmail("seller@test.com").orElseThrow();
            Listing listing = createTestListing(seller, ListingStatus.DRAFT);

            UpdateListingRequest request = UpdateListingRequest.builder()
                    .title("Updated Motorcycle Title")
                    .price(BigDecimal.valueOf(600000))
                    .build();

            mockMvc.perform(put("/api/v1/account/listings/{id}", listing.getId())
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.title").value("Updated Motorcycle Title"))
                    .andExpect(jsonPath("$.data.price").value(600000));
        }

        @Test
        @DisplayName("Should fail to update another user's listing")
        void shouldFailToUpdateOthersListing() throws Exception {
            // Create listing with one user
            User seller = createTestUser("seller@test.com", passwordEncoder.encode("password123"));
            Listing listing = createTestListing(seller, ListingStatus.DRAFT);

            // Try to update with another user
            String token = registerAndGetToken("another@test.com", "password123");

            UpdateListingRequest request = UpdateListingRequest.builder()
                    .title("Hacked Title")
                    .build();

            mockMvc.perform(put("/api/v1/account/listings/{id}", listing.getId())
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should fail to update sold listing")
        void shouldFailToUpdateSoldListing() throws Exception {
            String token = registerAndGetToken("seller@test.com", "password123");
            User seller = userRepository.findByEmail("seller@test.com").orElseThrow();
            Listing listing = createTestListing(seller, ListingStatus.SOLD);

            UpdateListingRequest request = UpdateListingRequest.builder()
                    .title("Updated Title")
                    .build();

            mockMvc.perform(put("/api/v1/account/listings/{id}", listing.getId())
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Cannot update a listing that is sold or deleted"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/account/listings/{id}/publish")
    class PublishListingTests {

        @Test
        @DisplayName("Should publish draft listing successfully")
        void shouldPublishListing() throws Exception {
            String token = registerAndGetToken("seller@test.com", "password123");
            User seller = userRepository.findByEmail("seller@test.com").orElseThrow();
            Listing listing = createTestListing(seller, ListingStatus.DRAFT);

            mockMvc.perform(post("/api/v1/account/listings/{id}/publish", listing.getId())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Listing published successfully"))
                    .andExpect(jsonPath("$.data.status").value("ACTIVE"));
        }

        @Test
        @DisplayName("Should fail to publish already active listing")
        void shouldFailToPublishActiveListing() throws Exception {
            String token = registerAndGetToken("seller@test.com", "password123");
            User seller = userRepository.findByEmail("seller@test.com").orElseThrow();
            Listing listing = createTestListing(seller, ListingStatus.ACTIVE);

            mockMvc.perform(post("/api/v1/account/listings/{id}/publish", listing.getId())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Only draft listings can be published"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/account/listings/{id}/mark-sold")
    class MarkAsSoldTests {

        @Test
        @DisplayName("Should mark active listing as sold")
        void shouldMarkAsSold() throws Exception {
            String token = registerAndGetToken("seller@test.com", "password123");
            User seller = userRepository.findByEmail("seller@test.com").orElseThrow();
            Listing listing = createTestListing(seller, ListingStatus.ACTIVE);

            mockMvc.perform(post("/api/v1/account/listings/{id}/mark-sold", listing.getId())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Listing marked as sold"))
                    .andExpect(jsonPath("$.data.status").value("SOLD"));
        }

        @Test
        @DisplayName("Should fail to mark draft listing as sold")
        void shouldFailToMarkDraftAsSold() throws Exception {
            String token = registerAndGetToken("seller@test.com", "password123");
            User seller = userRepository.findByEmail("seller@test.com").orElseThrow();
            Listing listing = createTestListing(seller, ListingStatus.DRAFT);

            mockMvc.perform(post("/api/v1/account/listings/{id}/mark-sold", listing.getId())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Only active listings can be marked as sold"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/account/listings - My Listings")
    class GetMyListingsTests {

        @Test
        @DisplayName("Should get current user's listings")
        void shouldGetMyListings() throws Exception {
            String token = registerAndGetToken("seller@test.com", "password123");
            User seller = userRepository.findByEmail("seller@test.com").orElseThrow();
            createTestListing(seller, ListingStatus.DRAFT);
            createTestListing(seller, ListingStatus.ACTIVE);
            createTestListing(seller, ListingStatus.SOLD);

            // Create another user's listing
            User otherSeller = createTestUser("other@test.com", passwordEncoder.encode("password123"));
            createTestListing(otherSeller, ListingStatus.ACTIVE);

            mockMvc.perform(get("/api/v1/account/listings")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content", hasSize(3)))
                    .andExpect(jsonPath("$.data.totalElements").value(3));
        }

        @Test
        @DisplayName("Should fail without authentication")
        void shouldFailWithoutAuth() throws Exception {
            mockMvc.perform(get("/api/v1/account/listings"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
