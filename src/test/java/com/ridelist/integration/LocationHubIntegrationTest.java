package com.ridelist.integration;

import com.ridelist.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Location Hub Integration Tests")
public class LocationHubIntegrationTest extends BaseIntegrationTest {

    @Nested
    @DisplayName("Location Hub Endpoint Tests")
    class LocationHubEndpointTests {

        @Test
        @DisplayName("LOCHUB-001: GET /listings/browse/motorcycles/locations returns states with counts")
        void getLocationHub_noStateSlug_returnsStatesWithCounts() throws Exception {
            User seller = createTestUser("seller@test.com", Role.USER);
            State lagos = createTestState("Lagos");
            State kano = createTestState("Kano");

            createActiveListing(seller, lagos, null, null, ListingType.VEHICLE, VehicleType.MOTORCYCLE);
            createActiveListing(seller, lagos, null, null, ListingType.VEHICLE, VehicleType.MOTORCYCLE);
            createActiveListing(seller, kano, null, null, ListingType.VEHICLE, VehicleType.MOTORCYCLE);

            mockMvc.perform(get("/api/v1/listings/browse/motorcycles/locations"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.level").value("states"))
                    .andExpect(jsonPath("$.data.locations.length()").value(2))
                    .andExpect(jsonPath("$.data.locations[0].name").value("Lagos"))
                    .andExpect(jsonPath("$.data.locations[0].count").value(2))
                    .andExpect(jsonPath("$.data.locations[0].url").value("/motorcycles/lagos"))
                    .andExpect(jsonPath("$.data.locations[1].name").value("Kano"))
                    .andExpect(jsonPath("$.data.locations[1].count").value(1));
        }

        @Test
        @DisplayName("LOCHUB-002: GET /listings/browse/motorcycles/locations?stateSlug=lagos returns axes with counts")
        void getLocationHub_withStateSlug_returnsAxesWithCounts() throws Exception {
            User seller = createTestUser("seller@test.com", Role.USER);
            State lagos = createTestState("Lagos");
            Axis mainland = createTestAxis("Mainland", lagos);
            Axis island = createTestAxis("Island", lagos);

            createActiveListing(seller, lagos, mainland, null, ListingType.VEHICLE, VehicleType.MOTORCYCLE);
            createActiveListing(seller, lagos, mainland, null, ListingType.VEHICLE, VehicleType.MOTORCYCLE);
            createActiveListing(seller, lagos, mainland, null, ListingType.VEHICLE, VehicleType.MOTORCYCLE);
            createActiveListing(seller, lagos, island, null, ListingType.VEHICLE, VehicleType.MOTORCYCLE);

            mockMvc.perform(get("/api/v1/listings/browse/motorcycles/locations")
                            .param("stateSlug", "lagos"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.level").value("axes"))
                    .andExpect(jsonPath("$.data.locations.length()").value(2))
                    .andExpect(jsonPath("$.data.locations[0].name").value("Mainland"))
                    .andExpect(jsonPath("$.data.locations[0].count").value(3))
                    .andExpect(jsonPath("$.data.locations[0].url").value("/motorcycles/lagos/mainland"));
        }

        @Test
        @DisplayName("LOCHUB-003: GET /listings/browse/motorcycles/locations?stateSlug=lagos&axisSlug=mainland returns areas with counts")
        void getLocationHub_withAxisSlug_returnsAreasWithCounts() throws Exception {
            User seller = createTestUser("seller@test.com", Role.USER);
            State lagos = createTestState("Lagos");
            Axis mainland = createTestAxis("Mainland", lagos);
            Area ikeja = createTestArea("Ikeja", mainland);
            Area surulere = createTestArea("Surulere", mainland);

            createActiveListing(seller, lagos, mainland, ikeja, ListingType.VEHICLE, VehicleType.MOTORCYCLE);
            createActiveListing(seller, lagos, mainland, ikeja, ListingType.VEHICLE, VehicleType.MOTORCYCLE);
            createActiveListing(seller, lagos, mainland, surulere, ListingType.VEHICLE, VehicleType.MOTORCYCLE);

            mockMvc.perform(get("/api/v1/listings/browse/motorcycles/locations")
                            .param("stateSlug", "lagos")
                            .param("axisSlug", "mainland"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.level").value("areas"))
                    .andExpect(jsonPath("$.data.locations.length()").value(2))
                    .andExpect(jsonPath("$.data.locations[0].name").value("Ikeja"))
                    .andExpect(jsonPath("$.data.locations[0].count").value(2))
                    .andExpect(jsonPath("$.data.locations[0].url").value("/motorcycles/lagos/mainland/ikeja"));
        }

        @Test
        @DisplayName("LOCHUB-004: Empty location (no listings) not returned in list")
        void getLocationHub_emptyLocation_notReturned() throws Exception {
            User seller = createTestUser("seller@test.com", Role.USER);
            State lagos = createTestState("Lagos");
            State kano = createTestState("Kano");

            createActiveListing(seller, lagos, null, null, ListingType.VEHICLE, VehicleType.MOTORCYCLE);

            mockMvc.perform(get("/api/v1/listings/browse/motorcycles/locations"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.locations.length()").value(1))
                    .andExpect(jsonPath("$.data.locations[0].name").value("Lagos"));
        }

        @Test
        @DisplayName("LOCHUB-005: Invalid state slug returns 404")
        void getLocationHub_invalidStateSlug_returns404() throws Exception {
            mockMvc.perform(get("/api/v1/listings/browse/motorcycles/locations")
                            .param("stateSlug", "invalid-state"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Browse Page Meta Nigerian Variants Tests")
    class BrowsePageMetaNigerianVariantsTests {

        @Test
        @DisplayName("LOCHUB-006: BrowsePageMeta for motorcycles includes 'okada' in description")
        void getBrowsePageMeta_motorcycles_includesOkada() throws Exception {
            mockMvc.perform(get("/api/v1/listings/browse/motorcycles/meta"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.description", containsString("okada bikes")));
        }

        @Test
        @DisplayName("LOCHUB-007: BrowsePageMeta for tricycles includes 'keke napep' in description")
        void getBrowsePageMeta_tricycles_includesKeKeNapep() throws Exception {
            mockMvc.perform(get("/api/v1/listings/browse/tricycles/meta"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.description", containsString("keke napep")));
        }

        @Test
        @DisplayName("LOCHUB-008: BrowsePageMeta includes 'tokunbo' in description")
        void getBrowsePageMeta_includesTokenbo() throws Exception {
            mockMvc.perform(get("/api/v1/listings/browse/motorcycles/meta"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.description", containsString("tokunbo")));
        }

        @Test
        @DisplayName("LOCHUB-009: BrowsePageMeta for spare-parts includes 'accessories' in description")
        void getBrowsePageMeta_spareParts_includesAccessories() throws Exception {
            mockMvc.perform(get("/api/v1/listings/browse/spare-parts/meta"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.description", containsString("accessories")));
        }
    }

    private Listing createActiveListing(User seller, State state, Axis axis, Area area,
                                        ListingType listingType, VehicleType vehicleType) {
        Listing listing = Listing.builder()
                .title("Test Listing For Location Hub Integration Test Cases")
                .description("Description")
                .price(BigDecimal.valueOf(150000))
                .seller(seller)
                .listingType(listingType)
                .vehicleType(vehicleType)
                .category(listingType == ListingType.VEHICLE ? ListingCategory.MOTORCYCLE : ListingCategory.SPARE_PART)
                .status(ListingStatus.ACTIVE)
                .condition(ListingCondition.GOOD)
                .state(state)
                .axis(axis)
                .area(area)
                .build();

        if (listingType == ListingType.PART) {
            listing.setPartName("Test Part");
        }

        return listingRepository.save(listing);
    }
}
