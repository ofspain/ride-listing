package com.ridelist.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.ridelist.dto.response.LocationResolution;
import com.ridelist.model.*;
import com.ridelist.repository.*;
import com.ridelist.service.LocationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SeoSlugIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private LocationService locationService;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private StateRepository stateRepository;

    @Autowired
    private AxisRepository axisRepository;

    @Autowired
    private AreaRepository areaRepository;

    @Nested
    @DisplayName("Create Listing - Slug Generation")
    class CreateListingSlugTests {

        @Test
        @DisplayName("should generate slug and listing_number on create")
        void createListing_generatesSlugAndListingNumber() throws Exception {
            String token = registerAndGetToken("seller@test.com", "password123");
            State state = createTestState("Lagos");

            String createRequest = """
                {
                    "listingType": "VEHICLE",
                    "vehicleType": "MOTORCYCLE",
                    "title": "Honda CB300R Tokunbo 2023",
                    "description": "Clean motorcycle",
                    "price": 500000,
                    "category": "MOTORCYCLE",
                    "condition": "GOOD",
                    "stateId": "%s"
                }
                """.formatted(state.getId());

            MvcResult result = mockMvc.perform(post("/api/v1/account/listings")
                            .header("Authorization", authHeader(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.listingNumber").isNumber())
                    .andExpect(jsonPath("$.data.slug").value("honda-cb300r-tokunbo-2023"))
                    .andExpect(jsonPath("$.data.canonicalUrl").exists())
                    .andExpect(jsonPath("$.data.categoryPath").value("motorcycles"))
                    .andReturn();

            JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
            int listingNumber = response.get("data").get("listingNumber").asInt();

            assertThat(listingNumber).isGreaterThanOrEqualTo(10000);

            String canonicalUrl = response.get("data").get("canonicalUrl").asText();
            assertThat(canonicalUrl).contains("/motorcycles/lagos/");
            assertThat(canonicalUrl).contains(listingNumber + "-honda-cb300r-tokunbo-2023");
        }

        @Test
        @DisplayName("should generate slug for part listing")
        void createPartListing_generatesCorrectCategoryPath() throws Exception {
            String token = registerAndGetToken("partseller@test.com", "password123");
            State state = createTestState("Kano");

            String createRequest = """
                {
                    "listingType": "PART",
                    "title": "Brake Pads - Honda CB300R",
                    "description": "OEM brake pads",
                    "price": 15000,
                    "category": "SPARE_PART",
                    "condition": "NEW",
                    "partName": "Brake Pads",
                    "stateId": "%s"
                }
                """.formatted(state.getId());

            mockMvc.perform(post("/api/v1/account/listings")
                            .header("Authorization", authHeader(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.categoryPath").value("spare-parts"))
                    .andExpect(jsonPath("$.data.slug").value("brake-pads-honda-cb300r"));
        }

        @Test
        @DisplayName("should handle special characters in title")
        void createListing_handlesSpecialCharacters() throws Exception {
            String token = registerAndGetToken("special@test.com", "password123");
            State state = createTestState("Oyo");

            String createRequest = """
                {
                    "listingType": "VEHICLE",
                    "vehicleType": "TRICYCLE",
                    "title": "TVS King (Clean!) & Ready - 2024",
                    "description": "Clean tricycle",
                    "price": 800000,
                    "category": "TRICYCLE",
                    "condition": "LIKE_NEW",
                    "stateId": "%s"
                }
                """.formatted(state.getId());

            mockMvc.perform(post("/api/v1/account/listings")
                            .header("Authorization", authHeader(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.slug").value("tvs-king-clean-and-ready-2024"))
                    .andExpect(jsonPath("$.data.categoryPath").value("tricycles"));
        }
    }

    @Nested
    @DisplayName("Update Listing - Slug Regeneration")
    class UpdateListingSlugTests {

        @Test
        @DisplayName("should regenerate slug when title changes")
        void updateListing_regeneratesSlugOnTitleChange() throws Exception {
            String token = registerAndGetToken("updater@test.com", "password123");
            User seller = userRepository.findByEmail("updater@test.com").orElseThrow();
            State state = createTestState("Abuja");

            Listing listing = Listing.builder()
                    .listingType(ListingType.VEHICLE)
                    .vehicleType(VehicleType.MOTORCYCLE)
                    .title("Original Title")
                    .slug("original-title")
                    .description("Test")
                    .price(BigDecimal.valueOf(500000))
                    .category(ListingCategory.MOTORCYCLE)
                    .condition(ListingCondition.GOOD)
                    .status(ListingStatus.DRAFT)
                    .seller(seller)
                    .state(state)
                    .build();
            listing = listingRepository.save(listing);
            Integer originalListingNumber = listing.getListingNumber();

            String updateRequest = """
                {
                    "title": "Updated Honda CB300R 2024"
                }
                """;

            mockMvc.perform(put("/api/v1/account/listings/" + listing.getId())
                            .header("Authorization", authHeader(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.slug").value("updated-honda-cb300r-2024"))
                    .andExpect(jsonPath("$.data.listingNumber").value(originalListingNumber));
        }

        @Test
        @DisplayName("should keep slug when title unchanged")
        void updateListing_keepsSlugWhenTitleUnchanged() throws Exception {
            String token = registerAndGetToken("keeper@test.com", "password123");
            User seller = userRepository.findByEmail("keeper@test.com").orElseThrow();
            State state = createTestState("Rivers");

            Listing listing = Listing.builder()
                    .listingType(ListingType.VEHICLE)
                    .vehicleType(VehicleType.BICYCLE)
                    .title("Mountain Bike 26 Inch")
                    .slug("mountain-bike-26-inch")
                    .description("Test")
                    .price(BigDecimal.valueOf(150000))
                    .category(ListingCategory.BICYCLE)
                    .condition(ListingCondition.GOOD)
                    .status(ListingStatus.DRAFT)
                    .seller(seller)
                    .state(state)
                    .build();
            listing = listingRepository.save(listing);

            String updateRequest = """
                {
                    "price": 180000
                }
                """;

            mockMvc.perform(put("/api/v1/account/listings/" + listing.getId())
                            .header("Authorization", authHeader(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.slug").value("mountain-bike-26-inch"));
        }
    }

    @Nested
    @DisplayName("Location Slug Resolution")
    class LocationSlugResolutionTests {

        @Test
        @DisplayName("should resolve full slug path to IDs")
        void resolveSlugPath_fullPath_returnsAllIds() {
            State state = createTestState("Lagos");
            Axis axis = createTestAxis("Mainland", state);
            Area area = createTestArea("Ikeja", axis);

            LocationResolution resolution = locationService.resolveSlugPath("lagos", "mainland", "ikeja");

            assertThat(resolution.stateId()).isEqualTo(state.getId());
            assertThat(resolution.axisId()).isEqualTo(axis.getId());
            assertThat(resolution.areaId()).isEqualTo(area.getId());
        }

        @Test
        @DisplayName("should resolve partial slug path (state only)")
        void resolveSlugPath_stateOnly_returnsStateId() {
            State state = createTestState("Kaduna");

            LocationResolution resolution = locationService.resolveSlugPath("kaduna", null, null);

            assertThat(resolution.stateId()).isEqualTo(state.getId());
            assertThat(resolution.axisId()).isNull();
            assertThat(resolution.areaId()).isNull();
        }

        @Test
        @DisplayName("should resolve partial slug path (state + axis)")
        void resolveSlugPath_stateAndAxis_returnsStateAndAxisIds() {
            State state = createTestState("Enugu");
            Axis axis = createTestAxis("Central", state);

            LocationResolution resolution = locationService.resolveSlugPath("enugu", "central", null);

            assertThat(resolution.stateId()).isEqualTo(state.getId());
            assertThat(resolution.axisId()).isEqualTo(axis.getId());
            assertThat(resolution.areaId()).isNull();
        }

        @Test
        @DisplayName("should return nulls for unknown slugs")
        void resolveSlugPath_unknownSlugs_returnsNulls() {
            LocationResolution resolution = locationService.resolveSlugPath("unknown-state", "unknown-axis", "unknown-area");

            assertThat(resolution.stateId()).isNull();
            assertThat(resolution.axisId()).isNull();
            assertThat(resolution.areaId()).isNull();
        }

        @Test
        @DisplayName("should handle null slugs gracefully")
        void resolveSlugPath_nullSlugs_returnsNulls() {
            LocationResolution resolution = locationService.resolveSlugPath(null, null, null);

            assertThat(resolution.stateId()).isNull();
            assertThat(resolution.axisId()).isNull();
            assertThat(resolution.areaId()).isNull();
        }
    }

    @Nested
    @DisplayName("Listing Response - SEO Fields")
    class ListingResponseSeoFieldsTests {

        @Test
        @DisplayName("GET listing should include all SEO fields")
        void getListing_includesAllSeoFields() throws Exception {
            String token = registerAndGetToken("viewer@test.com", "password123");
            User seller = userRepository.findByEmail("viewer@test.com").orElseThrow();
            State state = createTestState("Delta");
            Axis axis = createTestAxis("Warri", state);
            Area area = createTestArea("Effurun", axis);

            Listing listing = Listing.builder()
                    .listingType(ListingType.VEHICLE)
                    .vehicleType(VehicleType.MOTORCYCLE)
                    .title("Bajaj Pulsar NS200")
                    .slug("bajaj-pulsar-ns200")
                    .description("Fast bike")
                    .price(BigDecimal.valueOf(750000))
                    .category(ListingCategory.MOTORCYCLE)
                    .condition(ListingCondition.GOOD)
                    .status(ListingStatus.ACTIVE)
                    .seller(seller)
                    .state(state)
                    .axis(axis)
                    .area(area)
                    .build();
            listing = listingRepository.save(listing);

            mockMvc.perform(get("/api/v1/listings/" + listing.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.listingNumber").value(listing.getListingNumber()))
                    .andExpect(jsonPath("$.data.slug").value("bajaj-pulsar-ns200"))
                    .andExpect(jsonPath("$.data.categoryPath").value("motorcycles"))
                    .andExpect(jsonPath("$.data.canonicalUrl").value(
                            "/motorcycles/delta/warri/effurun/" + listing.getListingNumber() + "-bajaj-pulsar-ns200"))
                    .andExpect(jsonPath("$.data.statePath").value("/motorcycles/delta"))
                    .andExpect(jsonPath("$.data.axisPath").value("/motorcycles/delta/warri"))
                    .andExpect(jsonPath("$.data.areaPath").value("/motorcycles/delta/warri/effurun"));
        }

        @Test
        @DisplayName("listing summary should include SEO fields")
        void getListings_summaryIncludesSeoFields() throws Exception {
            String token = registerAndGetToken("lister@test.com", "password123");
            User seller = userRepository.findByEmail("lister@test.com").orElseThrow();
            State state = createTestState("Ondo");

            Listing listing = Listing.builder()
                    .listingType(ListingType.VEHICLE)
                    .vehicleType(VehicleType.MOTORCYCLE)
                    .title("Honda Activa")
                    .slug("honda-activa")
                    .description("Scooter")
                    .price(BigDecimal.valueOf(350000))
                    .category(ListingCategory.MOTORCYCLE)
                    .condition(ListingCondition.GOOD)
                    .status(ListingStatus.ACTIVE)
                    .seller(seller)
                    .state(state)
                    .build();
            listingRepository.save(listing);

            mockMvc.perform(get("/api/v1/listings"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].listingNumber").isNumber())
                    .andExpect(jsonPath("$.data.content[0].slug").exists())
                    .andExpect(jsonPath("$.data.content[0].canonicalUrl").exists())
                    .andExpect(jsonPath("$.data.content[0].categoryPath").exists());
        }
    }

    @Nested
    @DisplayName("Find by Listing Number")
    class FindByListingNumberTests {

        @Test
        @DisplayName("should find listing by listing_number")
        void findByListingNumber_existingNumber_returnsListing() {
            User seller = createTestUser("findtest@test.com", Role.USER);
            State state = createTestState("Benue");

            Listing listing = Listing.builder()
                    .listingType(ListingType.VEHICLE)
                    .vehicleType(VehicleType.MOTORCYCLE)
                    .title("Test Bike")
                    .slug("test-bike")
                    .description("Test")
                    .price(BigDecimal.valueOf(100000))
                    .category(ListingCategory.MOTORCYCLE)
                    .condition(ListingCondition.GOOD)
                    .status(ListingStatus.ACTIVE)
                    .seller(seller)
                    .state(state)
                    .build();
            listing = listingRepository.save(listing);
            Integer listingNumber = listing.getListingNumber();

            var found = listingRepository.findByListingNumber(listingNumber);

            assertThat(found).isPresent();
            assertThat(found.get().getId()).isEqualTo(listing.getId());
        }

        @Test
        @DisplayName("should return empty for non-existent listing_number")
        void findByListingNumber_nonExistent_returnsEmpty() {
            var found = listingRepository.findByListingNumber(99999);

            assertThat(found).isEmpty();
        }
    }
}
