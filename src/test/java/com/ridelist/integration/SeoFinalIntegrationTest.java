package com.ridelist.integration;

import com.ridelist.model.*;
import com.ridelist.repository.ListingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SeoFinalIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ListingRepository listingRepository;

    @Nested
    @DisplayName("Account listings response - canonicalUrl")
    class AccountListingsCanonicalUrlTests {

        @Test
        @DisplayName("GET /account/listings should include canonicalUrl")
        void accountListings_includesCanonicalUrl() throws Exception {
            String token = registerAndGetToken("accountseller@test.com", "password123");
            User seller = userRepository.findByEmail("accountseller@test.com").orElseThrow();
            State state = createTestState("Lagos");

            Listing listing = Listing.builder()
                    .listingType(ListingType.VEHICLE)
                    .vehicleType(VehicleType.MOTORCYCLE)
                    .title("My Honda CB300R")
                    .slug("my-honda-cb300r")
                    .description("Test motorcycle")
                    .price(BigDecimal.valueOf(500000))
                    .category(ListingCategory.MOTORCYCLE)
                    .condition(ListingCondition.GOOD)
                    .status(ListingStatus.DRAFT)
                    .seller(seller)
                    .state(state)
                    .build();
            listingRepository.save(listing);

            mockMvc.perform(get("/api/v1/account/listings")
                            .header("Authorization", authHeader(token)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].canonicalUrl").exists())
                    .andExpect(jsonPath("$.data.content[0].canonicalUrl", containsString("/motorcycles/lagos/")))
                    .andExpect(jsonPath("$.data.content[0].listingNumber").isNumber());
        }
    }

    @Nested
    @DisplayName("Admin listings response - canonicalUrl")
    class AdminListingsCanonicalUrlTests {

        @Test
        @DisplayName("GET /admin/listings should include canonicalUrl")
        void adminListings_includesCanonicalUrl() throws Exception {
            String token = registerAndGetToken("admin@test.com", "password123");
            User admin = userRepository.findByEmail("admin@test.com").orElseThrow();
            admin.setRole(Role.ADMIN);
            userRepository.save(admin);
            token = loginAndGetToken("admin@test.com", "password123");

            User seller = createTestUser("seller@test.com", Role.USER);
            State state = createTestState("Kano");

            Listing listing = Listing.builder()
                    .listingType(ListingType.VEHICLE)
                    .vehicleType(VehicleType.TRICYCLE)
                    .title("Admin Test Tricycle")
                    .slug("admin-test-tricycle")
                    .description("Admin test")
                    .price(BigDecimal.valueOf(800000))
                    .category(ListingCategory.TRICYCLE)
                    .condition(ListingCondition.GOOD)
                    .status(ListingStatus.ACTIVE)
                    .seller(seller)
                    .state(state)
                    .build();
            listingRepository.save(listing);

            mockMvc.perform(get("/api/v1/admin/listings")
                            .header("Authorization", authHeader(token)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].canonicalUrl").exists())
                    .andExpect(jsonPath("$.data.content[0].canonicalUrl", containsString("/tricycles/kano/")))
                    .andExpect(jsonPath("$.data.content[0].listingNumber").isNumber());
        }
    }

    @Nested
    @DisplayName("Sitemap data endpoint")
    class SitemapDataTests {

        @Test
        @DisplayName("should return active listings for sitemap")
        void sitemapData_returnsActiveListings() throws Exception {
            User seller = createTestUser("sitemapseller@test.com", Role.USER);
            State state = createTestState("Abuja");

            Listing activeListing = Listing.builder()
                    .listingType(ListingType.VEHICLE)
                    .vehicleType(VehicleType.MOTORCYCLE)
                    .title("Active Sitemap Bike")
                    .slug("active-sitemap-bike")
                    .description("Active")
                    .price(BigDecimal.valueOf(500000))
                    .category(ListingCategory.MOTORCYCLE)
                    .condition(ListingCondition.GOOD)
                    .status(ListingStatus.ACTIVE)
                    .seller(seller)
                    .state(state)
                    .build();
            listingRepository.save(activeListing);

            Listing draftListing = Listing.builder()
                    .listingType(ListingType.VEHICLE)
                    .vehicleType(VehicleType.MOTORCYCLE)
                    .title("Draft Sitemap Bike")
                    .slug("draft-sitemap-bike")
                    .description("Draft")
                    .price(BigDecimal.valueOf(300000))
                    .category(ListingCategory.MOTORCYCLE)
                    .condition(ListingCondition.GOOD)
                    .status(ListingStatus.DRAFT)
                    .seller(seller)
                    .state(state)
                    .build();
            listingRepository.save(draftListing);

            mockMvc.perform(get("/api/v1/listings/sitemap-data"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.listings").isArray())
                    .andExpect(jsonPath("$.data.listings[*].url", hasItem(containsString("active-sitemap-bike"))))
                    .andExpect(jsonPath("$.data.listings[*].url", not(hasItem(containsString("draft-sitemap-bike")))));
        }

        @Test
        @DisplayName("sitemap entry URLs should match expected pattern")
        void sitemapEntryUrls_matchExpectedPattern() throws Exception {
            User seller = createTestUser("patterntest@test.com", Role.USER);
            State state = createTestState("Delta");
            Axis axis = createTestAxis("Warri", state);
            Area area = createTestArea("Effurun", axis);

            Listing listing = Listing.builder()
                    .listingType(ListingType.VEHICLE)
                    .vehicleType(VehicleType.MOTORCYCLE)
                    .title("Pattern Test Honda CB300R")
                    .slug("pattern-test-honda-cb300r")
                    .description("Test")
                    .price(BigDecimal.valueOf(500000))
                    .category(ListingCategory.MOTORCYCLE)
                    .condition(ListingCondition.GOOD)
                    .status(ListingStatus.ACTIVE)
                    .seller(seller)
                    .state(state)
                    .axis(axis)
                    .area(area)
                    .build();
            listing = listingRepository.save(listing);

            mockMvc.perform(get("/api/v1/listings/sitemap-data"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.listings[*].url",
                            hasItem(containsString("/motorcycles/delta/warri/effurun/" + listing.getListingNumber()))));
        }

        @Test
        @DisplayName("sitemap entries include lastModified")
        void sitemapEntries_includeLastModified() throws Exception {
            User seller = createTestUser("modifiedtest@test.com", Role.USER);
            State state = createTestState("Rivers");

            Listing listing = Listing.builder()
                    .listingType(ListingType.PART)
                    .title("Test Brake Pads")
                    .slug("test-brake-pads")
                    .description("Test")
                    .price(BigDecimal.valueOf(15000))
                    .category(ListingCategory.SPARE_PART)
                    .condition(ListingCondition.NEW)
                    .status(ListingStatus.ACTIVE)
                    .seller(seller)
                    .state(state)
                    .partName("Brake Pads")
                    .build();
            listingRepository.save(listing);

            mockMvc.perform(get("/api/v1/listings/sitemap-data"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.listings[0].url").exists())
                    .andExpect(jsonPath("$.data.listings[0].lastModified").exists());
        }

        @Test
        @DisplayName("sitemap is public (no auth required)")
        void sitemap_isPublic() throws Exception {
            mockMvc.perform(get("/api/v1/listings/sitemap-data"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("sitemap includes PUBLISHED listings")
        void sitemapData_includesPublishedListings() throws Exception {
            User seller = createTestUser("publishedseller@test.com", Role.USER);
            State state = createTestState("Oyo");

            Listing publishedListing = Listing.builder()
                    .listingType(ListingType.VEHICLE)
                    .vehicleType(VehicleType.BICYCLE)
                    .title("Published Bicycle")
                    .slug("published-bicycle")
                    .description("Published")
                    .price(BigDecimal.valueOf(150000))
                    .category(ListingCategory.BICYCLE)
                    .condition(ListingCondition.GOOD)
                    .status(ListingStatus.PUBLISHED)
                    .seller(seller)
                    .state(state)
                    .build();
            listingRepository.save(publishedListing);

            mockMvc.perform(get("/api/v1/listings/sitemap-data"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.listings[*].url", hasItem(containsString("published-bicycle"))));
        }
    }

    @Nested
    @DisplayName("Full URL chain validation")
    class FullUrlChainTests {

        @Test
        @DisplayName("create listing -> get canonicalUrl -> resolve by ref")
        void fullUrlChain_worksEndToEnd() throws Exception {
            String token = registerAndGetToken("chaintest@test.com", "password123");
            State state = createTestState("Enugu");

            // Step 1: Create a listing
            String createRequest = """
                {
                    "listingType": "VEHICLE",
                    "vehicleType": "MOTORCYCLE",
                    "title": "Chain Test Honda CB300R",
                    "description": "Full chain test",
                    "price": 500000,
                    "category": "MOTORCYCLE",
                    "condition": "GOOD",
                    "stateId": "%s"
                }
                """.formatted(state.getId());

            String response = mockMvc.perform(post("/api/v1/account/listings")
                            .header("Authorization", authHeader(token))
                            .contentType("application/json")
                            .content(createRequest))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.listingNumber").isNumber())
                    .andExpect(jsonPath("$.data.canonicalUrl").exists())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            // Extract listing number from response
            com.fasterxml.jackson.databind.JsonNode json = objectMapper.readTree(response);
            int listingNumber = json.get("data").get("listingNumber").asInt();
            String canonicalUrl = json.get("data").get("canonicalUrl").asText();

            // Step 2: Verify canonicalUrl format
            org.assertj.core.api.Assertions.assertThat(canonicalUrl)
                    .contains("/motorcycles/enugu/")
                    .contains(listingNumber + "-chain-test-honda-cb300r");

            // Step 3: Resolve by ref using the listing number
            mockMvc.perform(get("/api/v1/listings/ref/" + listingNumber + "-chain-test-honda-cb300r"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.listingNumber").value(listingNumber))
                    .andExpect(jsonPath("$.data.title").value("Chain Test Honda CB300R"));

            // Step 4: Resolve by just the number (slug ignored)
            mockMvc.perform(get("/api/v1/listings/ref/" + listingNumber))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.listingNumber").value(listingNumber));
        }
    }
}
