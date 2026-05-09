package com.ridelist.integration;

import com.ridelist.dto.request.UpdateProfileRequest;
import com.ridelist.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Seller Profile Integration Tests")
public class SellerProfileIntegrationTest extends BaseIntegrationTest {

    @Nested
    @DisplayName("Seller Slug Generation Tests")
    class SlugGenerationTests {

        @Test
        @DisplayName("SELLER-001: Register as DEALER generates seller slug")
        void registerAsDealer_generatesSlug() throws Exception {
            String email = "dealer@test.com";
            String password = "Password123!";

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(java.util.Map.of(
                                    "email", email,
                                    "password", password,
                                    "firstName", "Chukwuemeka",
                                    "lastName", "Autos",
                                    "phoneNumber", "08012345678",
                                    "accountType", "DEALER"
                            ))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.user.sellerSlug").exists())
                    .andExpect(jsonPath("$.data.user.sellerUrl").exists())
                    .andExpect(jsonPath("$.data.user.hasPublicPage").value(true));

            User dealer = userRepository.findByEmail(email).orElseThrow();
            assertThat(dealer.getSellerSlug()).startsWith("chukwuemeka-autos-");
            assertThat(dealer.getSellerSlug()).hasSize("chukwuemeka-autos-".length() + 6);
        }

        @Test
        @DisplayName("SELLER-002: Register as INDIVIDUAL does not generate slug")
        void registerAsIndividual_noSlug() throws Exception {
            String email = "individual@test.com";
            String password = "Password123!";

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(java.util.Map.of(
                                    "email", email,
                                    "password", password,
                                    "firstName", "John",
                                    "lastName", "Doe",
                                    "phoneNumber", "08012345678",
                                    "accountType", "INDIVIDUAL"
                            ))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.user.sellerSlug").doesNotExist())
                    .andExpect(jsonPath("$.data.user.hasPublicPage").value(false));

            User user = userRepository.findByEmail(email).orElseThrow();
            assertThat(user.getSellerSlug()).isNull();
        }

