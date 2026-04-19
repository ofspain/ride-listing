package com.ridelist.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.ridelist.dto.request.ContactSellerRequest;
import com.ridelist.dto.response.ApiResponse;
import com.ridelist.dto.response.ContactRequestResponse;
import com.ridelist.dto.response.PagedResponse;
import com.ridelist.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for MessageController.
 * Tests buyer inquiry/contact request functionality.
 *
 * Test IDs: MSG-001 to MSG-008
 */
@DisplayName("MessageController Integration Tests")
class MessageControllerIntegrationTest extends BaseIntegrationTest {

    // ==================== SEND INQUIRY TESTS ====================

    @Nested
    @DisplayName("Send Inquiry Tests")
    class SendInquiryTests {

        @Test
        @DisplayName("MSG-001: Authenticated user sends inquiry - returns 201 Created")
        void authenticatedUserSendsInquiry_ReturnsCreated() throws Exception {
            // Given: A seller with an active listing and a buyer
            String sellerToken = registerAndGetToken("seller@test.com", "password123");
            User seller = userRepository.findByEmail("seller@test.com").orElseThrow();

            Listing listing = createTestListing(seller, ListingType.VEHICLE);
            listing.setStatus(ListingStatus.ACTIVE);
            listing.setTitle("Test Motorcycle");
            listingRepository.save(listing);

            String buyerToken = registerAndGetToken("buyer@test.com", "password123");

            ContactSellerRequest request = ContactSellerRequest.builder()
                    .message("I'm interested in this motorcycle. Is it still available?")
                    .build();

            // When & Then
            MvcResult result = mockMvc.perform(post("/api/v1/listings/{id}/inquire", listing.getId())
                            .header("Authorization", authHeader(buyerToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.message").value(request.getMessage()))
                    .andExpect(jsonPath("$.data.senderName").exists())
                    .andExpect(jsonPath("$.data.listing.id").value(listing.getId().toString()))
                    .andReturn();

            ApiResponse<ContactRequestResponse> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {});

            assertThat(response.getData().getId()).isNotNull();
            assertThat(response.getData().getSenderName()).isEqualTo("Test User");
        }

        @Test
        @DisplayName("MSG-002: Guest sends inquiry with contact info - returns 201 Created")
        void guestSendsInquiryWithContactInfo_ReturnsCreated() throws Exception {
            // Given: A seller with an active listing
            String sellerToken = registerAndGetToken("seller@test.com", "password123");
            User seller = userRepository.findByEmail("seller@test.com").orElseThrow();

            Listing listing = createTestListing(seller, ListingType.VEHICLE);
            listing.setStatus(ListingStatus.ACTIVE);
            listingRepository.save(listing);

            ContactSellerRequest request = ContactSellerRequest.builder()
                    .senderName("John Doe")
                    .senderPhone("08098765432")
                    .message("Hello, I would like to know more about this bike.")
                    .build();

            // When & Then: No auth header for guest
            mockMvc.perform(post("/api/v1/listings/{id}/inquire", listing.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.senderName").value("John Doe"))
                    .andExpect(jsonPath("$.data.senderPhone").value("08098765432"))
                    .andExpect(jsonPath("$.data.message").value(request.getMessage()))
                    .andExpect(jsonPath("$.data.buyer").doesNotExist());
        }

        @Test
        @DisplayName("MSG-003: Guest inquiry without contact info - returns 400 Bad Request")
        void guestInquiryWithoutContactInfo_ReturnsBadRequest() throws Exception {
            // Given: A seller with an active listing
            String sellerToken = registerAndGetToken("seller@test.com", "password123");
            User seller = userRepository.findByEmail("seller@test.com").orElseThrow();

            Listing listing = createTestListing(seller, ListingType.VEHICLE);
            listing.setStatus(ListingStatus.ACTIVE);
            listingRepository.save(listing);

            // Guest inquiry without name/phone
            ContactSellerRequest request = ContactSellerRequest.builder()
                    .message("I want to buy this motorcycle, please contact me.")
                    .build();

            // When & Then
            mockMvc.perform(post("/api/v1/listings/{id}/inquire", listing.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("MSG-003b: Guest inquiry with name but no phone - returns 400 Bad Request")
        void guestInquiryWithNameButNoPhone_ReturnsBadRequest() throws Exception {
            // Given: A seller with an active listing
            String sellerToken = registerAndGetToken("seller@test.com", "password123");
            User seller = userRepository.findByEmail("seller@test.com").orElseThrow();

            Listing listing = createTestListing(seller, ListingType.VEHICLE);
            listing.setStatus(ListingStatus.ACTIVE);
            listingRepository.save(listing);

            ContactSellerRequest request = ContactSellerRequest.builder()
                    .senderName("John Doe")
                    // missing phone
                    .message("I want to buy this motorcycle, please contact me.")
                    .build();

            // When & Then
            mockMvc.perform(post("/api/v1/listings/{id}/inquire", listing.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("MSG-004: Inquiry on non-active listing (DRAFT) - returns 400 Bad Request")
        void inquiryOnDraftListing_ReturnsBadRequest() throws Exception {
            // Given: A seller with a DRAFT listing
            String sellerToken = registerAndGetToken("seller@test.com", "password123");
            User seller = userRepository.findByEmail("seller@test.com").orElseThrow();

            Listing listing = createTestListing(seller, ListingType.VEHICLE);
            // listing is DRAFT by default from createTestListing

            String buyerToken = registerAndGetToken("buyer@test.com", "password123");

            ContactSellerRequest request = ContactSellerRequest.builder()
                    .message("Is this listing available for purchase?")
                    .build();

            // When & Then
            mockMvc.perform(post("/api/v1/listings/{id}/inquire", listing.getId())
                            .header("Authorization", authHeader(buyerToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Cannot send inquiry to a non-active listing"));
        }

        @Test
        @DisplayName("MSG-004b: Inquiry on SOLD listing - returns 400 Bad Request")
        void inquiryOnSoldListing_ReturnsBadRequest() throws Exception {
            // Given: A seller with a SOLD listing
            String sellerToken = registerAndGetToken("seller@test.com", "password123");
            User seller = userRepository.findByEmail("seller@test.com").orElseThrow();

            Listing listing = createTestListing(seller, ListingType.VEHICLE);
            listing.setStatus(ListingStatus.SOLD);
            listingRepository.save(listing);

            String buyerToken = registerAndGetToken("buyer@test.com", "password123");

            ContactSellerRequest request = ContactSellerRequest.builder()
                    .message("Is this listing still available?")
                    .build();

            // When & Then
            mockMvc.perform(post("/api/v1/listings/{id}/inquire", listing.getId())
                            .header("Authorization", authHeader(buyerToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("MSG-005: Inquiry on own listing - returns 400 Bad Request")
        void inquiryOnOwnListing_ReturnsBadRequest() throws Exception {
            // Given: A seller tries to inquire on their own listing
            String sellerToken = registerAndGetToken("seller@test.com", "password123");
            User seller = userRepository.findByEmail("seller@test.com").orElseThrow();

            Listing listing = createTestListing(seller, ListingType.VEHICLE);
            listing.setStatus(ListingStatus.ACTIVE);
            listingRepository.save(listing);

            ContactSellerRequest request = ContactSellerRequest.builder()
                    .message("Testing inquiry on my own listing")
                    .build();

            // When & Then
            mockMvc.perform(post("/api/v1/listings/{id}/inquire", listing.getId())
                            .header("Authorization", authHeader(sellerToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Cannot send inquiry to your own listing"));
        }

        @Test
        @DisplayName("MSG-006: Duplicate inquiry by authenticated user - returns 400 Bad Request")
        void duplicateInquiryByAuthUser_ReturnsBadRequest() throws Exception {
            // Given: A buyer who has already sent an inquiry
            String sellerToken = registerAndGetToken("seller@test.com", "password123");
            User seller = userRepository.findByEmail("seller@test.com").orElseThrow();

            Listing listing = createTestListing(seller, ListingType.VEHICLE);
            listing.setStatus(ListingStatus.ACTIVE);
            listingRepository.save(listing);

            String buyerToken = registerAndGetToken("buyer@test.com", "password123");

            ContactSellerRequest request = ContactSellerRequest.builder()
                    .message("First inquiry about this motorcycle.")
                    .build();

            // First inquiry succeeds
            mockMvc.perform(post("/api/v1/listings/{id}/inquire", listing.getId())
                            .header("Authorization", authHeader(buyerToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            // When & Then: Second inquiry fails
            ContactSellerRequest secondRequest = ContactSellerRequest.builder()
                    .message("Second inquiry - should fail")
                    .build();

            mockMvc.perform(post("/api/v1/listings/{id}/inquire", listing.getId())
                            .header("Authorization", authHeader(buyerToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(secondRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("You have already sent an inquiry for this listing"));
        }

        @Test
        @DisplayName("MSG-006b: Multiple guest inquiries allowed (no duplicate check)")
        void multipleGuestInquiriesAllowed() throws Exception {
            // Given: A seller with an active listing
            String sellerToken = registerAndGetToken("seller@test.com", "password123");
            User seller = userRepository.findByEmail("seller@test.com").orElseThrow();

            Listing listing = createTestListing(seller, ListingType.VEHICLE);
            listing.setStatus(ListingStatus.ACTIVE);
            listingRepository.save(listing);

            ContactSellerRequest firstRequest = ContactSellerRequest.builder()
                    .senderName("John Doe")
                    .senderPhone("08012345678")
                    .message("First guest inquiry about this motorcycle.")
                    .build();

            // First guest inquiry succeeds
            mockMvc.perform(post("/api/v1/listings/{id}/inquire", listing.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(firstRequest)))
                    .andExpect(status().isCreated());

            // When & Then: Second guest inquiry also succeeds (guests not tracked)
            ContactSellerRequest secondRequest = ContactSellerRequest.builder()
                    .senderName("John Doe")
                    .senderPhone("08012345678")
                    .message("Second guest inquiry - should also succeed.")
                    .build();

            mockMvc.perform(post("/api/v1/listings/{id}/inquire", listing.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(secondRequest)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("Inquiry on non-existent listing - returns 404 Not Found")
        void inquiryOnNonExistentListing_ReturnsNotFound() throws Exception {
            // Given: A buyer and a non-existent listing ID
            String buyerToken = registerAndGetToken("buyer@test.com", "password123");

            ContactSellerRequest request = ContactSellerRequest.builder()
                    .message("Inquiry for a listing that doesn't exist")
                    .build();

            // When & Then
            mockMvc.perform(post("/api/v1/listings/{id}/inquire", java.util.UUID.randomUUID())
                            .header("Authorization", authHeader(buyerToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Inquiry with message too short - returns 400 Bad Request")
        void inquiryWithShortMessage_ReturnsBadRequest() throws Exception {
            // Given: A seller with an active listing
            String sellerToken = registerAndGetToken("seller@test.com", "password123");
            User seller = userRepository.findByEmail("seller@test.com").orElseThrow();

            Listing listing = createTestListing(seller, ListingType.VEHICLE);
            listing.setStatus(ListingStatus.ACTIVE);
            listingRepository.save(listing);

            String buyerToken = registerAndGetToken("buyer@test.com", "password123");

            // Message less than 10 characters
            ContactSellerRequest request = ContactSellerRequest.builder()
                    .message("Hi")
                    .build();

            // When & Then
            mockMvc.perform(post("/api/v1/listings/{id}/inquire", listing.getId())
                            .header("Authorization", authHeader(buyerToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ==================== GET SELLER MESSAGES TESTS ====================

    @Nested
    @DisplayName("Get Seller Messages Tests")
    class GetSellerMessagesTests {

        @Test
        @DisplayName("MSG-007: Get seller messages - returns 200 OK with paginated results")
        void getSellerMessages_ReturnsPagedMessages() throws Exception {
            // Given: A seller with listings that have received inquiries
            String sellerToken = registerAndGetToken("seller@test.com", "password123");
            User seller = userRepository.findByEmail("seller@test.com").orElseThrow();

            Listing listing1 = createTestListing(seller, ListingType.VEHICLE);
            listing1.setStatus(ListingStatus.ACTIVE);
            listing1.setTitle("Motorcycle 1");
            listingRepository.save(listing1);

            Listing listing2 = createTestListing(seller, ListingType.PART);
            listing2.setStatus(ListingStatus.ACTIVE);
            listing2.setTitle("Spare Part");
            listingRepository.save(listing2);

            // Create inquiries from different buyers
            String buyer1Token = registerAndGetToken("buyer1@test.com", "password123");
            String buyer2Token = registerAndGetToken("buyer2@test.com", "password123");

            ContactSellerRequest request1 = ContactSellerRequest.builder()
                    .message("Interested in the motorcycle")
                    .build();
            ContactSellerRequest request2 = ContactSellerRequest.builder()
                    .message("Interested in the spare part")
                    .build();

            mockMvc.perform(post("/api/v1/listings/{id}/inquire", listing1.getId())
                            .header("Authorization", authHeader(buyer1Token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request1)))
                    .andExpect(status().isCreated());

            mockMvc.perform(post("/api/v1/listings/{id}/inquire", listing2.getId())
                            .header("Authorization", authHeader(buyer2Token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request2)))
                    .andExpect(status().isCreated());

            // When & Then: Seller retrieves their messages
            MvcResult result = mockMvc.perform(get("/api/v1/account/messages")
                            .header("Authorization", authHeader(sellerToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.totalElements").value(2))
                    .andReturn();

            ApiResponse<PagedResponse<ContactRequestResponse>> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {});

            assertThat(response.getData().getContent()).hasSize(2);
        }

        @Test
        @DisplayName("MSG-007b: Get seller messages with no inquiries - returns empty page")
        void getSellerMessagesWithNoInquiries_ReturnsEmptyPage() throws Exception {
            // Given: A seller with no inquiries
            String sellerToken = registerAndGetToken("seller@test.com", "password123");

            // When & Then
            mockMvc.perform(get("/api/v1/account/messages")
                            .header("Authorization", authHeader(sellerToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content").isEmpty())
                    .andExpect(jsonPath("$.data.totalElements").value(0));
        }

        @Test
        @DisplayName("MSG-008: Message links to correct listing - verify listing ID in response")
        void messageLinksToCorrectListing() throws Exception {
            // Given: A seller with a listing and an inquiry
            String sellerToken = registerAndGetToken("seller@test.com", "password123");
            User seller = userRepository.findByEmail("seller@test.com").orElseThrow();

            Listing listing = createTestListing(seller, ListingType.VEHICLE);
            listing.setStatus(ListingStatus.ACTIVE);
            listing.setTitle("Honda CB500");
            listingRepository.save(listing);

            String buyerToken = registerAndGetToken("buyer@test.com", "password123");

            ContactSellerRequest request = ContactSellerRequest.builder()
                    .message("Is the Honda CB500 still available?")
                    .build();

            mockMvc.perform(post("/api/v1/listings/{id}/inquire", listing.getId())
                            .header("Authorization", authHeader(buyerToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            // When & Then: Verify listing info in seller's messages
            MvcResult result = mockMvc.perform(get("/api/v1/account/messages")
                            .header("Authorization", authHeader(sellerToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].listing.id").value(listing.getId().toString()))
                    .andExpect(jsonPath("$.data.content[0].listing.title").value("Honda CB500"))
                    .andReturn();
        }

        @Test
        @DisplayName("Get messages without authentication - returns 401 Unauthorized")
        void getMessagesWithoutAuth_ReturnsUnauthorized() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/v1/account/messages"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Get messages with pagination parameters")
        void getMessagesWithPagination() throws Exception {
            // Given: A seller with multiple inquiries
            String sellerToken = registerAndGetToken("seller@test.com", "password123");
            User seller = userRepository.findByEmail("seller@test.com").orElseThrow();

            Listing listing = createTestListing(seller, ListingType.VEHICLE);
            listing.setStatus(ListingStatus.ACTIVE);
            listingRepository.save(listing);

            // Create multiple guest inquiries
            for (int i = 1; i <= 5; i++) {
                ContactSellerRequest request = ContactSellerRequest.builder()
                        .senderName("Guest " + i)
                        .senderPhone("0801234567" + i)
                        .message("Inquiry number " + i + " about the listing")
                        .build();

                mockMvc.perform(post("/api/v1/listings/{id}/inquire", listing.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isCreated());
            }

            // When & Then: Request page 0 with size 2
            mockMvc.perform(get("/api/v1/account/messages")
                            .header("Authorization", authHeader(sellerToken))
                            .param("page", "0")
                            .param("size", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content.length()").value(2))
                    .andExpect(jsonPath("$.data.totalElements").value(5))
                    .andExpect(jsonPath("$.data.totalPages").value(3))
                    .andExpect(jsonPath("$.data.first").value(true))
                    .andExpect(jsonPath("$.data.last").value(false));
        }
    }
}
