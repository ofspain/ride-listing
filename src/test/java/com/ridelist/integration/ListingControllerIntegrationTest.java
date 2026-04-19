package com.ridelist.integration;

import com.ridelist.dto.request.CreateListingRequest;
import com.ridelist.dto.request.UpdateListingRequest;
import com.ridelist.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("ListingController Integration Tests")
public class ListingControllerIntegrationTest extends BaseIntegrationTest {

    // ==================== CREATE LISTING TESTS (LIST-001 to LIST-005) ====================

    @Nested
    @DisplayName("Create Listing Tests")
    class CreateListingTests {

        @Test
        @DisplayName("LIST-001: Create vehicle listing returns draft status")
        void createListing_validVehicle_returnsDraft() throws Exception {
            String token = registerAndGetToken("seller@test.com", "Password123!");
            State state = createTestState("Lagos");

            CreateListingRequest request = CreateListingRequest.builder()
                    .title("Honda CBR 650R")
                    .description("Well maintained sports bike")
                    .price(BigDecimal.valueOf(2500000))
                    .listingType(ListingType.VEHICLE)
                    .vehicleType(VehicleType.MOTORCYCLE)
                    .category(ListingCategory.MOTORCYCLE)
                    .condition(ListingCondition.GOOD)
                    .stateId(state.getId())
                    .build();

            mockMvc.perform(post("/api/v1/account/listings")
                            .header("Authorization", authHeader(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.status").value("DRAFT"))
                    .andExpect(jsonPath("$.data.listingType").value("VEHICLE"))
                    .andExpect(jsonPath("$.data.vehicleType").value("MOTORCYCLE"))
                    .andExpect(jsonPath("$.data.title").value("Honda CBR 650R"));
        }

        @Test
        @DisplayName("LIST-002: Create part listing returns draft status")
        void createListing_validPart_returnsDraft() throws Exception {
            String token = registerAndGetToken("seller@test.com", "Password123!");
            State state = createTestState("Lagos");

            CreateListingRequest request = CreateListingRequest.builder()
                    .title("Motorcycle Chain Set")
                    .description("Heavy duty chain set for motorcycles")
                    .price(BigDecimal.valueOf(15000))
                    .listingType(ListingType.PART)
                    .partName("Chain Set")
                    .category(ListingCategory.SPARE_PART)
                    .condition(ListingCondition.NEW)
                    .stateId(state.getId())
                    .build();

            mockMvc.perform(post("/api/v1/account/listings")
                            .header("Authorization", authHeader(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.listingType").value("PART"))
                    .andExpect(jsonPath("$.data.partName").value("Chain Set"))
                    .andExpect(jsonPath("$.data.status").value("DRAFT"));
        }

        @Test
        @DisplayName("LIST-003: Create vehicle without vehicleType returns 400")
        void createListing_vehicleWithoutVehicleType_returns400() throws Exception {
            String token = registerAndGetToken("seller@test.com", "Password123!");
            State state = createTestState("Lagos");

            CreateListingRequest request = CreateListingRequest.builder()
                    .title("Some Vehicle")
                    .description("Description")
                    .price(BigDecimal.valueOf(100000))
                    .listingType(ListingType.VEHICLE)
                    // Missing vehicleType
                    .category(ListingCategory.MOTORCYCLE)
                    .condition(ListingCondition.GOOD)
                    .stateId(state.getId())
                    .build();

            mockMvc.perform(post("/api/v1/account/listings")
                            .header("Authorization", authHeader(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("LIST-004: Create part without partName returns 400")
        void createListing_partWithoutPartName_returns400() throws Exception {
            String token = registerAndGetToken("seller@test.com", "Password123!");
            State state = createTestState("Lagos");

            CreateListingRequest request = CreateListingRequest.builder()
                    .title("Some Part")
                    .description("Description")
                    .price(BigDecimal.valueOf(5000))
                    .listingType(ListingType.PART)
                    // Missing partName
                    .category(ListingCategory.SPARE_PART)
                    .condition(ListingCondition.NEW)
                    .stateId(state.getId())
                    .build();

            mockMvc.perform(post("/api/v1/account/listings")
                            .header("Authorization", authHeader(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("LIST-005: Create listing without authentication returns 401")
        void createListing_noAuth_returns401() throws Exception {
            State state = createTestState("Lagos");

            CreateListingRequest request = CreateListingRequest.builder()
                    .title("Honda CBR 650R")
                    .price(BigDecimal.valueOf(2500000))
                    .listingType(ListingType.VEHICLE)
                    .vehicleType(VehicleType.MOTORCYCLE)
                    .category(ListingCategory.MOTORCYCLE)
                    .condition(ListingCondition.GOOD)
                    .stateId(state.getId())
                    .build();

            mockMvc.perform(post("/api/v1/account/listings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ==================== UPDATE LISTING TESTS (LIST-010 to LIST-013) ====================

    @Nested
    @DisplayName("Update Listing Tests")
    class UpdateListingTests {

        @Test
        @DisplayName("LIST-010: Update own listing succeeds")
        void updateListing_owner_returnsUpdated() throws Exception {
            String token = registerAndGetToken("seller@test.com", "Password123!");
            User seller = userRepository.findByEmail("seller@test.com").orElseThrow();
            Listing listing = createTestListing(seller, ListingType.VEHICLE);

            UpdateListingRequest request = UpdateListingRequest.builder()
                    .title("Updated Title")
                    .price(BigDecimal.valueOf(200000))
                    .build();

            mockMvc.perform(put("/api/v1/account/listings/" + listing.getId())
                            .header("Authorization", authHeader(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.title").value("Updated Title"))
                    .andExpect(jsonPath("$.data.price").value(200000));
        }

        @Test
        @DisplayName("LIST-011: Update another user's listing returns 401 Unauthorized")
        void updateListing_notOwner_returns401() throws Exception {
            // Create listing as seller1
            User seller1 = createTestUser("seller1@test.com", Role.USER);
            Listing listing = createTestListing(seller1, ListingType.VEHICLE);

            // Try to update as seller2
            String seller2Token = registerAndGetToken("seller2@test.com", "Password123!");

            UpdateListingRequest request = UpdateListingRequest.builder()
                    .title("Hijacked Title")
                    .build();

            // Note: Service returns 401 Unauthorized when non-owner tries to modify listing
            mockMvc.perform(put("/api/v1/account/listings/" + listing.getId())
                            .header("Authorization", authHeader(seller2Token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("LIST-012: Update non-existent listing returns 404")
        void updateListing_nonExistent_returns404() throws Exception {
            String token = registerAndGetToken("seller@test.com", "Password123!");

            UpdateListingRequest request = UpdateListingRequest.builder()
                    .title("Some Title")
                    .build();

            mockMvc.perform(put("/api/v1/account/listings/" + UUID.randomUUID())
                            .header("Authorization", authHeader(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("LIST-013: Update with invalid price returns 400")
        void updateListing_invalidPrice_returns400() throws Exception {
            String token = registerAndGetToken("seller@test.com", "Password123!");
            User seller = userRepository.findByEmail("seller@test.com").orElseThrow();
            Listing listing = createTestListing(seller, ListingType.VEHICLE);

            UpdateListingRequest request = UpdateListingRequest.builder()
                    .price(BigDecimal.valueOf(-100))
                    .build();

            mockMvc.perform(put("/api/v1/account/listings/" + listing.getId())
                            .header("Authorization", authHeader(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ==================== PUBLISH LISTING TESTS (LIST-020 to LIST-024) ====================

    @Nested
    @DisplayName("Publish Listing Tests")
    class PublishListingTests {

        @Test
        @DisplayName("LIST-020: Publish valid draft becomes active")
        void publishListing_validDraft_becomesActive() throws Exception {
            String token = registerAndGetToken("seller@test.com", "Password123!");
            User seller = userRepository.findByEmail("seller@test.com").orElseThrow();
            State state = createTestState("Lagos");

            Listing listing = Listing.builder()
                    .title("Complete Listing")
                    .description("Full description")
                    .price(BigDecimal.valueOf(150000))
                    .seller(seller)
                    .listingType(ListingType.VEHICLE)
                    .vehicleType(VehicleType.MOTORCYCLE)
                    .category(ListingCategory.MOTORCYCLE)
                    .status(ListingStatus.DRAFT)
                    .condition(ListingCondition.GOOD)
                    .state(state)
                    .build();
            listing = listingRepository.save(listing);

            mockMvc.perform(post("/api/v1/account/listings/" + listing.getId() + "/publish")
                            .header("Authorization", authHeader(token)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.status").value("ACTIVE"));
        }

        @Test
        @DisplayName("LIST-023: Publish listing without state returns 400")
        void publishListing_missingState_returns400() throws Exception {
            String token = registerAndGetToken("seller@test.com", "Password123!");
            User seller = userRepository.findByEmail("seller@test.com").orElseThrow();

            Listing listing = Listing.builder()
                    .title("Incomplete Listing")
                    .description("Some description")
                    .price(BigDecimal.valueOf(150000))
                    .seller(seller)
                    .listingType(ListingType.VEHICLE)
                    .vehicleType(VehicleType.MOTORCYCLE)
                    .category(ListingCategory.MOTORCYCLE)
                    .status(ListingStatus.DRAFT)
                    .condition(ListingCondition.GOOD)
                    // Missing state
                    .build();
            listing = listingRepository.save(listing);

            mockMvc.perform(post("/api/v1/account/listings/" + listing.getId() + "/publish")
                            .header("Authorization", authHeader(token)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("LIST-024: Publish already active listing returns 400")
        void publishListing_alreadyActive_returns400() throws Exception {
            String token = registerAndGetToken("seller@test.com", "Password123!");
            User seller = userRepository.findByEmail("seller@test.com").orElseThrow();
            State state = createTestState("Lagos");

            Listing listing = Listing.builder()
                    .title("Active Listing")
                    .description("Description")
                    .price(BigDecimal.valueOf(150000))
                    .seller(seller)
                    .listingType(ListingType.VEHICLE)
                    .vehicleType(VehicleType.MOTORCYCLE)
                    .category(ListingCategory.MOTORCYCLE)
                    .status(ListingStatus.ACTIVE) // Already active
                    .condition(ListingCondition.GOOD)
                    .state(state)
                    .build();
            listing = listingRepository.save(listing);

            mockMvc.perform(post("/api/v1/account/listings/" + listing.getId() + "/publish")
                            .header("Authorization", authHeader(token)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ==================== MARK AS SOLD TESTS (LIST-030 to LIST-032) ====================

    @Nested
    @DisplayName("Mark as Sold Tests")
    class MarkAsSoldTests {

        @Test
        @DisplayName("LIST-030: Mark active listing as sold succeeds")
        void markAsSold_activeListing_becomesSold() throws Exception {
            String token = registerAndGetToken("seller@test.com", "Password123!");
            User seller = userRepository.findByEmail("seller@test.com").orElseThrow();
            State state = createTestState("Lagos");

            Listing listing = Listing.builder()
                    .title("Active Listing")
                    .description("Description")
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

            mockMvc.perform(post("/api/v1/account/listings/" + listing.getId() + "/mark-sold")
                            .header("Authorization", authHeader(token)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.status").value("SOLD"));
        }

        @Test
        @DisplayName("LIST-031: Mark draft as sold returns 400")
        void markAsSold_draftListing_returns400() throws Exception {
            String token = registerAndGetToken("seller@test.com", "Password123!");
            User seller = userRepository.findByEmail("seller@test.com").orElseThrow();

            Listing listing = createTestListing(seller, ListingType.VEHICLE);
            // listing is DRAFT by default from createTestListing

            mockMvc.perform(post("/api/v1/account/listings/" + listing.getId() + "/mark-sold")
                            .header("Authorization", authHeader(token)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("LIST-032: Mark another user's listing as sold returns 401 Unauthorized")
        void markAsSold_notOwner_returns401() throws Exception {
            User seller1 = createTestUser("seller1@test.com", Role.USER);
            State state = createTestState("Lagos");

            Listing listing = Listing.builder()
                    .title("Active Listing")
                    .price(BigDecimal.valueOf(150000))
                    .seller(seller1)
                    .listingType(ListingType.VEHICLE)
                    .vehicleType(VehicleType.MOTORCYCLE)
                    .category(ListingCategory.MOTORCYCLE)
                    .status(ListingStatus.ACTIVE)
                    .condition(ListingCondition.GOOD)
                    .state(state)
                    .build();
            listing = listingRepository.save(listing);

            String seller2Token = registerAndGetToken("seller2@test.com", "Password123!");

            // Note: Service returns 401 Unauthorized when non-owner tries to modify listing
            mockMvc.perform(post("/api/v1/account/listings/" + listing.getId() + "/mark-sold")
                            .header("Authorization", authHeader(seller2Token)))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ==================== SEARCH & FILTER TESTS (SRCH-001 to SRCH-010) ====================

    @Nested
    @DisplayName("Search and Filter Tests")
    class SearchAndFilterTests {

        @Test
        @DisplayName("SRCH-001: Filter by listing type returns only matching type")
        void getListings_filterByListingType_returnsOnlyVehicles() throws Exception {
            User seller = createTestUser("seller@test.com", Role.USER);
            State state = createTestState("Lagos");

            Listing vehicle = createTestListing(seller, ListingType.VEHICLE);
            vehicle.setStatus(ListingStatus.ACTIVE);
            vehicle.setState(state);

            Listing part = createTestListing(seller, ListingType.PART);
            part.setStatus(ListingStatus.ACTIVE);
            part.setState(state);

            listingRepository.saveAll(List.of(vehicle, part));

            mockMvc.perform(get("/api/v1/listings")
                            .param("listingType", "VEHICLE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].listingType").value("VEHICLE"));
        }

        @Test
        @DisplayName("SRCH-002: Filter by vehicle type returns only matching vehicles")
        void getListings_filterByVehicleType_returnsOnlyMotorcycles() throws Exception {
            User seller = createTestUser("seller@test.com", Role.USER);
            State state = createTestState("Lagos");

            Listing motorcycle = Listing.builder()
                    .title("Motorcycle")
                    .price(BigDecimal.valueOf(100000))
                    .seller(seller)
                    .listingType(ListingType.VEHICLE)
                    .vehicleType(VehicleType.MOTORCYCLE)
                    .category(ListingCategory.MOTORCYCLE)
                    .status(ListingStatus.ACTIVE)
                    .condition(ListingCondition.GOOD)
                    .state(state)
                    .build();

            Listing tricycle = Listing.builder()
                    .title("Tricycle")
                    .price(BigDecimal.valueOf(200000))
                    .seller(seller)
                    .listingType(ListingType.VEHICLE)
                    .vehicleType(VehicleType.TRICYCLE)
                    .category(ListingCategory.TRICYCLE)
                    .status(ListingStatus.ACTIVE)
                    .condition(ListingCondition.GOOD)
                    .state(state)
                    .build();

            listingRepository.saveAll(List.of(motorcycle, tricycle));

            mockMvc.perform(get("/api/v1/listings")
                            .param("vehicleType", "MOTORCYCLE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].vehicleType").value("MOTORCYCLE"));
        }

        @Test
        @DisplayName("SRCH-003: Filter by price range returns matching listings")
        void getListings_filterByPriceRange_returnsMatchingListings() throws Exception {
            User seller = createTestUser("seller@test.com", Role.USER);
            State state = createTestState("Lagos");

            Listing cheap = createTestListing(seller, ListingType.VEHICLE);
            cheap.setPrice(BigDecimal.valueOf(50000));
            cheap.setStatus(ListingStatus.ACTIVE);
            cheap.setState(state);

            Listing mid = createTestListing(seller, ListingType.VEHICLE);
            mid.setPrice(BigDecimal.valueOf(100000));
            mid.setStatus(ListingStatus.ACTIVE);
            mid.setState(state);

            Listing expensive = createTestListing(seller, ListingType.VEHICLE);
            expensive.setPrice(BigDecimal.valueOf(500000));
            expensive.setStatus(ListingStatus.ACTIVE);
            expensive.setState(state);

            listingRepository.saveAll(List.of(cheap, mid, expensive));

            mockMvc.perform(get("/api/v1/listings")
                            .param("minPrice", "40000")
                            .param("maxPrice", "150000"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(2));
        }

        @Test
        @DisplayName("SRCH-004: Filter by state returns listings in that state")
        void getListings_filterByState_returnsListingsInState() throws Exception {
            User seller = createTestUser("seller@test.com", Role.USER);
            State lagos = createTestState("Lagos");
            State abuja = createTestState("Abuja");

            Listing lagosListing = createTestListing(seller, ListingType.VEHICLE);
            lagosListing.setState(lagos);
            lagosListing.setStatus(ListingStatus.ACTIVE);

            Listing abujaListing = createTestListing(seller, ListingType.VEHICLE);
            abujaListing.setState(abuja);
            abujaListing.setStatus(ListingStatus.ACTIVE);

            listingRepository.saveAll(List.of(lagosListing, abujaListing));

            mockMvc.perform(get("/api/v1/listings")
                            .param("stateId", lagos.getId().toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1));
        }

        @Test
        @DisplayName("SRCH-008: Combined filters return intersection")
        void getListings_combinedFilters_returnsIntersection() throws Exception {
            User seller = createTestUser("seller@test.com", Role.USER);
            State lagos = createTestState("Lagos");

            // Vehicle in Lagos, cheap - should match
            Listing match = Listing.builder()
                    .title("Match")
                    .price(BigDecimal.valueOf(100000))
                    .seller(seller)
                    .listingType(ListingType.VEHICLE)
                    .vehicleType(VehicleType.MOTORCYCLE)
                    .category(ListingCategory.MOTORCYCLE)
                    .status(ListingStatus.ACTIVE)
                    .condition(ListingCondition.GOOD)
                    .state(lagos)
                    .build();

            // Vehicle in Lagos, expensive - no match
            Listing noMatch1 = Listing.builder()
                    .title("No Match 1")
                    .price(BigDecimal.valueOf(500000))
                    .seller(seller)
                    .listingType(ListingType.VEHICLE)
                    .vehicleType(VehicleType.MOTORCYCLE)
                    .category(ListingCategory.MOTORCYCLE)
                    .status(ListingStatus.ACTIVE)
                    .condition(ListingCondition.GOOD)
                    .state(lagos)
                    .build();

            // Part in Lagos, cheap - no match (wrong type)
            Listing noMatch2 = Listing.builder()
                    .title("No Match 2")
                    .price(BigDecimal.valueOf(100000))
                    .seller(seller)
                    .listingType(ListingType.PART)
                    .partName("Part")
                    .category(ListingCategory.SPARE_PART)
                    .status(ListingStatus.ACTIVE)
                    .condition(ListingCondition.GOOD)
                    .state(lagos)
                    .build();

            listingRepository.saveAll(List.of(match, noMatch1, noMatch2));

            mockMvc.perform(get("/api/v1/listings")
                            .param("listingType", "VEHICLE")
                            .param("stateId", lagos.getId().toString())
                            .param("maxPrice", "200000"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].title").value("Match"));
        }

        @Test
        @DisplayName("SRCH-009: No matches returns empty list")
        void getListings_noMatches_returnsEmptyList() throws Exception {
            mockMvc.perform(get("/api/v1/listings")
                            .param("minPrice", "999999999"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content.length()").value(0));
        }

        @Test
        @DisplayName("SRCH-010: Pagination returns correct page")
        void getListings_pagination_returnsCorrectPage() throws Exception {
            User seller = createTestUser("seller@test.com", Role.USER);
            State state = createTestState("Lagos");

            // Create 25 listings
            for (int i = 0; i < 25; i++) {
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
            }

            // Request page 1 (second page) with size 10
            mockMvc.perform(get("/api/v1/listings")
                            .param("page", "1")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(10))
                    .andExpect(jsonPath("$.data.page").value(1))
                    .andExpect(jsonPath("$.data.size").value(10))
                    .andExpect(jsonPath("$.data.totalElements").value(25))
                    .andExpect(jsonPath("$.data.totalPages").value(3));
        }
    }

    // ==================== GET LISTING TESTS ====================

    @Nested
    @DisplayName("Get Listing Tests")
    class GetListingTests {

        @Test
        @DisplayName("Get listing by ID returns listing details")
        void getListingById_validId_returnsListing() throws Exception {
            User seller = createTestUser("seller@test.com", Role.USER);
            State state = createTestState("Lagos");

            Listing listing = Listing.builder()
                    .title("Test Listing")
                    .description("Test Description")
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

            mockMvc.perform(get("/api/v1/listings/" + listing.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(listing.getId().toString()))
                    .andExpect(jsonPath("$.data.title").value("Test Listing"))
                    .andExpect(jsonPath("$.data.seller").exists());
        }

        @Test
        @DisplayName("Get non-existent listing returns 404")
        void getListingById_nonExistent_returns404() throws Exception {
            mockMvc.perform(get("/api/v1/listings/" + UUID.randomUUID()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Get my listings requires authentication")
        void getMyListings_noAuth_returns401() throws Exception {
            mockMvc.perform(get("/api/v1/account/listings"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Get my listings returns only own listings")
        void getMyListings_authenticated_returnsOwnListings() throws Exception {
            String token = registerAndGetToken("seller@test.com", "Password123!");
            User seller = userRepository.findByEmail("seller@test.com").orElseThrow();

            User otherSeller = createTestUser("other@test.com", Role.USER);

            Listing myListing = createTestListing(seller, ListingType.VEHICLE);
            Listing otherListing = createTestListing(otherSeller, ListingType.VEHICLE);

            mockMvc.perform(get("/api/v1/account/listings")
                            .header("Authorization", authHeader(token)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].id").value(myListing.getId().toString()));
        }
    }
}
