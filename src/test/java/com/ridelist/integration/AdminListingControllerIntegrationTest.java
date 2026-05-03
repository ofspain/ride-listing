package com.ridelist.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.ridelist.dto.request.ChangeListingStatusRequest;
import com.ridelist.dto.response.ApiResponse;
import com.ridelist.dto.response.ListingResponse;
import com.ridelist.dto.response.ListingSummaryResponse;
import com.ridelist.dto.response.PagedResponse;
import com.ridelist.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminListingControllerIntegrationTest extends BaseIntegrationTest {

    private String adminToken;
    private String userToken;
    private User adminUser;
    private User regularUser;
    private User seller;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = registerAndGetToken("admin@test.com", "password123");
        adminUser = userRepository.findByEmail("admin@test.com").orElseThrow();
        adminUser.setRole(Role.ADMIN);
        userRepository.save(adminUser);
        adminToken = loginAndGetToken("admin@test.com", "password123");

        userToken = registerAndGetToken("user@test.com", "password123");
        regularUser = userRepository.findByEmail("user@test.com").orElseThrow();

        seller = createTestUser("seller@test.com", Role.USER);
        seller.setFirstName("John");
        seller.setLastName("Seller");
        userRepository.save(seller);
    }

    @Nested
    @DisplayName("GET /api/v1/admin/listings")
    class GetListingsTests {

        @Test
        @DisplayName("Should return all listings for admin")
        void shouldReturnAllListingsForAdmin() throws Exception {
            createTestListing(seller, ListingType.VEHICLE);
            createTestListing(seller, ListingType.PART);

            MvcResult result = mockMvc.perform(get("/api/v1/admin/listings")
                            .header("Authorization", authHeader(adminToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andReturn();

            ApiResponse<PagedResponse<ListingSummaryResponse>> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {});

            assertThat(response.getData().getContent()).hasSize(2);
        }

        @Test
        @DisplayName("Should filter listings by status")
        void shouldFilterByStatus() throws Exception {
            Listing draft = createTestListing(seller, ListingType.VEHICLE);
            Listing active = createTestListing(seller, ListingType.VEHICLE);
            active.setStatus(ListingStatus.ACTIVE);
            listingRepository.save(active);

            MvcResult result = mockMvc.perform(get("/api/v1/admin/listings")
                            .param("status", "ACTIVE")
                            .header("Authorization", authHeader(adminToken)))
                    .andExpect(status().isOk())
                    .andReturn();

            ApiResponse<PagedResponse<ListingSummaryResponse>> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {});

            assertThat(response.getData().getContent()).hasSize(1);
            assertThat(response.getData().getContent().get(0).getStatus()).isEqualTo(ListingStatus.ACTIVE);
        }

        @Test
        @DisplayName("Should filter listings by listingType")
        void shouldFilterByListingType() throws Exception {
            createTestListing(seller, ListingType.VEHICLE);
            createTestListing(seller, ListingType.PART);

            MvcResult result = mockMvc.perform(get("/api/v1/admin/listings")
                            .param("listingType", "VEHICLE")
                            .header("Authorization", authHeader(adminToken)))
                    .andExpect(status().isOk())
                    .andReturn();

            ApiResponse<PagedResponse<ListingSummaryResponse>> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {});

            assertThat(response.getData().getContent()).hasSize(1);
            assertThat(response.getData().getContent().get(0).getListingType()).isEqualTo(ListingType.VEHICLE);
        }

        @Test
        @DisplayName("Should filter listings by category")
        void shouldFilterByCategory() throws Exception {
            createTestListing(seller, ListingType.VEHICLE);
            createTestListing(seller, ListingType.PART);

            MvcResult result = mockMvc.perform(get("/api/v1/admin/listings")
                            .param("category", "MOTORCYCLE")
                            .header("Authorization", authHeader(adminToken)))
                    .andExpect(status().isOk())
                    .andReturn();

            ApiResponse<PagedResponse<ListingSummaryResponse>> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {});

            assertThat(response.getData().getContent()).hasSize(1);
            assertThat(response.getData().getContent().get(0).getCategory()).isEqualTo(ListingCategory.MOTORCYCLE);
        }

        @Test
        @DisplayName("Should search listings by title")
        void shouldSearchByTitle() throws Exception {
            Listing listing1 = createTestListing(seller, ListingType.VEHICLE);
            listing1.setTitle("Honda CB500");
            listingRepository.save(listing1);

            Listing listing2 = createTestListing(seller, ListingType.VEHICLE);
            listing2.setTitle("Yamaha R15");
            listingRepository.save(listing2);

            MvcResult result = mockMvc.perform(get("/api/v1/admin/listings")
                            .param("search", "honda")
                            .header("Authorization", authHeader(adminToken)))
                    .andExpect(status().isOk())
                    .andReturn();

            ApiResponse<PagedResponse<ListingSummaryResponse>> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {});

            assertThat(response.getData().getContent()).hasSize(1);
            assertThat(response.getData().getContent().get(0).getTitle()).isEqualTo("Honda CB500");
        }

        @Test
        @DisplayName("Should search listings by seller name")
        void shouldSearchBySellerName() throws Exception {
            createTestListing(seller, ListingType.VEHICLE);

            MvcResult result = mockMvc.perform(get("/api/v1/admin/listings")
                            .param("search", "John")
                            .header("Authorization", authHeader(adminToken)))
                    .andExpect(status().isOk())
                    .andReturn();

            ApiResponse<PagedResponse<ListingSummaryResponse>> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {});

            assertThat(response.getData().getContent()).hasSize(1);
        }

        @Test
        @DisplayName("Admin can see DELETED listings")
        void adminCanSeeDeletedListings() throws Exception {
            Listing deleted = createTestListing(seller, ListingType.VEHICLE);
            deleted.setStatus(ListingStatus.DELETED);
            listingRepository.save(deleted);

            MvcResult result = mockMvc.perform(get("/api/v1/admin/listings")
                            .param("status", "DELETED")
                            .header("Authorization", authHeader(adminToken)))
                    .andExpect(status().isOk())
                    .andReturn();

            ApiResponse<PagedResponse<ListingSummaryResponse>> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {});

            assertThat(response.getData().getContent()).hasSize(1);
            assertThat(response.getData().getContent().get(0).getStatus()).isEqualTo(ListingStatus.DELETED);
        }

        @Test
        @DisplayName("Should support pagination")
        void shouldSupportPagination() throws Exception {
            for (int i = 0; i < 25; i++) {
                createTestListing(seller, ListingType.VEHICLE);
            }

            MvcResult result = mockMvc.perform(get("/api/v1/admin/listings")
                            .param("page", "0")
                            .param("size", "10")
                            .header("Authorization", authHeader(adminToken)))
                    .andExpect(status().isOk())
                    .andReturn();

            ApiResponse<PagedResponse<ListingSummaryResponse>> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {});

            assertThat(response.getData().getContent()).hasSize(10);
            assertThat(response.getData().getTotalElements()).isEqualTo(25);
            assertThat(response.getData().getTotalPages()).isEqualTo(3);
        }

        @Test
        @DisplayName("Non-admin should get 403")
        void nonAdminShouldGet403() throws Exception {
            mockMvc.perform(get("/api/v1/admin/listings")
                            .header("Authorization", authHeader(userToken)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Unauthenticated should get 401")
        void unauthenticatedShouldGet401() throws Exception {
            mockMvc.perform(get("/api/v1/admin/listings"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/admin/listings/{id}/status")
    class ChangeStatusTests {

        @Test
        @DisplayName("Admin can change listing status from DRAFT to ACTIVE")
        void adminCanChangeStatusFromDraftToActive() throws Exception {
            Listing listing = createTestListing(seller, ListingType.VEHICLE);

            ChangeListingStatusRequest request = ChangeListingStatusRequest.builder()
                    .status(ListingStatus.ACTIVE)
                    .build();

            MvcResult result = mockMvc.perform(put("/api/v1/admin/listings/{id}/status", listing.getId())
                            .header("Authorization", authHeader(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andReturn();

            ApiResponse<ListingResponse> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {});

            assertThat(response.getData().getStatus()).isEqualTo(ListingStatus.ACTIVE);
        }

        @Test
        @DisplayName("Admin can change listing status from ACTIVE to SOLD")
        void adminCanChangeStatusFromActiveToSold() throws Exception {
            Listing listing = createTestListing(seller, ListingType.VEHICLE);
            listing.setStatus(ListingStatus.ACTIVE);
            listingRepository.save(listing);

            ChangeListingStatusRequest request = ChangeListingStatusRequest.builder()
                    .status(ListingStatus.SOLD)
                    .build();

            mockMvc.perform(put("/api/v1/admin/listings/{id}/status", listing.getId())
                            .header("Authorization", authHeader(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("SOLD"));
        }

        @Test
        @DisplayName("Admin can change listing status from EXPIRED to ACTIVE")
        void adminCanChangeStatusFromExpiredToActive() throws Exception {
            Listing listing = createTestListing(seller, ListingType.VEHICLE);
            listing.setStatus(ListingStatus.EXPIRED);
            listingRepository.save(listing);

            ChangeListingStatusRequest request = ChangeListingStatusRequest.builder()
                    .status(ListingStatus.ACTIVE)
                    .build();

            mockMvc.perform(put("/api/v1/admin/listings/{id}/status", listing.getId())
                            .header("Authorization", authHeader(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("ACTIVE"));
        }

        @Test
        @DisplayName("Invalid status transition returns 400")
        void invalidTransitionReturns400() throws Exception {
            Listing listing = createTestListing(seller, ListingType.VEHICLE);
            listing.setStatus(ListingStatus.DELETED);
            listingRepository.save(listing);

            ChangeListingStatusRequest request = ChangeListingStatusRequest.builder()
                    .status(ListingStatus.ACTIVE)
                    .build();

            mockMvc.perform(put("/api/v1/admin/listings/{id}/status", listing.getId())
                            .header("Authorization", authHeader(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Cannot transition from DELETED to ACTIVE"));
        }

        @Test
        @DisplayName("Cannot transition from SOLD to ACTIVE")
        void cannotTransitionFromSoldToActive() throws Exception {
            Listing listing = createTestListing(seller, ListingType.VEHICLE);
            listing.setStatus(ListingStatus.SOLD);
            listingRepository.save(listing);

            ChangeListingStatusRequest request = ChangeListingStatusRequest.builder()
                    .status(ListingStatus.ACTIVE)
                    .build();

            mockMvc.perform(put("/api/v1/admin/listings/{id}/status", listing.getId())
                            .header("Authorization", authHeader(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Cannot transition from SOLD to ACTIVE"));
        }

        @Test
        @DisplayName("Listing not found returns 404")
        void listingNotFoundReturns404() throws Exception {
            ChangeListingStatusRequest request = ChangeListingStatusRequest.builder()
                    .status(ListingStatus.ACTIVE)
                    .build();

            mockMvc.perform(put("/api/v1/admin/listings/{id}/status", UUID.randomUUID())
                            .header("Authorization", authHeader(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Non-admin cannot change status")
        void nonAdminCannotChangeStatus() throws Exception {
            Listing listing = createTestListing(seller, ListingType.VEHICLE);

            ChangeListingStatusRequest request = ChangeListingStatusRequest.builder()
                    .status(ListingStatus.ACTIVE)
                    .build();

            mockMvc.perform(put("/api/v1/admin/listings/{id}/status", listing.getId())
                            .header("Authorization", authHeader(userToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Null status returns 400 validation error")
        void nullStatusReturns400() throws Exception {
            Listing listing = createTestListing(seller, ListingType.VEHICLE);

            mockMvc.perform(put("/api/v1/admin/listings/{id}/status", listing.getId())
                            .header("Authorization", authHeader(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/admin/listings/{id}")
    class DeleteListingTests {

        @Test
        @DisplayName("Admin can delete listing (soft delete)")
        void adminCanDeleteListing() throws Exception {
            Listing listing = createTestListing(seller, ListingType.VEHICLE);

            mockMvc.perform(delete("/api/v1/admin/listings/{id}", listing.getId())
                            .header("Authorization", authHeader(adminToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Listing deleted successfully"));

            Listing deletedListing = listingRepository.findById(listing.getId()).orElseThrow();
            assertThat(deletedListing.getStatus()).isEqualTo(ListingStatus.DELETED);
        }

        @Test
        @DisplayName("Admin can delete already deleted listing (idempotent)")
        void adminCanDeleteAlreadyDeletedListing() throws Exception {
            Listing listing = createTestListing(seller, ListingType.VEHICLE);
            listing.setStatus(ListingStatus.DELETED);
            listingRepository.save(listing);

            mockMvc.perform(delete("/api/v1/admin/listings/{id}", listing.getId())
                            .header("Authorization", authHeader(adminToken)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Listing not found returns 404")
        void listingNotFoundReturns404() throws Exception {
            mockMvc.perform(delete("/api/v1/admin/listings/{id}", UUID.randomUUID())
                            .header("Authorization", authHeader(adminToken)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Non-admin cannot delete listing")
        void nonAdminCannotDeleteListing() throws Exception {
            Listing listing = createTestListing(seller, ListingType.VEHICLE);

            mockMvc.perform(delete("/api/v1/admin/listings/{id}", listing.getId())
                            .header("Authorization", authHeader(userToken)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Unauthenticated cannot delete listing")
        void unauthenticatedCannotDeleteListing() throws Exception {
            Listing listing = createTestListing(seller, ListingType.VEHICLE);

            mockMvc.perform(delete("/api/v1/admin/listings/{id}", listing.getId()))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Status Transition Rules")
    class StatusTransitionTests {

        @Test
        @DisplayName("DRAFT can transition to PUBLISHED")
        void draftCanTransitionToPublished() throws Exception {
            Listing listing = createTestListing(seller, ListingType.VEHICLE);

            ChangeListingStatusRequest request = ChangeListingStatusRequest.builder()
                    .status(ListingStatus.PUBLISHED)
                    .build();

            mockMvc.perform(put("/api/v1/admin/listings/{id}/status", listing.getId())
                            .header("Authorization", authHeader(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
        }

        @Test
        @DisplayName("DRAFT can transition to DELETED")
        void draftCanTransitionToDeleted() throws Exception {
            Listing listing = createTestListing(seller, ListingType.VEHICLE);

            ChangeListingStatusRequest request = ChangeListingStatusRequest.builder()
                    .status(ListingStatus.DELETED)
                    .build();

            mockMvc.perform(put("/api/v1/admin/listings/{id}/status", listing.getId())
                            .header("Authorization", authHeader(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("DELETED"));
        }

        @Test
        @DisplayName("PUBLISHED can transition to ACTIVE")
        void publishedCanTransitionToActive() throws Exception {
            Listing listing = createTestListing(seller, ListingType.VEHICLE);
            listing.setStatus(ListingStatus.PUBLISHED);
            listingRepository.save(listing);

            ChangeListingStatusRequest request = ChangeListingStatusRequest.builder()
                    .status(ListingStatus.ACTIVE)
                    .build();

            mockMvc.perform(put("/api/v1/admin/listings/{id}/status", listing.getId())
                            .header("Authorization", authHeader(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("ACTIVE"));
        }

        @Test
        @DisplayName("PUBLISHED can transition to EXPIRED")
        void publishedCanTransitionToExpired() throws Exception {
            Listing listing = createTestListing(seller, ListingType.VEHICLE);
            listing.setStatus(ListingStatus.PUBLISHED);
            listingRepository.save(listing);

            ChangeListingStatusRequest request = ChangeListingStatusRequest.builder()
                    .status(ListingStatus.EXPIRED)
                    .build();

            mockMvc.perform(put("/api/v1/admin/listings/{id}/status", listing.getId())
                            .header("Authorization", authHeader(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("EXPIRED"));
        }

        @Test
        @DisplayName("ACTIVE can transition to EXPIRED")
        void activeCanTransitionToExpired() throws Exception {
            Listing listing = createTestListing(seller, ListingType.VEHICLE);
            listing.setStatus(ListingStatus.ACTIVE);
            listingRepository.save(listing);

            ChangeListingStatusRequest request = ChangeListingStatusRequest.builder()
                    .status(ListingStatus.EXPIRED)
                    .build();

            mockMvc.perform(put("/api/v1/admin/listings/{id}/status", listing.getId())
                            .header("Authorization", authHeader(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("EXPIRED"));
        }

        @Test
        @DisplayName("SOLD can transition to DELETED")
        void soldCanTransitionToDeleted() throws Exception {
            Listing listing = createTestListing(seller, ListingType.VEHICLE);
            listing.setStatus(ListingStatus.SOLD);
            listingRepository.save(listing);

            ChangeListingStatusRequest request = ChangeListingStatusRequest.builder()
                    .status(ListingStatus.DELETED)
                    .build();

            mockMvc.perform(put("/api/v1/admin/listings/{id}/status", listing.getId())
                            .header("Authorization", authHeader(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("DELETED"));
        }

        @Test
        @DisplayName("EXPIRED can transition to DELETED")
        void expiredCanTransitionToDeleted() throws Exception {
            Listing listing = createTestListing(seller, ListingType.VEHICLE);
            listing.setStatus(ListingStatus.EXPIRED);
            listingRepository.save(listing);

            ChangeListingStatusRequest request = ChangeListingStatusRequest.builder()
                    .status(ListingStatus.DELETED)
                    .build();

            mockMvc.perform(put("/api/v1/admin/listings/{id}/status", listing.getId())
                            .header("Authorization", authHeader(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("DELETED"));
        }
    }
}
