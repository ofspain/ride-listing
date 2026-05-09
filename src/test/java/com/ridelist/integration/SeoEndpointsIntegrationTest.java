package com.ridelist.integration;

import com.ridelist.model.*;
import com.ridelist.repository.ListingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SeoEndpointsIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ListingRepository listingRepository;

    @Nested
    @DisplayName("GET /api/v1/listings/ref/{ref}")
    class GetListingByRefTests {

        @Test
        @DisplayName("should return listing by listing number only")
        void validListingNumber_returnsListing() throws Exception {
            User seller = createTestUser("refseller@test.com", Role.USER);
            State state = createTestState("Lagos");

            Listing listing = Listing.builder()
                    .listingType(ListingType.VEHICLE)
                    .vehicleType(VehicleType.MOTORCYCLE)
                    .title("Honda CB300R Tokunbo 2023")
                    .slug("honda-cb300r-tokunbo-2023")
                    .description("Clean motorcycle")
                    .price(BigDecimal.valueOf(500000))
                    .category(ListingCategory.MOTORCYCLE)
                    .condition(ListingCondition.GOOD)
                    .status(ListingStatus.ACTIVE)
                    .seller(seller)
                    .state(state)
                    .build();
            listing = listingRepository.save(listing);
            Integer listingNumber = listing.getListingNumber();

            mockMvc.perform(get("/api/v1/listings/ref/" + listingNumber))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(listing.getId().toString()))
                    .andExpect(jsonPath("$.data.title").value("Honda CB300R Tokunbo 2023"));
        }

        @Test
        @DisplayName("should return listing with slug appended (slug ignored)")
        void listingNumberWithSlug_returnsListing() throws Exception {
            User seller = createTestUser("refslug@test.com", Role.USER);
            State state = createTestState("Kano");

            Listing listing = Listing.builder()
                    .listingType(ListingType.VEHICLE)
                    .vehicleType(VehicleType.TRICYCLE)
                    .title("TVS King Keke")
                    .slug("tvs-king-keke")
                    .description("Good tricycle")
                    .price(BigDecimal.valueOf(800000))
                    .category(ListingCategory.TRICYCLE)
                    .condition(ListingCondition.GOOD)
                    .status(ListingStatus.ACTIVE)
                    .seller(seller)
                    .state(state)
                    .build();
            listing = listingRepository.save(listing);
            Integer listingNumber = listing.getListingNumber();

            // Slug is ignored - any text after the number works
            mockMvc.perform(get("/api/v1/listings/ref/" + listingNumber + "-anything-here"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(listing.getId().toString()));

            // Wrong slug still works
            mockMvc.perform(get("/api/v1/listings/ref/" + listingNumber + "-wrong-slug"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(listing.getId().toString()));
        }

        @Test
        @DisplayName("should return 404 for non-existent listing number")
        void nonExistentListingNumber_returns404() throws Exception {
            mockMvc.perform(get("/api/v1/listings/ref/99999"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 404 for invalid ref format")
        void invalidRefFormat_returns404() throws Exception {
            mockMvc.perform(get("/api/v1/listings/ref/abc-invalid"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 404 for DELETED listing")
        void deletedListing_returns404() throws Exception {
            User seller = createTestUser("deletedref@test.com", Role.USER);
            State state = createTestState("Oyo");

            Listing listing = Listing.builder()
                    .listingType(ListingType.VEHICLE)
                    .vehicleType(VehicleType.MOTORCYCLE)
                    .title("Deleted Bike")
                    .slug("deleted-bike")
                    .description("Deleted")
                    .price(BigDecimal.valueOf(100000))
                    .category(ListingCategory.MOTORCYCLE)
                    .condition(ListingCondition.GOOD)
                    .status(ListingStatus.DELETED)
                    .seller(seller)
                    .state(state)
                    .build();
            listing = listingRepository.save(listing);

            mockMvc.perform(get("/api/v1/listings/ref/" + listing.getListingNumber()))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/listings/{idOrRef} (backward compatible)")
    class GetListingByIdOrRefTests {

        @Test
        @DisplayName("should return listing by UUID")
        void validUUID_returnsListing() throws Exception {
            User seller = createTestUser("uuidseller@test.com", Role.USER);
            State state = createTestState("Abuja");

            Listing listing = Listing.builder()
                    .listingType(ListingType.VEHICLE)
                    .vehicleType(VehicleType.MOTORCYCLE)
                    .title("UUID Test Bike")
                    .slug("uuid-test-bike")
                    .description("Test")
                    .price(BigDecimal.valueOf(300000))
                    .category(ListingCategory.MOTORCYCLE)
                    .condition(ListingCondition.GOOD)
                    .status(ListingStatus.ACTIVE)
                    .seller(seller)
                    .state(state)
                    .build();
            listing = listingRepository.save(listing);

            mockMvc.perform(get("/api/v1/listings/" + listing.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(listing.getId().toString()));
        }

        @Test
        @DisplayName("should return listing by listing number")
        void validListingNumber_returnsListing() throws Exception {
            User seller = createTestUser("numberseller@test.com", Role.USER);
            State state = createTestState("Rivers");

            Listing listing = Listing.builder()
                    .listingType(ListingType.VEHICLE)
                    .vehicleType(VehicleType.BICYCLE)
                    .title("Mountain Bike")
                    .slug("mountain-bike")
                    .description("Test")
                    .price(BigDecimal.valueOf(150000))
                    .category(ListingCategory.BICYCLE)
                    .condition(ListingCondition.GOOD)
                    .status(ListingStatus.ACTIVE)
                    .seller(seller)
                    .state(state)
                    .build();
            listing = listingRepository.save(listing);

            mockMvc.perform(get("/api/v1/listings/" + listing.getListingNumber()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(listing.getId().toString()));
        }

        @Test
        @DisplayName("should return listing by listing number with slug")
        void listingNumberWithSlug_returnsListing() throws Exception {
            User seller = createTestUser("slugseller@test.com", Role.USER);
            State state = createTestState("Enugu");

            Listing listing = Listing.builder()
                    .listingType(ListingType.PART)
                    .title("Brake Pads")
                    .slug("brake-pads")
                    .description("OEM pads")
                    .price(BigDecimal.valueOf(15000))
                    .category(ListingCategory.SPARE_PART)
                    .condition(ListingCondition.NEW)
                    .status(ListingStatus.ACTIVE)
                    .seller(seller)
                    .state(state)
                    .partName("Brake Pads")
                    .build();
            listing = listingRepository.save(listing);

            mockMvc.perform(get("/api/v1/listings/" + listing.getListingNumber() + "-brake-pads"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(listing.getId().toString()));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/listings/browse/{categoryPath}")
    class BrowseListingsTests {

        @Test
        @DisplayName("should return motorcycles by category path")
        void motorcyclesCategory_returnsVehicleListings() throws Exception {
            User seller = createTestUser("browseseller@test.com", Role.USER);
            State state = createTestState("Lagos");

            Listing motorcycle = Listing.builder()
                    .listingType(ListingType.VEHICLE)
                    .vehicleType(VehicleType.MOTORCYCLE)
                    .title("Honda CB300R")
                    .slug("honda-cb300r")
                    .description("Motorcycle")
                    .price(BigDecimal.valueOf(500000))
                    .category(ListingCategory.MOTORCYCLE)
                    .condition(ListingCondition.GOOD)
                    .status(ListingStatus.ACTIVE)
                    .seller(seller)
                    .state(state)
                    .build();
            listingRepository.save(motorcycle);

            Listing tricycle = Listing.builder()
                    .listingType(ListingType.VEHICLE)
                    .vehicleType(VehicleType.TRICYCLE)
                    .title("TVS King")
                    .slug("tvs-king")
                    .description("Tricycle")
                    .price(BigDecimal.valueOf(800000))
                    .category(ListingCategory.TRICYCLE)
                    .condition(ListingCondition.GOOD)
                    .status(ListingStatus.ACTIVE)
                    .seller(seller)
                    .state(state)
                    .build();
            listingRepository.save(tricycle);

            mockMvc.perform(get("/api/v1/listings/browse/motorcycles"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content[0].vehicleType").value("MOTORCYCLE"));
        }

        @Test
        @DisplayName("should filter by state slug")
        void stateSlugFilter_filtersCorrectly() throws Exception {
            User seller = createTestUser("stateseller@test.com", Role.USER);
            State lagos = createTestState("Lagos");
            State kano = createTestState("Kano");

            Listing lagosListing = Listing.builder()
                    .listingType(ListingType.VEHICLE)
                    .vehicleType(VehicleType.MOTORCYCLE)
                    .title("Lagos Bike")
                    .slug("lagos-bike")
                    .description("Motorcycle")
                    .price(BigDecimal.valueOf(500000))
                    .category(ListingCategory.MOTORCYCLE)
                    .condition(ListingCondition.GOOD)
                    .status(ListingStatus.ACTIVE)
                    .seller(seller)
                    .state(lagos)
                    .build();
            listingRepository.save(lagosListing);

            Listing kanoListing = Listing.builder()
                    .listingType(ListingType.VEHICLE)
                    .vehicleType(VehicleType.MOTORCYCLE)
                    .title("Kano Bike")
                    .slug("kano-bike")
                    .description("Motorcycle")
                    .price(BigDecimal.valueOf(600000))
                    .category(ListingCategory.MOTORCYCLE)
                    .condition(ListingCondition.GOOD)
                    .status(ListingStatus.ACTIVE)
                    .seller(seller)
                    .state(kano)
                    .build();
            listingRepository.save(kanoListing);

            mockMvc.perform(get("/api/v1/listings/browse/motorcycles")
                            .param("stateSlug", "lagos"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.totalElements").value(1));
        }

        @Test
        @DisplayName("should return spare-parts category")
        void sparePartsCategory_returnsPartListings() throws Exception {
            User seller = createTestUser("partseller@test.com", Role.USER);
            State state = createTestState("Delta");

            Listing part = Listing.builder()
                    .listingType(ListingType.PART)
                    .title("Brake Pads Set")
                    .slug("brake-pads-set")
                    .description("OEM brake pads")
                    .price(BigDecimal.valueOf(15000))
                    .category(ListingCategory.SPARE_PART)
                    .condition(ListingCondition.NEW)
                    .status(ListingStatus.ACTIVE)
                    .seller(seller)
                    .state(state)
                    .partName("Brake Pads")
                    .build();
            listingRepository.save(part);

            mockMvc.perform(get("/api/v1/listings/browse/spare-parts"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content[0].listingType").value("PART"));
        }

        @Test
        @DisplayName("should return 404 for unknown category")
        void unknownCategory_returns404() throws Exception {
            mockMvc.perform(get("/api/v1/listings/browse/cars"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should handle unknown state slug gracefully")
        void unknownStateSlug_returnsEmptyResults() throws Exception {
            mockMvc.perform(get("/api/v1/listings/browse/motorcycles")
                            .param("stateSlug", "unknown-state"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.totalElements").value(0));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/listings/browse/{categoryPath}/meta")
    class BrowsePageMetaTests {

        @Test
        @DisplayName("should return correct metadata for category only")
        void categoryOnly_returnsCorrectMeta() throws Exception {
            mockMvc.perform(get("/api/v1/listings/browse/motorcycles/meta"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.title").value("Motorcycles for Sale in Nigeria"))
                    .andExpect(jsonPath("$.data.categoryLabel").value("Motorcycles"))
                    .andExpect(jsonPath("$.data.locationLabel").value("Nigeria"))
                    .andExpect(jsonPath("$.data.canonicalUrl").value("/motorcycles"));
        }

        @Test
        @DisplayName("should return correct metadata with state")
        void withState_returnsCorrectMeta() throws Exception {
            mockMvc.perform(get("/api/v1/listings/browse/motorcycles/meta")
                            .param("stateSlug", "lagos"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.title").value("Motorcycles for Sale in Lagos"))
                    .andExpect(jsonPath("$.data.locationLabel").value("Lagos"))
                    .andExpect(jsonPath("$.data.canonicalUrl").value("/motorcycles/lagos"));
        }

        @Test
        @DisplayName("should return correct metadata with state and axis")
        void withStateAndAxis_returnsCorrectMeta() throws Exception {
            mockMvc.perform(get("/api/v1/listings/browse/motorcycles/meta")
                            .param("stateSlug", "lagos")
                            .param("axisSlug", "mainland"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.title").value("Motorcycles for Sale in Mainland, Lagos"))
                    .andExpect(jsonPath("$.data.locationLabel").value("Mainland, Lagos"))
                    .andExpect(jsonPath("$.data.canonicalUrl").value("/motorcycles/lagos/mainland"));
        }

        @Test
        @DisplayName("should return correct metadata with full location")
        void withFullLocation_returnsCorrectMeta() throws Exception {
            mockMvc.perform(get("/api/v1/listings/browse/motorcycles/meta")
                            .param("stateSlug", "lagos")
                            .param("axisSlug", "mainland")
                            .param("areaSlug", "ikeja"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.title").value("Motorcycles for Sale in Ikeja, Lagos"))
                    .andExpect(jsonPath("$.data.locationLabel").value("Ikeja, Lagos"))
                    .andExpect(jsonPath("$.data.canonicalUrl").value("/motorcycles/lagos/mainland/ikeja"));
        }

        @Test
        @DisplayName("should return correct metadata for spare-parts")
        void spareParts_returnsCorrectMeta() throws Exception {
            mockMvc.perform(get("/api/v1/listings/browse/spare-parts/meta"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.title").value("Spare Parts & Accessories for Sale in Nigeria"))
                    .andExpect(jsonPath("$.data.categoryLabel").value("Spare Parts & Accessories"));
        }

        @Test
        @DisplayName("should return 404 for unknown category")
        void unknownCategory_returns404() throws Exception {
            mockMvc.perform(get("/api/v1/listings/browse/cars/meta"))
                    .andExpect(status().isNotFound());
        }
    }
}
