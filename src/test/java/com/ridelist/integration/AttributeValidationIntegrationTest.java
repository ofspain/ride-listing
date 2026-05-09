package com.ridelist.integration;

import com.ridelist.dto.request.AttributeValueRequest;
import com.ridelist.dto.request.CreateListingRequest;
import com.ridelist.dto.request.UpdateListingRequest;
import com.ridelist.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for attribute value validation on listings.
 * Tests the acceptableValues enforcement and listingType membership checks.
 */
@DisplayName("Attribute Validation Integration Tests")
class AttributeValidationIntegrationTest extends BaseIntegrationTest {

    private String sellerToken;
    private State testState;

    @BeforeEach
    void setup() throws Exception {
        sellerToken = registerAndGetToken("seller@test.com", "password123");
        testState = createTestState("Lagos");
    }

    @Nested
    @DisplayName("Listing Save with Attribute Validation")
    class ListingSaveAttributeValidation {

        @Test
        @DisplayName("ATTR-VAL-001: Create listing with valid attribute value succeeds")
        void createListingWithValidAttributeValue_Succeeds() throws Exception {
            AttributeDefinition attr = createTestAttribute("Engine Type", ListingType.VEHICLE, true,
                    List.of("150cc", "200cc", "250cc", "350cc"));

            CreateListingRequest request = CreateListingRequest.builder()
                    .title("TVS Apache RTR 200 Sports Motorcycle Lagos Nigeria 2024")
                    .description("Well maintained bike")
                    .price(BigDecimal.valueOf(250000))
                    .listingType(ListingType.VEHICLE)
                    .vehicleType(VehicleType.MOTORCYCLE)
                    .category(ListingCategory.MOTORCYCLE)
                    .condition(ListingCondition.GOOD)
                    .stateId(testState.getId())
                    .attributes(List.of(
                            AttributeValueRequest.builder()
                                    .attributeId(attr.getId())
                                    .value("200cc")
                                    .build()
                    ))
                    .build();

            mockMvc.perform(post("/api/v1/account/listings")
                            .header("Authorization", authHeader(sellerToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.status").value("DRAFT"));
        }

        @Test
        @DisplayName("ATTR-VAL-002: Create listing with invalid attribute value fails with 400")
        void createListingWithInvalidAttributeValue_ReturnsBadRequest() throws Exception {
            AttributeDefinition attr = createTestAttribute("Engine Type", ListingType.VEHICLE, true,
                    List.of("150cc", "200cc", "250cc", "350cc"));

            CreateListingRequest request = CreateListingRequest.builder()
                    .title("TVS Apache RTR 200 Sports Motorcycle Lagos Nigeria 2024")
                    .description("Well maintained bike")
                    .price(BigDecimal.valueOf(250000))
                    .listingType(ListingType.VEHICLE)
                    .vehicleType(VehicleType.MOTORCYCLE)
                    .category(ListingCategory.MOTORCYCLE)
                    .condition(ListingCondition.GOOD)
                    .stateId(testState.getId())
                    .attributes(List.of(
                            AttributeValueRequest.builder()
                                    .attributeId(attr.getId())
                                    .value("500cc") // Not in acceptable values
                                    .build()
                    ))
                    .build();

            mockMvc.perform(post("/api/v1/account/listings")
                            .header("Authorization", authHeader(sellerToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(
                            org.hamcrest.Matchers.containsString("Invalid value '500cc'")));
        }

        @Test
        @DisplayName("ATTR-VAL-003: Create listing with attribute of wrong listing type fails with 400")
        void createListingWithWrongListingTypeAttribute_ReturnsBadRequest() throws Exception {
            // Create a PART-only attribute
            AttributeDefinition partAttr = createTestAttribute("Compatibility", ListingType.PART, true,
                    List.of("Honda", "TVS", "Universal"));

            // Try to use it on a VEHICLE listing
            CreateListingRequest request = CreateListingRequest.builder()
                    .title("TVS Apache RTR 200 Sports Motorcycle Lagos Nigeria 2024")
                    .description("Well maintained bike")
                    .price(BigDecimal.valueOf(250000))
                    .listingType(ListingType.VEHICLE)
                    .vehicleType(VehicleType.MOTORCYCLE)
                    .category(ListingCategory.MOTORCYCLE)
                    .condition(ListingCondition.GOOD)
                    .stateId(testState.getId())
                    .attributes(List.of(
                            AttributeValueRequest.builder()
                                    .attributeId(partAttr.getId())
                                    .value("Honda")
                                    .build()
                    ))
                    .build();

            mockMvc.perform(post("/api/v1/account/listings")
                            .header("Authorization", authHeader(sellerToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(
                            org.hamcrest.Matchers.containsString("does not apply to VEHICLE listings")));
        }

        @Test
        @DisplayName("ATTR-VAL-004: Attribute applicable to both VEHICLE and PART works on VEHICLE")
        void attributeApplicableToBothTypes_WorksOnVehicle() throws Exception {
            // Create attribute applicable to both types
            AttributeDefinition brandAttr = AttributeDefinition.builder()
                    .name("Brand")
                    .slug("brand")
                    .listingTypes(Set.of(ListingType.VEHICLE, ListingType.PART))
                    .acceptableValues(List.of("Honda", "TVS", "Bajaj"))
                    .filterable(true)
                    .active(true)
                    .build();
            brandAttr = attributeRepository.save(brandAttr);

            CreateListingRequest request = CreateListingRequest.builder()
                    .title("Honda CB Shine Commuter Motorcycle 2024 Lagos Nigeria")
                    .description("Reliable commuter bike")
                    .price(BigDecimal.valueOf(120000))
                    .listingType(ListingType.VEHICLE)
                    .vehicleType(VehicleType.MOTORCYCLE)
                    .category(ListingCategory.MOTORCYCLE)
                    .condition(ListingCondition.GOOD)
                    .stateId(testState.getId())
                    .attributes(List.of(
                            AttributeValueRequest.builder()
                                    .attributeId(brandAttr.getId())
                                    .value("Honda")
                                    .build()
                    ))
                    .build();

            mockMvc.perform(post("/api/v1/account/listings")
                            .header("Authorization", authHeader(sellerToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("ATTR-VAL-005: Attribute applicable to both VEHICLE and PART works on PART")
        void attributeApplicableToBothTypes_WorksOnPart() throws Exception {
            // Create attribute applicable to both types
            AttributeDefinition brandAttr = AttributeDefinition.builder()
                    .name("Brand For Parts")
                    .slug("brand-for-parts")
                    .listingTypes(Set.of(ListingType.VEHICLE, ListingType.PART))
                    .acceptableValues(List.of("Honda", "TVS", "Universal"))
                    .filterable(true)
                    .active(true)
                    .build();
            brandAttr = attributeRepository.save(brandAttr);

            CreateListingRequest request = CreateListingRequest.builder()
                    .title("Honda Brake Pads Original Front Rear Set For CBR")
                    .description("Original Honda brake pads")
                    .price(BigDecimal.valueOf(5000))
                    .listingType(ListingType.PART)
                    .partName("Brake Pads")
                    .category(ListingCategory.SPARE_PART)
                    .condition(ListingCondition.NEW)
                    .stateId(testState.getId())
                    .attributes(List.of(
                            AttributeValueRequest.builder()
                                    .attributeId(brandAttr.getId())
                                    .value("Honda")
                                    .build()
                    ))
                    .build();

            mockMvc.perform(post("/api/v1/account/listings")
                            .header("Authorization", authHeader(sellerToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("ATTR-VAL-006: Update listing with invalid attribute value fails")
        void updateListingWithInvalidAttributeValue_ReturnsBadRequest() throws Exception {
            User seller = userRepository.findByEmail("seller@test.com").orElseThrow();
            Listing listing = createTestListing(seller, ListingType.VEHICLE);

            AttributeDefinition attr = createTestAttribute("Fuel Type", ListingType.VEHICLE, true,
                    List.of("Petrol", "Electric", "Hybrid"));

            UpdateListingRequest request = UpdateListingRequest.builder()
                    .attributes(List.of(
                            AttributeValueRequest.builder()
                                    .attributeId(attr.getId())
                                    .value("Diesel") // Not in acceptable values
                                    .build()
                    ))
                    .build();

            mockMvc.perform(put("/api/v1/account/listings/{id}", listing.getId())
                            .header("Authorization", authHeader(sellerToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(
                            org.hamcrest.Matchers.containsString("Invalid value 'Diesel'")));
        }

        @Test
        @DisplayName("ATTR-VAL-007: Update listing with valid attribute value succeeds")
        void updateListingWithValidAttributeValue_Succeeds() throws Exception {
            User seller = userRepository.findByEmail("seller@test.com").orElseThrow();
            Listing listing = createTestListing(seller, ListingType.VEHICLE);

            AttributeDefinition attr = createTestAttribute("Fuel Type", ListingType.VEHICLE, true,
                    List.of("Petrol", "Electric", "Hybrid"));

            UpdateListingRequest request = UpdateListingRequest.builder()
                    .attributes(List.of(
                            AttributeValueRequest.builder()
                                    .attributeId(attr.getId())
                                    .value("Petrol")
                                    .build()
                    ))
                    .build();

            mockMvc.perform(put("/api/v1/account/listings/{id}", listing.getId())
                            .header("Authorization", authHeader(sellerToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("Filter by Attribute with Invalid Values")
    class FilterByAttributeInvalidValues {

        @Test
        @DisplayName("ATTR-FILTER-001: Filter by valid attribute value returns matching listings")
        void filterByValidAttributeValue_ReturnsMatchingListings() throws Exception {
            // Create attribute
            AttributeDefinition engineAttr = createTestAttribute("Engine CC", ListingType.VEHICLE, true,
                    List.of("150cc", "200cc", "250cc"));

            // Create seller and listing with attribute
            User seller = userRepository.findByEmail("seller@test.com").orElseThrow();
            Listing listing = Listing.builder()
                    .title("TVS Apache 200 Sports Motorcycle 2024 Lagos Nigeria")
                    .description("Test bike")
                    .price(BigDecimal.valueOf(200000))
                    .seller(seller)
                    .listingType(ListingType.VEHICLE)
                    .vehicleType(VehicleType.MOTORCYCLE)
                    .category(ListingCategory.MOTORCYCLE)
                    .condition(ListingCondition.GOOD)
                    .status(ListingStatus.ACTIVE)
                    .state(testState)
                    .build();
            listing = listingRepository.save(listing);

            // Add attribute value to listing
            ListingAttributeValue attrValue = ListingAttributeValue.builder()
                    .listing(listing)
                    .attribute(engineAttr)
                    .value("200cc")
                    .build();
            listing.getAttributes().add(attrValue);
            listingRepository.save(listing);

            // Filter by attribute
            mockMvc.perform(get("/api/v1/listings")
                            .param("attr_engine-cc", "200cc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].title").value("TVS Apache 200 Sports Motorcycle 2024 Lagos Nigeria"));
        }

        @Test
        @DisplayName("ATTR-FILTER-002: Filter by invalid attribute value returns empty results (no error)")
        void filterByInvalidAttributeValue_ReturnsEmptyResults() throws Exception {
            // Create attribute
            AttributeDefinition engineAttr = createTestAttribute("Engine CC Filter", ListingType.VEHICLE, true,
                    List.of("150cc", "200cc", "250cc"));

            // Create seller and listing with attribute
            User seller = userRepository.findByEmail("seller@test.com").orElseThrow();
            Listing listing = Listing.builder()
                    .title("TVS Apache 200 Filter Test Sports Motorcycle Lagos 2024")
                    .description("Test bike")
                    .price(BigDecimal.valueOf(200000))
                    .seller(seller)
                    .listingType(ListingType.VEHICLE)
                    .vehicleType(VehicleType.MOTORCYCLE)
                    .category(ListingCategory.MOTORCYCLE)
                    .condition(ListingCondition.GOOD)
                    .status(ListingStatus.ACTIVE)
                    .state(testState)
                    .build();
            listing = listingRepository.save(listing);

            // Add attribute value
            ListingAttributeValue attrValue = ListingAttributeValue.builder()
                    .listing(listing)
                    .attribute(engineAttr)
                    .value("200cc")
                    .build();
            listing.getAttributes().add(attrValue);
            listingRepository.save(listing);

            // Filter by invalid value - should return empty (not an error)
            // Since "500cc" was never saved to any listing, no results match
            mockMvc.perform(get("/api/v1/listings")
                            .param("attr_engine-cc-filter", "500cc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(0));
        }
    }

    @Nested
    @DisplayName("Public Attribute Endpoint Response")
    class PublicAttributeEndpointResponse {

        @Test
        @DisplayName("Public endpoint returns acceptableValues for frontend dropdown")
        void publicEndpointReturnsAcceptableValues() throws Exception {
            createTestAttribute("Engine Type Public", ListingType.VEHICLE, true,
                    List.of("150cc", "200cc", "250cc", "350cc", "400cc+"));

            mockMvc.perform(get("/api/v1/attributes")
                            .param("listingType", "VEHICLE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].acceptableValues").isArray())
                    .andExpect(jsonPath("$.data[0].acceptableValues.length()").value(5))
                    .andExpect(jsonPath("$.data[0].acceptableValues[0]").value("150cc"));
        }

        @Test
        @DisplayName("Public endpoint returns iconUrl")
        void publicEndpointReturnsIconUrl() throws Exception {
            AttributeDefinition attr = AttributeDefinition.builder()
                    .name("Fuel Type Icon")
                    .slug("fuel-type-icon")
                    .listingTypes(Set.of(ListingType.VEHICLE))
                    .iconUrl("https://cdn.ridelist.ng/icons/fuel.svg")
                    .acceptableValues(List.of("Petrol", "Electric"))
                    .filterable(true)
                    .active(true)
                    .build();
            attributeRepository.save(attr);

            mockMvc.perform(get("/api/v1/attributes")
                            .param("listingType", "VEHICLE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[?(@.slug=='fuel-type-icon')].iconUrl")
                            .value("https://cdn.ridelist.ng/icons/fuel.svg"));
        }

        @Test
        @DisplayName("Public endpoint returns listingTypes set")
        void publicEndpointReturnsListingTypesSet() throws Exception {
            AttributeDefinition attr = AttributeDefinition.builder()
                    .name("Universal Brand")
                    .slug("universal-brand")
                    .listingTypes(Set.of(ListingType.VEHICLE, ListingType.PART))
                    .acceptableValues(List.of("Honda", "TVS"))
                    .filterable(true)
                    .active(true)
                    .build();
            attributeRepository.save(attr);

            mockMvc.perform(get("/api/v1/attributes")
                            .param("listingType", "VEHICLE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[?(@.slug=='universal-brand')].listingTypes").isArray());
        }

        @Test
        @DisplayName("Filterable endpoint returns attributes with acceptableValues")
        void filterableEndpointReturnsAcceptableValues() throws Exception {
            createTestAttribute("Filterable Engine", ListingType.VEHICLE, true,
                    List.of("150cc", "200cc"));

            mockMvc.perform(get("/api/v1/attributes/filterable")
                            .param("listingType", "VEHICLE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].acceptableValues").isArray())
                    .andExpect(jsonPath("$.data[0].filterable").value(true));
        }
    }
}
