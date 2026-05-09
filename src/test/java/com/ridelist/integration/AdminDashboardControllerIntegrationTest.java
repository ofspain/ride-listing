package com.ridelist.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.ridelist.dto.response.AdminDashboardStatsResponse;
import com.ridelist.dto.response.ApiResponse;
import com.ridelist.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("AdminDashboardController Integration Tests")
class AdminDashboardControllerIntegrationTest extends BaseIntegrationTest {

    private String adminToken;

    @BeforeEach
    void setupAdmin() throws Exception {
        adminToken = registerAndGetToken("admin@test.com", "password123");
        User admin = userRepository.findByEmail("admin@test.com").orElseThrow();
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);
        adminToken = loginAndGetToken("admin@test.com", "password123");
    }

    @Nested
    @DisplayName("Get Dashboard Stats")
    class GetDashboardStatsTests {

        @Test
        @DisplayName("DASH-001: Admin gets dashboard stats successfully")
        void adminGetsDashboardStats_ReturnsOk() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/v1/admin/dashboard/stats")
                            .header("Authorization", authHeader(adminToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.totalListings").isNumber())
                    .andExpect(jsonPath("$.data.activeListings").isNumber())
                    .andExpect(jsonPath("$.data.totalUsers").isNumber())
                    .andExpect(jsonPath("$.data.pendingInquiries").isNumber())
                    .andExpect(jsonPath("$.data.listingsThisWeek").isNumber())
                    .andExpect(jsonPath("$.data.usersThisWeek").isNumber())
                    .andExpect(jsonPath("$.data.statusBreakdown").isArray())
                    .andExpect(jsonPath("$.data.recentListings").isArray())
                    .andExpect(jsonPath("$.data.recentUsers").isArray())
                    .andExpect(jsonPath("$.data.generatedAt").exists())
                    .andReturn();

            ApiResponse<AdminDashboardStatsResponse> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {});

            assertThat(response.getData().getTotalUsers()).isGreaterThanOrEqualTo(1);
        }

        @Test
        @DisplayName("DASH-002: Dashboard stats include correct listing counts")
        void dashboardStatsIncludeCorrectListingCounts() throws Exception {
            User seller = userRepository.findByEmail("admin@test.com").orElseThrow();

            Listing draft = createTestListing(seller, ListingType.VEHICLE);
            draft.setStatus(ListingStatus.DRAFT);
            listingRepository.save(draft);

            Listing active = createTestListing(seller, ListingType.VEHICLE);
            active.setStatus(ListingStatus.ACTIVE);
            listingRepository.save(active);

            MvcResult result = mockMvc.perform(get("/api/v1/admin/dashboard/stats")
                            .header("Authorization", authHeader(adminToken)))
                    .andExpect(status().isOk())
                    .andReturn();

            ApiResponse<AdminDashboardStatsResponse> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {});

            assertThat(response.getData().getTotalListings()).isGreaterThanOrEqualTo(2);
            assertThat(response.getData().getActiveListings()).isGreaterThanOrEqualTo(1);
        }

        @Test
        @DisplayName("DASH-003: Dashboard stats include status breakdown")
        void dashboardStatsIncludeStatusBreakdown() throws Exception {
            User seller = userRepository.findByEmail("admin@test.com").orElseThrow();

            Listing listing = createTestListing(seller, ListingType.VEHICLE);
            listing.setStatus(ListingStatus.ACTIVE);
            listingRepository.save(listing);

            MvcResult result = mockMvc.perform(get("/api/v1/admin/dashboard/stats")
                            .header("Authorization", authHeader(adminToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.statusBreakdown").isArray())
                    .andExpect(jsonPath("$.data.statusBreakdown.length()").value(ListingStatus.values().length))
                    .andReturn();

            ApiResponse<AdminDashboardStatsResponse> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {});

            assertThat(response.getData().getStatusBreakdown()).isNotEmpty();
            assertThat(response.getData().getStatusBreakdown())
                    .anyMatch(s -> s.getStatus() == ListingStatus.ACTIVE && s.getCount() >= 1);
        }

        @Test
        @DisplayName("DASH-004: Dashboard stats include recent listings with seller name")
        void dashboardStatsIncludeRecentListings() throws Exception {
            User seller = userRepository.findByEmail("admin@test.com").orElseThrow();
            seller.setFirstName("Test");
            seller.setLastName("Admin");
            userRepository.save(seller);

            Listing listing = createTestListing(seller, ListingType.VEHICLE);
            listing.setTitle("Test Motorcycle");
            listing.setStatus(ListingStatus.ACTIVE);
            listingRepository.save(listing);

            MvcResult result = mockMvc.perform(get("/api/v1/admin/dashboard/stats")
                            .header("Authorization", authHeader(adminToken)))
                    .andExpect(status().isOk())
                    .andReturn();

            ApiResponse<AdminDashboardStatsResponse> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {});

            assertThat(response.getData().getRecentListings()).isNotEmpty();
            assertThat(response.getData().getRecentListings().get(0).getSellerName())
                    .isEqualTo("Test Admin");
        }

        @Test
        @DisplayName("DASH-005: Dashboard stats include recent users")
        void dashboardStatsIncludeRecentUsers() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/v1/admin/dashboard/stats")
                            .header("Authorization", authHeader(adminToken)))
                    .andExpect(status().isOk())
                    .andReturn();

            ApiResponse<AdminDashboardStatsResponse> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {});

            assertThat(response.getData().getRecentUsers()).isNotEmpty();
            assertThat(response.getData().getRecentUsers().get(0).getEmail())
                    .isEqualTo("admin@test.com");
        }

        @Test
        @DisplayName("DASH-006: Dashboard includes pending inquiries count")
        void dashboardIncludesPendingInquiriesCount() throws Exception {
            User seller = userRepository.findByEmail("admin@test.com").orElseThrow();
            Listing listing = createTestListing(seller, ListingType.VEHICLE);
            listing.setStatus(ListingStatus.ACTIVE);
            listingRepository.save(listing);

            String buyerToken = registerAndGetToken("buyer@test.com", "password123");
            User buyer = userRepository.findByEmail("buyer@test.com").orElseThrow();

            ContactRequest inquiry = createTestInquiry(listing, buyer, "Is this available?");
            inquiry.setStatus(ContactStatus.PENDING);
            contactRequestRepository.save(inquiry);

            MvcResult result = mockMvc.perform(get("/api/v1/admin/dashboard/stats")
                            .header("Authorization", authHeader(adminToken)))
                    .andExpect(status().isOk())
                    .andReturn();

            ApiResponse<AdminDashboardStatsResponse> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {});

            assertThat(response.getData().getPendingInquiries()).isGreaterThanOrEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Refresh Dashboard Stats")
    class RefreshDashboardStatsTests {

        @Test
        @DisplayName("DASH-007: Admin can refresh dashboard stats")
        void adminCanRefreshDashboardStats() throws Exception {
            mockMvc.perform(post("/api/v1/admin/dashboard/stats/refresh")
                            .header("Authorization", authHeader(adminToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Dashboard stats refreshed"))
                    .andExpect(jsonPath("$.data.totalListings").isNumber());
        }
    }

    @Nested
    @DisplayName("Authentication and Authorization")
    class AuthTests {

        @Test
        @DisplayName("DASH-008: Unauthenticated request returns 401")
        void unauthenticatedRequest_ReturnsUnauthorized() throws Exception {
            mockMvc.perform(get("/api/v1/admin/dashboard/stats"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("DASH-009: Non-admin user cannot access dashboard stats")
        void nonAdminUser_ReturnsForbidden() throws Exception {
            String userToken = registerAndGetToken("user@test.com", "password123");

            mockMvc.perform(get("/api/v1/admin/dashboard/stats")
                            .header("Authorization", authHeader(userToken)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("DASH-010: Non-admin cannot refresh dashboard stats")
        void nonAdminCannotRefresh_ReturnsForbidden() throws Exception {
            String userToken = registerAndGetToken("user@test.com", "password123");

            mockMvc.perform(post("/api/v1/admin/dashboard/stats/refresh")
                            .header("Authorization", authHeader(userToken)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Data Integrity Tests")
    class DataIntegrityTests {

        @Test
        @DisplayName("DASH-011: Status breakdown percentages sum to 100")
        void statusBreakdownPercentagesSumTo100() throws Exception {
            User seller = userRepository.findByEmail("admin@test.com").orElseThrow();

            for (int i = 0; i < 5; i++) {
                Listing listing = createTestListing(seller, ListingType.VEHICLE);
                listing.setStatus(ListingStatus.ACTIVE);
                listingRepository.save(listing);
            }

            MvcResult result = mockMvc.perform(get("/api/v1/admin/dashboard/stats")
                            .header("Authorization", authHeader(adminToken)))
                    .andExpect(status().isOk())
                    .andReturn();

            ApiResponse<AdminDashboardStatsResponse> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {});

            double totalPercentage = response.getData().getStatusBreakdown().stream()
                    .mapToDouble(AdminDashboardStatsResponse.StatusCount::getPercentage)
                    .sum();

            assertThat(totalPercentage).isBetween(99.0, 101.0);
        }

        @Test
        @DisplayName("DASH-012: Recent listings limited to 10")
        void recentListingsLimitedTo10() throws Exception {
            User seller = userRepository.findByEmail("admin@test.com").orElseThrow();

            for (int i = 0; i < 15; i++) {
                Listing listing = createTestListing(seller, ListingType.VEHICLE);
                listing.setTitle("Listing " + i);
                listingRepository.save(listing);
            }

            MvcResult result = mockMvc.perform(get("/api/v1/admin/dashboard/stats")
                            .header("Authorization", authHeader(adminToken)))
                    .andExpect(status().isOk())
                    .andReturn();

            ApiResponse<AdminDashboardStatsResponse> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {});

            assertThat(response.getData().getRecentListings()).hasSizeLessThanOrEqualTo(10);
        }

        @Test
        @DisplayName("DASH-013: Recent users limited to 10")
        void recentUsersLimitedTo10() throws Exception {
            for (int i = 0; i < 15; i++) {
                registerAndGetToken("user" + i + "@test.com", "password123");
            }

            MvcResult result = mockMvc.perform(get("/api/v1/admin/dashboard/stats")
                            .header("Authorization", authHeader(adminToken)))
                    .andExpect(status().isOk())
                    .andReturn();

            ApiResponse<AdminDashboardStatsResponse> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {});

            assertThat(response.getData().getRecentUsers()).hasSizeLessThanOrEqualTo(10);
        }
    }
}