        @Test
        @DisplayName("SELLER-003: Update name regenerates seller slug for DEALER")
        void updateName_regeneratesSlugForDealer() throws Exception {
            String token = registerAndGetToken("dealer@test.com", "Password123!");
            User dealer = userRepository.findByEmail("dealer@test.com").orElseThrow();
            dealer.setAccountType(AccountType.DEALER);
            dealer.setSellerSlug("old-slug-" + dealer.getId().toString().replace("-", "").substring(0, 6));
            userRepository.save(dealer);

            String oldSlug = dealer.getSellerSlug();

            UpdateProfileRequest request = UpdateProfileRequest.builder()
                    .firstName("NewName")
                    .lastName("NewLastName")
                    .build();

            mockMvc.perform(put("/api/v1/account/me")
                            .header("Authorization", authHeader(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.sellerSlug").exists());

            User updated = userRepository.findByEmail("dealer@test.com").orElseThrow();
            assertThat(updated.getSellerSlug()).isNotEqualTo(oldSlug);
            assertThat(updated.getSellerSlug()).startsWith("newname-newlastname-");
        }
    }

    @Nested
    @DisplayName("Seller Profile Endpoint Tests")
    class ProfileEndpointTests {

        @Test
        @DisplayName("SELLER-004: GET /sellers/{slug} returns seller profile")
        void getSellerBySlug_returnsProfile() throws Exception {
            User dealer = createTestUser("dealer@test.com", Role.USER);
            dealer.setAccountType(AccountType.DEALER);
            dealer.setFirstName("Tunde");
            dealer.setLastName("Motors");
            dealer.setSellerSlug("tunde-motors-" + dealer.getId().toString().replace("-", "").substring(0, 6));
            dealer.setBio("Premium vehicle dealer in Lagos");
            userRepository.save(dealer);

            mockMvc.perform(get("/api/v1/sellers/" + dealer.getSellerSlug()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.firstName").value("Tunde"))
                    .andExpect(jsonPath("$.data.lastName").value("Motors"))
                    .andExpect(jsonPath("$.data.bio").value("Premium vehicle dealer in Lagos"))
                    .andExpect(jsonPath("$.data.sellerSlug").value(dealer.getSellerSlug()))
                    .andExpect(jsonPath("$.data.sellerUrl").value("/sellers/" + dealer.getSellerSlug()));
        }

        @Test
        @DisplayName("SELLER-005: GET /sellers/{slug-with-wrong-name} still resolves via UUID fragment")
        void getSellerBySlugWithWrongName_stillResolves() throws Exception {
            User dealer = createTestUser("dealer@test.com", Role.USER);
            dealer.setAccountType(AccountType.DEALER);
            dealer.setFirstName("Tunde");
            dealer.setLastName("Motors");
            String uuidFragment = dealer.getId().toString().replace("-", "").substring(0, 6);
            dealer.setSellerSlug("tunde-motors-" + uuidFragment);
            userRepository.save(dealer);

            // Use wrong name but same UUID fragment
            String wrongSlug = "wrong-name-lagos-" + uuidFragment;

            mockMvc.perform(get("/api/v1/sellers/" + wrongSlug))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.firstName").value("Tunde"));
        }

        @Test
        @DisplayName("SELLER-006: GET /seller/{uuid} backward compatibility")
        void getSellerByUuid_backwardCompat() throws Exception {
            User dealer = createTestUser("dealer@test.com", Role.USER);
            dealer.setAccountType(AccountType.DEALER);
            dealer.setFirstName("Ade");
            dealer.setLastName("Bikes");
            dealer.setSellerSlug("ade-bikes-" + dealer.getId().toString().replace("-", "").substring(0, 6));
            userRepository.save(dealer);

            mockMvc.perform(get("/api/v1/seller/" + dealer.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.firstName").value("Ade"))
                    .andExpect(jsonPath("$.data.sellerUrl").value("/sellers/" + dealer.getSellerSlug()));
        }

        @Test
        @DisplayName("SELLER-007: GET /sellers/{invalid-slug} returns 404")
        void getSellerByInvalidSlug_returns404() throws Exception {
            mockMvc.perform(get("/api/v1/sellers/invalid-slug-123456"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Bio Update Tests")
    class BioUpdateTests {

        @Test
        @DisplayName("SELLER-008: Update bio via PUT /account/me")
        void updateBio_succeeds() throws Exception {
            String token = registerAndGetToken("user@test.com", "Password123!");

            UpdateProfileRequest request = UpdateProfileRequest.builder()
                    .bio("Professional motorcycle dealer with 10 years experience")
                    .build();

            mockMvc.perform(put("/api/v1/account/me")
                            .header("Authorization", authHeader(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.bio").value("Professional motorcycle dealer with 10 years experience"));
        }

        @Test
        @DisplayName("SELLER-009: Bio exceeding 500 chars returns 400")
        void updateBioTooLong_returns400() throws Exception {
            String token = registerAndGetToken("user@test.com", "Password123!");

            UpdateProfileRequest request = UpdateProfileRequest.builder()
                    .bio("A".repeat(501))
                    .build();

            mockMvc.perform(put("/api/v1/account/me")
                            .header("Authorization", authHeader(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Has Public Page Tests")
    class HasPublicPageTests {

        @Test
        @DisplayName("SELLER-010: Seller with 0 listings has hasPublicPage false")
        void sellerWithNoListings_hasPublicPageFalse() throws Exception {
            User dealer = createTestUser("dealer@test.com", Role.USER);
            dealer.setAccountType(AccountType.DEALER);
            dealer.setSellerSlug("test-dealer-" + dealer.getId().toString().replace("-", "").substring(0, 6));
            userRepository.save(dealer);

            mockMvc.perform(get("/api/v1/sellers/" + dealer.getSellerSlug()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.hasPublicPage").value(false))
                    .andExpect(jsonPath("$.data.activeListingCount").value(0));
        }

        @Test
        @DisplayName("SELLER-011: Seller with 1+ active listings has hasPublicPage true")
        void sellerWithActiveListings_hasPublicPageTrue() throws Exception {
            User dealer = createTestUser("dealer@test.com", Role.USER);
            dealer.setAccountType(AccountType.DEALER);
            dealer.setSellerSlug("test-dealer-" + dealer.getId().toString().replace("-", "").substring(0, 6));
            userRepository.save(dealer);

            State state = createTestState("Lagos");
            Listing listing = Listing.builder()
                    .title("Test Active Listing For Seller Profile Integration")
                    .description("Description")
                    .price(BigDecimal.valueOf(150000))
                    .seller(dealer)
                    .listingType(ListingType.VEHICLE)
                    .vehicleType(VehicleType.MOTORCYCLE)
                    .category(ListingCategory.MOTORCYCLE)
                    .status(ListingStatus.ACTIVE)
                    .condition(ListingCondition.GOOD)
                    .state(state)
                    .build();
            listingRepository.save(listing);

            mockMvc.perform(get("/api/v1/sellers/" + dealer.getSellerSlug()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.hasPublicPage").value(true))
                    .andExpect(jsonPath("$.data.activeListingCount").value(1));
        }
    }

    @Nested
    @DisplayName("Sitemap Tests")
    class SitemapTests {

        @Test
        @DisplayName("SELLER-012: GET /sellers/sitemap-data returns dealer URLs")
        void getSitemapData_returnsDealerUrls() throws Exception {
            User dealer1 = createTestUser("dealer1@test.com", Role.USER);
            dealer1.setAccountType(AccountType.DEALER);
            dealer1.setSellerSlug("dealer-one-" + dealer1.getId().toString().replace("-", "").substring(0, 6));
            userRepository.save(dealer1);

            User dealer2 = createTestUser("dealer2@test.com", Role.USER);
            dealer2.setAccountType(AccountType.DEALER);
            dealer2.setSellerSlug("dealer-two-" + dealer2.getId().toString().replace("-", "").substring(0, 6));
            userRepository.save(dealer2);

            // Individual without slug should not appear
            User individual = createTestUser("individual@test.com", Role.USER);
            individual.setAccountType(AccountType.INDIVIDUAL);
            userRepository.save(individual);

            mockMvc.perform(get("/api/v1/sellers/sitemap-data"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].url").exists())
                    .andExpect(jsonPath("$.data[0].lastModified").exists());
        }
    }
}
