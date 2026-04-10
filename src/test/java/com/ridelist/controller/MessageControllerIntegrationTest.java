package com.ridelist.controller;

import com.ridelist.BaseIntegrationTest;
import com.ridelist.dto.request.ContactSellerRequest;
import com.ridelist.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("MessageController Integration Tests")
class MessageControllerIntegrationTest extends BaseIntegrationTest {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Nested
    @DisplayName("POST /api/v1/listings/{id}/inquire")
    class SendInquiryTests {

        @Test
        @DisplayName("Should send inquiry as authenticated user")
        void shouldSendInquiryAsAuthenticatedUser() throws Exception {
            User seller = createTestUser("seller@test.com", passwordEncoder.encode("password123"));
            Listing listing = createTestListing(seller, ListingStatus.ACTIVE);

            String buyerToken = registerAndGetToken("buyer@test.com", "password123");

            ContactSellerRequest request = ContactSellerRequest.builder()
                    .message("Hello, I am interested in this motorcycle. Is the price negotiable?")
                    .build();

            mockMvc.perform(post("/api/v1/listings/{id}/inquire", listing.getId())
                            .header("Authorization", "Bearer " + buyerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Inquiry sent successfully"))
                    .andExpect(jsonPath("$.data.message").value("Hello, I am interested in this motorcycle. Is the price negotiable?"))
                    .andExpect(jsonPath("$.data.status").value("PENDING"))
                    .andExpect(jsonPath("$.data.buyer").isNotEmpty());
        }

        @Test
        @DisplayName("Should send inquiry as guest user with contact details")
        void shouldSendInquiryAsGuest() throws Exception {
            User seller = createTestUser("seller@test.com", passwordEncoder.encode("password123"));
            Listing listing = createTestListing(seller, ListingStatus.ACTIVE);

            ContactSellerRequest request = ContactSellerRequest.builder()
                    .senderName("John Guest")
                    .senderPhone("08098765432")
                    .message("I am interested in this listing. Please call me back.")
                    .build();

            mockMvc.perform(post("/api/v1/listings/{id}/inquire", listing.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.senderName").value("John Guest"))
                    .andExpect(jsonPath("$.data.senderPhone").value("08098765432"))
                    .andExpect(jsonPath("$.data.buyer").isEmpty());
        }

        @Test
        @DisplayName("Should fail guest inquiry without sender name")
        void shouldFailGuestInquiryWithoutName() throws Exception {
            User seller = createTestUser("seller@test.com", passwordEncoder.encode("password123"));
            Listing listing = createTestListing(seller, ListingStatus.ACTIVE);

            ContactSellerRequest request = ContactSellerRequest.builder()
                    .senderPhone("08098765432")
                    .message("I am interested in this listing.")
                    .build();

            mockMvc.perform(post("/api/v1/listings/{id}/inquire", listing.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Sender name is required for guest inquiries"));
        }

        @Test
        @DisplayName("Should fail guest inquiry without phone")
        void shouldFailGuestInquiryWithoutPhone() throws Exception {
            User seller = createTestUser("seller@test.com", passwordEncoder.encode("password123"));
            Listing listing = createTestListing(seller, ListingStatus.ACTIVE);

            ContactSellerRequest request = ContactSellerRequest.builder()
                    .senderName("John Guest")
                    .message("I am interested in this listing.")
                    .build();

            mockMvc.perform(post("/api/v1/listings/{id}/inquire", listing.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Sender phone is required for guest inquiries"));
        }

        @Test
        @DisplayName("Should fail to inquire on own listing")
        void shouldFailToInquireOnOwnListing() throws Exception {
            String sellerToken = registerAndGetToken("seller@test.com", "password123");
            User seller = userRepository.findByEmail("seller@test.com").orElseThrow();
            Listing listing = createTestListing(seller, ListingStatus.ACTIVE);

            ContactSellerRequest request = ContactSellerRequest.builder()
                    .message("This is my own listing test.")
                    .build();

            mockMvc.perform(post("/api/v1/listings/{id}/inquire", listing.getId())
                            .header("Authorization", "Bearer " + sellerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Cannot send inquiry to your own listing"));
        }

        @Test
        @DisplayName("Should fail to send duplicate inquiry")
        void shouldFailToSendDuplicateInquiry() throws Exception {
            User seller = createTestUser("seller@test.com", passwordEncoder.encode("password123"));
            Listing listing = createTestListing(seller, ListingStatus.ACTIVE);

            String buyerToken = registerAndGetToken("buyer@test.com", "password123");

            ContactSellerRequest request = ContactSellerRequest.builder()
                    .message("First inquiry message.")
                    .build();

            // First inquiry
            mockMvc.perform(post("/api/v1/listings/{id}/inquire", listing.getId())
                            .header("Authorization", "Bearer " + buyerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            // Second inquiry (should fail)
            mockMvc.perform(post("/api/v1/listings/{id}/inquire", listing.getId())
                            .header("Authorization", "Bearer " + buyerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("You have already sent an inquiry for this listing"));
        }

        @Test
        @DisplayName("Should fail to inquire on non-active listing")
        void shouldFailToInquireOnNonActiveListing() throws Exception {
            User seller = createTestUser("seller@test.com", passwordEncoder.encode("password123"));
            Listing listing = createTestListing(seller, ListingStatus.DRAFT);

            String buyerToken = registerAndGetToken("buyer@test.com", "password123");

            ContactSellerRequest request = ContactSellerRequest.builder()
                    .message("I want to inquire about this draft listing.")
                    .build();

            mockMvc.perform(post("/api/v1/listings/{id}/inquire", listing.getId())
                            .header("Authorization", "Bearer " + buyerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Cannot send inquiry to a non-active listing"));
        }

        @Test
        @DisplayName("Should fail with message too short")
        void shouldFailWithShortMessage() throws Exception {
            User seller = createTestUser("seller@test.com", passwordEncoder.encode("password123"));
            Listing listing = createTestListing(seller, ListingStatus.ACTIVE);

            String buyerToken = registerAndGetToken("buyer@test.com", "password123");

            ContactSellerRequest request = ContactSellerRequest.builder()
                    .message("Hi")
                    .build();

            mockMvc.perform(post("/api/v1/listings/{id}/inquire", listing.getId())
                            .header("Authorization", "Bearer " + buyerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should fail for non-existent listing")
        void shouldFailForNonExistentListing() throws Exception {
            String buyerToken = registerAndGetToken("buyer@test.com", "password123");

            ContactSellerRequest request = ContactSellerRequest.builder()
                    .message("I am interested in this listing.")
                    .build();

            mockMvc.perform(post("/api/v1/listings/{id}/inquire", "00000000-0000-0000-0000-000000000000")
                            .header("Authorization", "Bearer " + buyerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/account/messages")
    class GetMessagesTests {

        @Test
        @DisplayName("Should get messages for seller")
        void shouldGetMessagesForSeller() throws Exception {
            String sellerToken = registerAndGetToken("seller@test.com", "password123");
            User seller = userRepository.findByEmail("seller@test.com").orElseThrow();
            Listing listing1 = createTestListing(seller, ListingStatus.ACTIVE);
            Listing listing2 = createTestListing(seller, ListingStatus.ACTIVE);

            // Create buyers and send inquiries
            User buyer1 = createTestUser("buyer1@test.com", passwordEncoder.encode("password123"));
            User buyer2 = createTestUser("buyer2@test.com", passwordEncoder.encode("password123"));

            contactRequestRepository.save(ContactRequest.builder()
                    .listing(listing1)
                    .buyer(buyer1)
                    .senderName(buyer1.getFullName())
                    .senderPhone(buyer1.getPhoneNumber())
                    .message("Inquiry from buyer 1")
                    .build());

            contactRequestRepository.save(ContactRequest.builder()
                    .listing(listing2)
                    .buyer(buyer2)
                    .senderName(buyer2.getFullName())
                    .senderPhone(buyer2.getPhoneNumber())
                    .message("Inquiry from buyer 2")
                    .build());

            mockMvc.perform(get("/api/v1/account/messages")
                            .header("Authorization", "Bearer " + sellerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content", hasSize(2)))
                    .andExpect(jsonPath("$.data.totalElements").value(2));
        }

        @Test
        @DisplayName("Should return empty list when no messages")
        void shouldReturnEmptyList() throws Exception {
            String sellerToken = registerAndGetToken("seller@test.com", "password123");

            mockMvc.perform(get("/api/v1/account/messages")
                            .header("Authorization", "Bearer " + sellerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content", hasSize(0)));
        }

        @Test
        @DisplayName("Should not show messages for other sellers' listings")
        void shouldNotShowOtherSellersMessages() throws Exception {
            // Seller 1 creates listing
            User seller1 = createTestUser("seller1@test.com", passwordEncoder.encode("password123"));
            Listing listing = createTestListing(seller1, ListingStatus.ACTIVE);

            // Buyer sends inquiry to seller 1
            User buyer = createTestUser("buyer@test.com", passwordEncoder.encode("password123"));
            contactRequestRepository.save(ContactRequest.builder()
                    .listing(listing)
                    .buyer(buyer)
                    .senderName(buyer.getFullName())
                    .senderPhone(buyer.getPhoneNumber())
                    .message("Inquiry to seller 1")
                    .build());

            // Seller 2 checks their messages (should be empty)
            String seller2Token = registerAndGetToken("seller2@test.com", "password123");

            mockMvc.perform(get("/api/v1/account/messages")
                            .header("Authorization", "Bearer " + seller2Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(0)));
        }

        @Test
        @DisplayName("Should paginate messages")
        void shouldPaginateMessages() throws Exception {
            String sellerToken = registerAndGetToken("seller@test.com", "password123");
            User seller = userRepository.findByEmail("seller@test.com").orElseThrow();
            Listing listing = createTestListing(seller, ListingStatus.ACTIVE);

            // Create 15 contact requests
            for (int i = 0; i < 15; i++) {
                contactRequestRepository.save(ContactRequest.builder()
                        .listing(listing)
                        .senderName("Guest " + i)
                        .senderPhone("0801234567" + i)
                        .message("Inquiry message " + i + " with enough characters")
                        .build());
            }

            mockMvc.perform(get("/api/v1/account/messages")
                            .header("Authorization", "Bearer " + sellerToken)
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(10)))
                    .andExpect(jsonPath("$.data.totalElements").value(15))
                    .andExpect(jsonPath("$.data.totalPages").value(2));
        }

        @Test
        @DisplayName("Should fail without authentication")
        void shouldFailWithoutAuth() throws Exception {
            mockMvc.perform(get("/api/v1/account/messages"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should include guest inquiries")
        void shouldIncludeGuestInquiries() throws Exception {
            String sellerToken = registerAndGetToken("seller@test.com", "password123");
            User seller = userRepository.findByEmail("seller@test.com").orElseThrow();
            Listing listing = createTestListing(seller, ListingStatus.ACTIVE);

            // Create guest inquiry (no buyer)
            contactRequestRepository.save(ContactRequest.builder()
                    .listing(listing)
                    .buyer(null)
                    .senderName("Guest User")
                    .senderPhone("08099999999")
                    .message("Guest inquiry message with enough characters")
                    .build());

            mockMvc.perform(get("/api/v1/account/messages")
                            .header("Authorization", "Bearer " + sellerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].senderName").value("Guest User"))
                    .andExpect(jsonPath("$.data.content[0].senderPhone").value("08099999999"))
                    .andExpect(jsonPath("$.data.content[0].buyer").isEmpty());
        }
    }
}
