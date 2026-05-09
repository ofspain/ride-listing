package com.ridelist.integration;

import com.ridelist.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for multi-value location and attribute filtering.
 * Tests the new search system with OR/AND predicate logic.
 */
@DisplayName("Multi-Value Filter Integration Tests")
class MultiValueFilterIntegrationTest extends BaseIntegrationTest {

    private User seller;
    private State lagos;
    private State abuja;
    private Axis ikeja;
    private Axis lekki;
    private Area opebi;
    private Area victoria;

    @BeforeEach
    void setup() {
        seller = createTestUser("seller@test.com", Role.USER);

        // Create location hierarchy
        lagos = createTestState("Lagos");
        abuja = createTestState("Abuja");

        ikeja = createTestAxis("Ikeja", lagos);
        lekki = createTestAxis("Lekki", lagos);

        opebi = createTestArea("Opebi", ikeja);
        victoria = createTestArea("Victoria Island", lekki);
    }

    // ==================== LOCATION SLUG FILTER TESTS ====================

    @Nested
    @DisplayName("Location Slug Filter Tests")
    class LocationSlugFilterTests {

        @Test
        @DisplayName("MULTI-LOC-001: Single location slug filters correctly")
        void singleLocationSlug_filtersCorrectly() throws Exception {
            Listing ikejaListing = createActiveListing("Ikeja Bike", lagos, ikeja, null);
            Listing abujaListing = createActiveListing("Abuja Bike", abuja, null, null);

            mockMvc.perform(get("/api/v1/listings")
                            .param("location", "ikeja"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].title").value("Ikeja Bike"));
        }

        @Test
        @DisplayName("MULTI-LOC-002: Multiple location slugs use OR logic")
        void multipleLocationSlugs_useOrLogic() throws Exception {
            Listing ikejaListing = createActiveListing("Ikeja Bike", lagos, ikeja, null);
            Listing lekkiListing = createActiveListing("Lekki Bike", lagos, lekki, null);
            Listing abujaListing = createActiveListing("Abuja Bike", abuja, null, null);

            mockMvc.perform(get("/api/v1/listings")
                            .param("location", "ikeja")
                            .param("location", "lekki"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(2));
        }

        @Test
        @DisplayName("MULTI-LOC-003: Location slug matches state level")
        void locationSlug_matchesStateLevel() throws Exception {
            Listing lagosListing = createActiveListing("Lagos Bike", lagos, null, null);
            Listing abujaListing = createActiveListing("Abuja Bike", abuja, null, null);

            mockMvc.perform(get("/api/v1/listings")
                            .param("location", "lagos"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].title").value("Lagos Bike"));
        }

        @Test
        @DisplayName("MULTI-LOC-004: Location slug matches axis level")
        void locationSlug_matchesAxisLevel() throws Exception {
            Listing ikejaListing = createActiveListing("Ikeja Bike", lagos, ikeja, null);
            Listing lekkiListing = createActiveListing("Lekki Bike", lagos, lekki, null);

            mockMvc.perform(get("/api/v1/listings")
                            .param("location", "ikeja"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].title").value("Ikeja Bike"));
        }

        @Test
        @DisplayName("MULTI-LOC-005: Location slug matches area level")
        void locationSlug_matchesAreaLevel() throws Exception {
            Listing opebiListing = createActiveListing("Opebi Bike", lagos, ikeja, opebi);
            Listing victoriaListing = createActiveListing("VI Bike", lagos, lekki, victoria);

            mockMvc.perform(get("/api/v1/listings")
                            .param("location", "opebi"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].title").value("Opebi Bike"));
        }

        @Test
        @DisplayName("MULTI-LOC-006: Empty location list applies no filter")
        void emptyLocationList_appliesNoFilter() throws Exception {
            createActiveListing("Ikeja Bike", lagos, ikeja, null);
            createActiveListing("Abuja Bike", abuja, null, null);

            mockMvc.perform(get("/api/v1/listings"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(2));
        }

        @Test
        @DisplayName("MULTI-LOC-007: Unknown location slug returns empty results")
        void unknownLocationSlug_returnsEmptyResults() throws Exception {
            createActiveListing("Ikeja Bike", lagos, ikeja, null);

            mockMvc.perform(get("/api/v1/listings")
                            .param("location", "nonexistent"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(0));
        }

        @Test
        @DisplayName("MULTI-LOC-008: Mixed state and axis slugs work together")
        void mixedStateAndAxisSlugs_workTogether() throws Exception {
            Listing lagosOnlyListing = createActiveListing("Lagos Only", lagos, null, null);
            Listing ikejaListing = createActiveListing("Ikeja Bike", lagos, ikeja, null);
            Listing abujaListing = createActiveListing("Abuja Bike", abuja, null, null);

            // Search for Lagos state OR Abuja state
            mockMvc.perform(get("/api/v1/listings")
                            .param("location", "lagos")
                            .param("location", "abuja"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(3));
        }
    }

    // ==================== ATTRIBUTE FILTER TESTS ====================

    @Nested
    @DisplayName("Attribute Filter Tests")
    class AttributeFilterTests {

        @Test
        @DisplayName("MULTI-ATTR-001: Single attribute value filters correctly")
        void singleAttributeValue_filtersCorrectly() throws Exception {
            AttributeDefinition engineAttr = createTestAttribute("Engine Type", ListingType.VEHICLE, true,
                    List.of("150cc", "200cc", "250cc"));

            Listing listing150 = createActiveListingWithAttribute("150cc Bike", engineAttr, "150cc");
            Listing listing200 = createActiveListingWithAttribute("200cc Bike", engineAttr, "200cc");

            mockMvc.perform(get("/api/v1/listings")
                            .param("attr_engine-type", "150cc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].title").value("150cc Bike"));
        }

        @Test
        @DisplayName("MULTI-ATTR-002: Multiple values for same attribute use OR logic")
        void multipleValuesForSameAttribute_useOrLogic() throws Exception {
            AttributeDefinition engineAttr = createTestAttribute("Engine CC", ListingType.VEHICLE, true,
                    List.of("150cc", "200cc", "250cc"));

            Listing listing150 = createActiveListingWithAttribute("150cc Bike", engineAttr, "150cc");
            Listing listing200 = createActiveListingWithAttribute("200cc Bike", engineAttr, "200cc");
            Listing listing250 = createActiveListingWithAttribute("250cc Bike", engineAttr, "250cc");

            mockMvc.perform(get("/api/v1/listings")
                            .param("attr_engine-cc", "150cc")
                            .param("attr_engine-cc", "200cc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(2));
        }

        @Test
        @DisplayName("MULTI-ATTR-003: Different attributes use AND logic")
        void differentAttributes_useAndLogic() throws Exception {
            AttributeDefinition engineAttr = createTestAttribute("Engine Size", ListingType.VEHICLE, true,
                    List.of("150cc", "200cc"));
            AttributeDefinition fuelAttr = createTestAttribute("Fuel Type", ListingType.VEHICLE, true,
                    List.of("Petrol", "Electric"));

            // Create listing with both attributes
            Listing matchingListing = createActiveListingWithAttributes("Petrol 150cc",
                    List.of(engineAttr, fuelAttr), List.of("150cc", "Petrol"));

            // Create listing with only one attribute matching
            Listing partialListing = createActiveListingWithAttributes("Electric 150cc",
                    List.of(engineAttr, fuelAttr), List.of("150cc", "Electric"));

            mockMvc.perform(get("/api/v1/listings")
                            .param("attr_engine-size", "150cc")
                            .param("attr_fuel-type", "Petrol"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].title").value("Petrol 150cc"));
        }

        @Test
        @DisplayName("MULTI-ATTR-004: Unknown attribute slug returns empty results gracefully")
        void unknownAttributeSlug_returnsEmptyGracefully() throws Exception {
            AttributeDefinition engineAttr = createTestAttribute("Known Engine", ListingType.VEHICLE, true,
                    List.of("150cc", "200cc"));
            createActiveListingWithAttribute("Test Bike", engineAttr, "150cc");

            mockMvc.perform(get("/api/v1/listings")
                            .param("attr_unknown-attribute", "somevalue"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(0));
        }

        @Test
        @DisplayName("MULTI-ATTR-005: Invalid attribute value returns empty results")
        void invalidAttributeValue_returnsEmptyResults() throws Exception {
            AttributeDefinition engineAttr = createTestAttribute("Engine Power", ListingType.VEHICLE, true,
                    List.of("150cc", "200cc"));
            createActiveListingWithAttribute("Test Bike", engineAttr, "150cc");

            mockMvc.perform(get("/api/v1/listings")
                            .param("attr_engine-power", "999cc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(0));
        }

        @Test
        @DisplayName("MULTI-ATTR-006: Case insensitive attribute value matching")
        void caseInsensitiveAttributeValueMatching() throws Exception {
            AttributeDefinition fuelAttr = createTestAttribute("Fuel Kind", ListingType.VEHICLE, true,
                    List.of("Petrol", "Electric"));
            createActiveListingWithAttribute("Petrol Bike", fuelAttr, "Petrol");

            mockMvc.perform(get("/api/v1/listings")
                            .param("attr_fuel-kind", "petrol"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1));
        }
    }

    // ==================== COMBINED FILTER TESTS ====================

    @Nested
    @DisplayName("Combined Location and Attribute Filter Tests")
    class CombinedFilterTests {

        @Test
        @DisplayName("COMBINED-001: Location AND attribute filters work together")
        void locationAndAttributeFilters_workTogether() throws Exception {
            AttributeDefinition engineAttr = createTestAttribute("CC Rating", ListingType.VEHICLE, true,
                    List.of("150cc", "200cc"));

            Listing ikejaListing = createActiveListingWithAttributeAndLocation(
                    "Ikeja 150cc", lagos, ikeja, null, engineAttr, "150cc");
            Listing lekkiListing = createActiveListingWithAttributeAndLocation(
                    "Lekki 150cc", lagos, lekki, null, engineAttr, "150cc");
            Listing ikejaListing200 = createActiveListingWithAttributeAndLocation(
                    "Ikeja 200cc", lagos, ikeja, null, engineAttr, "200cc");

            // Filter: location=ikeja AND attr_cc-rating=150cc
            mockMvc.perform(get("/api/v1/listings")
                            .param("location", "ikeja")
                            .param("attr_cc-rating", "150cc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].title").value("Ikeja 150cc"));
        }

        @Test
        @DisplayName("COMBINED-002: Multiple locations AND multiple attribute values")
        void multipleLocationsAndMultipleAttributeValues() throws Exception {
            AttributeDefinition engineAttr = createTestAttribute("Displacement", ListingType.VEHICLE, true,
                    List.of("150cc", "200cc", "250cc"));

            Listing ikeja150 = createActiveListingWithAttributeAndLocation(
                    "Ikeja 150", lagos, ikeja, null, engineAttr, "150cc");
            Listing lekki200 = createActiveListingWithAttributeAndLocation(
                    "Lekki 200", lagos, lekki, null, engineAttr, "200cc");
            Listing abuja150 = createActiveListingWithAttributeAndLocation(
                    "Abuja 150", abuja, null, null, engineAttr, "150cc");
            Listing ikeja250 = createActiveListingWithAttributeAndLocation(
                    "Ikeja 250", lagos, ikeja, null, engineAttr, "250cc");

            // Filter: (location=ikeja OR location=lekki) AND (displacement=150cc OR displacement=200cc)
            mockMvc.perform(get("/api/v1/listings")
                            .param("location", "ikeja")
                            .param("location", "lekki")
                            .param("attr_displacement", "150cc")
                            .param("attr_displacement", "200cc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(2));
        }

        @Test
        @DisplayName("COMBINED-003: Location + UUID filters + attributes work together")
        void locationAndUuidFiltersAndAttributes_workTogether() throws Exception {
            AttributeDefinition condAttr = createTestAttribute("Item Condition", ListingType.VEHICLE, true,
                    List.of("New", "Used"));

            Listing lagosNew = createActiveListingWithAttributeAndLocation(
                    "Lagos New", lagos, null, null, condAttr, "New");
            Listing lagosUsed = createActiveListingWithAttributeAndLocation(
                    "Lagos Used", lagos, null, null, condAttr, "Used");

            // Filter by stateId (UUID) AND attribute
            mockMvc.perform(get("/api/v1/listings")
                            .param("stateId", lagos.getId().toString())
                            .param("attr_item-condition", "New"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].title").value("Lagos New"));
        }
    }

    // ==================== BACKWARD COMPATIBILITY TESTS ====================

    @Nested
    @DisplayName("Backward Compatibility Tests")
    class BackwardCompatibilityTests {

        @Test
        @DisplayName("COMPAT-001: UUID-based stateId filter still works")
        void uuidBasedStateIdFilter_stillWorks() throws Exception {
            Listing lagosListing = createActiveListing("Lagos Bike", lagos, null, null);
            Listing abujaListing = createActiveListing("Abuja Bike", abuja, null, null);

            mockMvc.perform(get("/api/v1/listings")
                            .param("stateId", lagos.getId().toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].title").value("Lagos Bike"));
        }

        @Test
        @DisplayName("COMPAT-002: UUID-based axisId filter still works")
        void uuidBasedAxisIdFilter_stillWorks() throws Exception {
            Listing ikejaListing = createActiveListing("Ikeja Bike", lagos, ikeja, null);
            Listing lekkiListing = createActiveListing("Lekki Bike", lagos, lekki, null);

            mockMvc.perform(get("/api/v1/listings")
                            .param("axisId", ikeja.getId().toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].title").value("Ikeja Bike"));
        }

        @Test
        @DisplayName("COMPAT-003: UUID-based areaId filter still works")
        void uuidBasedAreaIdFilter_stillWorks() throws Exception {
            Listing opebiListing = createActiveListing("Opebi Bike", lagos, ikeja, opebi);
            Listing victoriaListing = createActiveListing("VI Bike", lagos, lekki, victoria);

            mockMvc.perform(get("/api/v1/listings")
                            .param("areaId", opebi.getId().toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].title").value("Opebi Bike"));
        }

        @Test
        @DisplayName("COMPAT-004: Listing type and vehicle type filters still work")
        void listingTypeAndVehicleTypeFilters_stillWork() throws Exception {
            Listing motorcycle = Listing.builder()
                    .title("Test Motorcycle")
                    .price(BigDecimal.valueOf(100000))
                    .seller(seller)
                    .listingType(ListingType.VEHICLE)
                    .vehicleType(VehicleType.MOTORCYCLE)
                    .category(ListingCategory.MOTORCYCLE)
                    .status(ListingStatus.ACTIVE)
                    .condition(ListingCondition.GOOD)
                    .state(lagos)
                    .build();
            listingRepository.save(motorcycle);

            Listing tricycle = Listing.builder()
                    .title("Test Tricycle")
                    .price(BigDecimal.valueOf(200000))
                    .seller(seller)
                    .listingType(ListingType.VEHICLE)
                    .vehicleType(VehicleType.TRICYCLE)
                    .category(ListingCategory.TRICYCLE)
                    .status(ListingStatus.ACTIVE)
                    .condition(ListingCondition.GOOD)
                    .state(lagos)
                    .build();
            listingRepository.save(tricycle);

            mockMvc.perform(get("/api/v1/listings")
                            .param("vehicleType", "MOTORCYCLE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].title").value("Test Motorcycle"));
        }

        @Test
        @DisplayName("COMPAT-005: Price range filters still work")
        void priceRangeFilters_stillWork() throws Exception {
            Listing cheapBike = createActiveListing("Cheap Bike", lagos, null, null);
            cheapBike.setPrice(BigDecimal.valueOf(50000));
            listingRepository.save(cheapBike);

            Listing expensiveBike = createActiveListing("Expensive Bike", lagos, null, null);
            expensiveBike.setPrice(BigDecimal.valueOf(500000));
            listingRepository.save(expensiveBike);

            mockMvc.perform(get("/api/v1/listings")
                            .param("minPrice", "40000")
                            .param("maxPrice", "100000"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].title").value("Cheap Bike"));
        }
    }

    // ==================== BROWSE ENDPOINT TESTS ====================

    @Nested
    @DisplayName("Browse Endpoint Tests")
    class BrowseEndpointTests {

        @Test
        @DisplayName("BROWSE-001: Browse with location slug filter works")
        void browseWithLocationSlugFilter_works() throws Exception {
            Listing ikejaListing = createActiveListing("Ikeja Motorcycle", lagos, ikeja, null);
            Listing abujaListing = createActiveListing("Abuja Motorcycle", abuja, null, null);

            mockMvc.perform(get("/api/v1/listings/browse/motorcycles")
                            .param("location", "ikeja"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].title").value("Ikeja Motorcycle"));
        }

        @Test
        @DisplayName("BROWSE-002: Browse with multi-value attribute filter works")
        void browseWithMultiValueAttributeFilter_works() throws Exception {
            AttributeDefinition engineAttr = createTestAttribute("Browse Engine", ListingType.VEHICLE, true,
                    List.of("150cc", "200cc", "250cc"));

            createActiveListingWithAttribute("150cc Motorcycle", engineAttr, "150cc");
            createActiveListingWithAttribute("200cc Motorcycle", engineAttr, "200cc");
            createActiveListingWithAttribute("250cc Motorcycle", engineAttr, "250cc");

            mockMvc.perform(get("/api/v1/listings/browse/motorcycles")
                            .param("attr_browse-engine", "150cc")
                            .param("attr_browse-engine", "200cc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(2));
        }

        @Test
        @DisplayName("BROWSE-003: Browse with path-based location works")
        void browseWithPathBasedLocation_works() throws Exception {
            Listing ikejaListing = createActiveListing("Ikeja Motorcycle", lagos, ikeja, null);
            Listing abujaListing = createActiveListing("Abuja Motorcycle", abuja, null, null);

            mockMvc.perform(get("/api/v1/listings/browse/motorcycles/lagos/ikeja"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].title").value("Ikeja Motorcycle"));
        }

        @Test
        @DisplayName("BROWSE-004: Browse with deep path location (state/axis/area) works")
        void browseWithDeepPathLocation_works() throws Exception {
            Listing opebiListing = createActiveListing("Opebi Motorcycle", lagos, ikeja, opebi);
            Listing victoriaListing = createActiveListing("Victoria Motorcycle", lagos, lekki, victoria);

            mockMvc.perform(get("/api/v1/listings/browse/motorcycles/lagos/ikeja/opebi"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].title").value("Opebi Motorcycle"));
        }
    }

    // ==================== ALL CATEGORY TESTS ====================

    @Nested
    @DisplayName("All Category Tests")
    class AllCategoryTests {

        @Test
        @DisplayName("ALL-001: Browse all returns all listing types")
        void browseAll_returnsAllListingTypes() throws Exception {
            // Create motorcycle
            Listing motorcycle = Listing.builder()
                    .title("Test Motorcycle")
                    .price(BigDecimal.valueOf(100000))
                    .seller(seller)
                    .listingType(ListingType.VEHICLE)
                    .vehicleType(VehicleType.MOTORCYCLE)
                    .category(ListingCategory.MOTORCYCLE)
                    .status(ListingStatus.ACTIVE)
                    .condition(ListingCondition.GOOD)
                    .state(lagos)
                    .build();
            listingRepository.save(motorcycle);

            // Create tricycle
            Listing tricycle = Listing.builder()
                    .title("Test Tricycle")
                    .price(BigDecimal.valueOf(200000))
                    .seller(seller)
                    .listingType(ListingType.VEHICLE)
                    .vehicleType(VehicleType.TRICYCLE)
                    .category(ListingCategory.TRICYCLE)
                    .status(ListingStatus.ACTIVE)
                    .condition(ListingCondition.GOOD)
                    .state(lagos)
                    .build();
            listingRepository.save(tricycle);

            // Create spare part
            Listing part = Listing.builder()
                    .title("Test Brake Pad")
                    .price(BigDecimal.valueOf(5000))
                    .seller(seller)
                    .listingType(ListingType.PART)
                    .category(ListingCategory.SPARE_PART)
                    .status(ListingStatus.ACTIVE)
                    .condition(ListingCondition.NEW)
                    .partName("Brake Pad")
                    .state(lagos)
                    .build();
            listingRepository.save(part);

            mockMvc.perform(get("/api/v1/listings/browse/all"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(3));
        }

        @Test
        @DisplayName("ALL-002: Browse all with location filters correctly")
        void browseAll_withLocationFiltersCorrectly() throws Exception {
            Listing lagosMotorcycle = createActiveListing("Lagos Motorcycle", lagos, null, null);
            Listing abujaMotorcycle = createActiveListing("Abuja Motorcycle", abuja, null, null);

            mockMvc.perform(get("/api/v1/listings/browse/all/lagos"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].title").value("Lagos Motorcycle"));
        }

        @Test
        @DisplayName("ALL-003: Browse all with path-based location works")
        void browseAll_withPathBasedLocation_works() throws Exception {
            Listing ikejaListing = createActiveListing("Ikeja Item", lagos, ikeja, null);
            Listing lekkiListing = createActiveListing("Lekki Item", lagos, lekki, null);

            mockMvc.perform(get("/api/v1/listings/browse/all/lagos/ikeja"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].title").value("Ikeja Item"));
        }

        @Test
        @DisplayName("ALL-004: Browse all with attribute filters works")
        void browseAll_withAttributeFilters_works() throws Exception {
            AttributeDefinition condAttr = createTestAttribute("All Condition", ListingType.VEHICLE, true,
                    List.of("New", "Used"));

            createActiveListingWithAttribute("New Bike", condAttr, "New");
            createActiveListingWithAttribute("Used Bike", condAttr, "Used");

            mockMvc.perform(get("/api/v1/listings/browse/all")
                            .param("attr_all-condition", "New"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].title").value("New Bike"));
        }

        @Test
        @DisplayName("ALL-005: Browse all is case-insensitive")
        void browseAll_isCaseInsensitive() throws Exception {
            createActiveListing("Test Bike", lagos, null, null);

            mockMvc.perform(get("/api/v1/listings/browse/ALL"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1));

            mockMvc.perform(get("/api/v1/listings/browse/All"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1));
        }

        @Test
        @DisplayName("ALL-006: Browse all meta returns correct labels")
        void browseAllMeta_returnsCorrectLabels() throws Exception {
            mockMvc.perform(get("/api/v1/listings/browse/all/meta"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.categoryLabel").value("All Listings"))
                    .andExpect(jsonPath("$.data.locationLabel").value("Nigeria"));
        }

        @Test
        @DisplayName("ALL-007: Browse all meta with location returns correct labels")
        void browseAllMeta_withLocationReturnsCorrectLabels() throws Exception {
            mockMvc.perform(get("/api/v1/listings/browse/all/meta")
                            .param("stateSlug", "lagos"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.categoryLabel").value("All Listings"))
                    .andExpect(jsonPath("$.data.locationLabel").value("Lagos"));
        }

        @Test
        @DisplayName("ALL-008: Browse all meta canonical URL omits all prefix")
        void browseAllMeta_canonicalUrlOmitsAllPrefix() throws Exception {
            mockMvc.perform(get("/api/v1/listings/browse/all/meta")
                            .param("stateSlug", "lagos")
                            .param("axisSlug", "ikeja"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.canonicalUrl").value("/lagos/ikeja"));
        }

        @Test
        @DisplayName("ALL-009: Browse all meta at root has correct canonical URL")
        void browseAllMeta_atRootHasCorrectCanonicalUrl() throws Exception {
            mockMvc.perform(get("/api/v1/listings/browse/all/meta"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.canonicalUrl").value("/"));
        }
    }

    // ==================== TEXT SEARCH TESTS ====================

    @Nested
    @DisplayName("Text Search Tests")
    class TextSearchTests {

        @Test
        @DisplayName("SEARCH-001: Search by title returns matching results")
        void searchByTitle_returnsMatchingResults() throws Exception {
            createActiveListing("Honda CB300R Tokunbo", lagos, null, null);
            createActiveListing("Bajaj Boxer 100", lagos, null, null);
            createActiveListing("TVS Apache RTR", lagos, null, null);

            mockMvc.perform(get("/api/v1/listings")
                            .param("q", "honda"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].title").value("Honda CB300R Tokunbo"));
        }

        @Test
        @DisplayName("SEARCH-002: Search is case-insensitive")
        void search_isCaseInsensitive() throws Exception {
            createActiveListing("HONDA CB500F", lagos, null, null);

            mockMvc.perform(get("/api/v1/listings")
                            .param("q", "honda"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1));

            mockMvc.perform(get("/api/v1/listings")
                            .param("q", "HONDA"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1));
        }

        @Test
        @DisplayName("SEARCH-003: Search supports partial matching")
        void search_supportsPartialMatching() throws Exception {
            createActiveListing("Honda CB300R Tokunbo", lagos, null, null);

            mockMvc.perform(get("/api/v1/listings")
                            .param("q", "hon"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1));

            mockMvc.perform(get("/api/v1/listings")
                            .param("q", "300"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1));
        }

        @Test
        @DisplayName("SEARCH-004: Search with location filter works")
        void search_withLocationFilter_works() throws Exception {
            createActiveListing("Honda CB300R Lagos", lagos, null, null);
            createActiveListing("Honda CB300R Abuja", abuja, null, null);

            mockMvc.perform(get("/api/v1/listings")
                            .param("q", "honda")
                            .param("location", "lagos"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].title").value("Honda CB300R Lagos"));
        }

        @Test
        @DisplayName("SEARCH-005: Search with attribute filter works")
        void search_withAttributeFilter_works() throws Exception {
            AttributeDefinition condAttr = createTestAttribute("Search Condition", ListingType.VEHICLE, true,
                    List.of("New", "Used"));

            createActiveListingWithAttribute("Honda New Bike", condAttr, "New");
            createActiveListingWithAttribute("Honda Used Bike", condAttr, "Used");
            createActiveListingWithAttribute("Bajaj New Bike", condAttr, "New");

            mockMvc.perform(get("/api/v1/listings")
                            .param("q", "honda")
                            .param("attr_search-condition", "New"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].title").value("Honda New Bike"));
        }

        @Test
        @DisplayName("SEARCH-006: Search with empty query returns all results")
        void search_withEmptyQuery_returnsAllResults() throws Exception {
            createActiveListing("Honda CB300R", lagos, null, null);
            createActiveListing("Bajaj Boxer 100", lagos, null, null);

            mockMvc.perform(get("/api/v1/listings")
                            .param("q", ""))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(2));
        }

        @Test
        @DisplayName("SEARCH-007: Search with query less than 2 chars returns all results")
        void search_withShortQuery_returnsAllResults() throws Exception {
            createActiveListing("Honda CB300R", lagos, null, null);
            createActiveListing("Bajaj Boxer 100", lagos, null, null);

            mockMvc.perform(get("/api/v1/listings")
                            .param("q", "h"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(2));
        }

        @Test
        @DisplayName("SEARCH-008: Search on browse endpoint works")
        void search_onBrowseEndpoint_works() throws Exception {
            createActiveListing("Honda Motorcycle", lagos, null, null);
            createActiveListing("Bajaj Motorcycle", lagos, null, null);

            mockMvc.perform(get("/api/v1/listings/browse/motorcycles")
                            .param("q", "honda"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].title").value("Honda Motorcycle"));
        }

        @Test
        @DisplayName("SEARCH-009: Search on browse with path location works")
        void search_onBrowseWithPathLocation_works() throws Exception {
            Listing ikejaHonda = createActiveListing("Honda Ikeja", lagos, ikeja, null);
            Listing abujaHonda = createActiveListing("Honda Abuja", abuja, null, null);

            mockMvc.perform(get("/api/v1/listings/browse/motorcycles/lagos/ikeja")
                            .param("q", "honda"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].title").value("Honda Ikeja"));
        }

        @Test
        @DisplayName("SEARCH-010: Search on all category works")
        void search_onAllCategory_works() throws Exception {
            createActiveListing("Honda Motorcycle", lagos, null, null);

            Listing part = Listing.builder()
                    .title("Honda Brake Pad")
                    .price(BigDecimal.valueOf(5000))
                    .seller(seller)
                    .listingType(ListingType.PART)
                    .category(ListingCategory.SPARE_PART)
                    .status(ListingStatus.ACTIVE)
                    .condition(ListingCondition.NEW)
                    .partName("Brake Pad")
                    .state(lagos)
                    .build();
            listingRepository.save(part);

            createActiveListing("Bajaj Tricycle", lagos, null, null);

            mockMvc.perform(get("/api/v1/listings/browse/all")
                            .param("q", "honda"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(2));
        }

        @Test
        @DisplayName("SEARCH-011: Combined search with location and all category")
        void combinedSearch_withLocationAndAllCategory() throws Exception {
            Listing lagosHonda = createActiveListing("Honda Lagos", lagos, ikeja, null);
            Listing abujaHonda = createActiveListing("Honda Abuja", abuja, null, null);

            mockMvc.perform(get("/api/v1/listings/browse/all/lagos")
                            .param("q", "honda"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].title").value("Honda Lagos"));
        }

        @Test
        @DisplayName("SEARCH-012: Search returns no results for non-matching query")
        void search_returnsNoResults_forNonMatchingQuery() throws Exception {
            createActiveListing("Honda CB300R", lagos, null, null);

            mockMvc.perform(get("/api/v1/listings")
                            .param("q", "yamaha"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(0));
        }
    }

    // ==================== HELPER METHODS ====================

    private Listing createActiveListing(String title, State state, Axis axis, Area area) {
        Listing listing = Listing.builder()
                .title(title)
                .price(BigDecimal.valueOf(150000))
                .seller(seller)
                .listingType(ListingType.VEHICLE)
                .vehicleType(VehicleType.MOTORCYCLE)
                .category(ListingCategory.MOTORCYCLE)
                .status(ListingStatus.ACTIVE)
                .condition(ListingCondition.GOOD)
                .state(state)
                .axis(axis)
                .area(area)
                .build();
        return listingRepository.save(listing);
    }

    private Listing createActiveListingWithAttribute(String title, AttributeDefinition attr, String value) {
        Listing listing = createActiveListing(title, lagos, null, null);

        ListingAttributeValue attrValue = ListingAttributeValue.builder()
                .listing(listing)
                .attribute(attr)
                .value(value)
                .build();
        listing.getAttributes().add(attrValue);

        return listingRepository.save(listing);
    }

    private Listing createActiveListingWithAttributes(String title, List<AttributeDefinition> attrs, List<String> values) {
        Listing listing = createActiveListing(title, lagos, null, null);

        for (int i = 0; i < attrs.size(); i++) {
            ListingAttributeValue attrValue = ListingAttributeValue.builder()
                    .listing(listing)
                    .attribute(attrs.get(i))
                    .value(values.get(i))
                    .build();
            listing.getAttributes().add(attrValue);
        }

        return listingRepository.save(listing);
    }

    private Listing createActiveListingWithAttributeAndLocation(
            String title, State state, Axis axis, Area area,
            AttributeDefinition attr, String value) {
        Listing listing = createActiveListing(title, state, axis, area);

        ListingAttributeValue attrValue = ListingAttributeValue.builder()
                .listing(listing)
                .attribute(attr)
                .value(value)
                .build();
        listing.getAttributes().add(attrValue);

        return listingRepository.save(listing);
    }
}
