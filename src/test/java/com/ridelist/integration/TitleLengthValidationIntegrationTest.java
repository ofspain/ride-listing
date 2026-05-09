package com.ridelist.integration;

import com.ridelist.dto.request.CreateListingRequest;
import com.ridelist.dto.request.UpdateListingRequest;
import com.ridelist.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Title Length Validation Integration Tests")
public class TitleLengthValidationIntegrationTest extends BaseIntegrationTest {

    private String generateTitle(int length) {
        return "A".repeat(length);
    }

    @Nested
    @DisplayName("Create Listing Title Validation")
    class CreateListingTitleTests {

        @Test
        @DisplayName("TITLE-001: Create listing with 44-char title returns 400 with guidance message")
        void createListing_44CharTitle_returns400() throws Exception {
            String token = registerAndGetToken("seller@test.com", "Password123!");
            State state = createTestState("Lagos");

            CreateListingRequest request = CreateListingRequest.builder()
                    .title(generateTitle(44))
                    .description("Well maintained vehicle")
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
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.data.title").value("Title must be between 45 and 150 characters. Include make, model, year, condition and location for best results."));
        }

        @Test
        @DisplayName("TITLE-002: Create listing with 45-char title returns 201 success")
        void createListing_45CharTitle_returnsCreated() throws Exception {
            String token = registerAndGetToken("seller@test.com", "Password123!");
            State state = createTestState("Lagos");

            String title = generateTitle(45);
            CreateListingRequest request = CreateListingRequest.builder()
                    .title(title)
                    .description("Well maintained vehicle")
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
                    .andExpect(jsonPath("$.data.title").value(title));
        }

        @Test
        @DisplayName("TITLE-003: Create listing with 150-char title returns 201 success")
        void createListing_150CharTitle_returnsCreated() throws Exception {
            String token = registerAndGetToken("seller@test.com", "Password123!");
            State state = createTestState("Lagos");

            String title = generateTitle(150);
            CreateListingRequest request = CreateListingRequest.builder()
                    .title(title)
                    .description("Well maintained vehicle")
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
                    .andExpect(jsonPath("$.data.title").value(title));
        }

        @Test
        @DisplayName("TITLE-004: Create listing with 151-char title returns 400")
        void createListing_151CharTitle_returns400() throws Exception {
            String token = registerAndGetToken("seller@test.com", "Password123!");
            State state = createTestState("Lagos");

            CreateListingRequest request = CreateListingRequest.builder()
                    .title(generateTitle(151))
                    .description("Well maintained vehicle")
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
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.data.title").exists());
        }
    }

    @Nested
    @DisplayName("Update Listing Title Validation")
    class UpdateListingTitleTests {

        @Test
        @DisplayName("TITLE-005: Update listing with null title has no validation error (optional field)")
        void updateListing_nullTitle_noValidationError() throws Exception {
            String token = registerAndGetToken("seller@test.com", "Password123!");
            State state = createTestState("Lagos");
            User seller = userRepository.findByEmail("seller@test.com").orElseThrow();
            Listing listing = createTestListing(seller, ListingType.VEHICLE);

            UpdateListingRequest request = UpdateListingRequest.builder()
                    .title(null)
                    .description("Updated description")
                    .build();

            mockMvc.perform(put("/api/v1/account/listings/" + listing.getId())
                            .header("Authorization", authHeader(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("TITLE-006: Update listing with 44-char title returns 400")
        void updateListing_44CharTitle_returns400() throws Exception {
            String token = registerAndGetToken("seller@test.com", "Password123!");
            State state = createTestState("Lagos");
            User seller = userRepository.findByEmail("seller@test.com").orElseThrow();
            Listing listing = createTestListing(seller, ListingType.VEHICLE);

            UpdateListingRequest request = UpdateListingRequest.builder()
                    .title(generateTitle(44))
                    .build();

            mockMvc.perform(put("/api/v1/account/listings/" + listing.getId())
                            .header("Authorization", authHeader(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.data.title").value("Title must be between 45 and 150 characters."));
        }
    }
}
