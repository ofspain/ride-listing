# Delete Account Feature - Integration Test Plan

## 1. Overview

This document outlines the comprehensive integration test plan for the "Delete Account" soft delete feature in RideList.

**Objective:** Ensure that account deletion is safe, secure, and maintains data integrity while making deleted users completely invisible in the system.

**Scope:**
- Account deletion flow and validation
- Authentication blocking after deletion
- Global filtering enforcement (deleted users invisible)
- Listing visibility after deletion
- Favorites cleanup
- Message preservation
- Data integrity verification
- Security enforcement

**Out of Scope:**
- Unit tests
- UI/frontend tests
- Hard delete functionality

---

## 2. Test Environment Setup

### 2.1 Technology Stack

Uses the same stack as the main TEST_PLAN.md:
- JUnit 5 + Spring Boot Test
- PostgreSQL 15 via Testcontainers
- MockMvc for HTTP testing
- AssertJ for assertions

### 2.2 Test Class Structure

```
src/test/java/com/ridelist/integration/
├── AccountDeletionIntegrationTest.java      # Core deletion flow
├── AuthAfterDeletionIntegrationTest.java    # Authentication blocking
├── ListingVisibilityAfterDeletionTest.java  # Listing filtering
└── DeleteAccountDataIntegrityTest.java      # Data integrity checks
```

### 2.3 Test Data Strategy

Reuse existing helper methods from `BaseIntegrationTest`:
- `registerAndGetToken(email, password)` - Register user and get JWT
- `loginAndGetToken(email, password)` - Login and get JWT
- `createTestUser(email, role)` - Create user directly in DB
- `createTestListing(seller, type)` - Create test listing

New helper methods needed:
```java
protected void deleteAccount(String token) throws Exception {
    mockMvc.perform(delete("/api/v1/account")
                    .header("Authorization", authHeader(token)))
            .andExpect(status().isOk());
}

protected Favorite createTestFavorite(User user, Listing listing) {
    return favoriteRepository.save(Favorite.builder()
            .user(user)
            .listing(listing)
            .build());
}

protected ContactRequest createTestInquiry(Listing listing, User buyer, String message) {
    return contactRequestRepository.save(ContactRequest.builder()
            .listing(listing)
            .buyer(buyer)
            .message(message)
            .build());
}
```

---

## 3. Test Scenarios

### 3.1 Account Deletion Flow

**Test Class:** `AccountDeletionIntegrationTest`

| Test ID | Scenario | Input | Expected Result |
|---------|----------|-------|-----------------|
| DEL-001 | Successfully delete own account | Valid token | 200 OK, success message |
| DEL-002 | Verify enabled = false after deletion | Deleted account | enabled = false in DB |
| DEL-003 | Verify deletedAt is set after deletion | Deleted account | deletedAt != null in DB |
| DEL-004 | Attempt to delete already deleted account | Second delete call | 400 Bad Request |
| DEL-005 | Delete account without authentication | No token | 401 Unauthorized |

```java
@Nested
@DisplayName("Account Deletion Flow")
class DeletionFlowTests {

    @Test
    void deleteAccount_validToken_returnsSuccess() throws Exception {
        String token = registerAndGetToken("user@test.com", "Password123!");

        mockMvc.perform(delete("/api/v1/account")
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Account deleted successfully"));
    }

    @Test
    void deleteAccount_verifyEnabledFalse() throws Exception {
        String token = registerAndGetToken("user@test.com", "Password123!");
        User user = userRepository.findByEmail("user@test.com").orElseThrow();
        UUID userId = user.getId();

        mockMvc.perform(delete("/api/v1/account")
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isOk());

        // Use native query to bypass @Where filter
        User deletedUser = userRepository.findByIdIncludingDeleted(userId).orElseThrow();
        assertThat(deletedUser.isEnabled()).isFalse();
    }

    @Test
    void deleteAccount_verifyDeletedAtSet() throws Exception {
        String token = registerAndGetToken("user@test.com", "Password123!");
        User user = userRepository.findByEmail("user@test.com").orElseThrow();
        UUID userId = user.getId();

        mockMvc.perform(delete("/api/v1/account")
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isOk());

        User deletedUser = userRepository.findByIdIncludingDeleted(userId).orElseThrow();
        assertThat(deletedUser.getDeletedAt()).isNotNull();
        assertThat(deletedUser.getDeletedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    void deleteAccount_alreadyDeleted_returns400() throws Exception {
        String token = registerAndGetToken("user@test.com", "Password123!");

        // First deletion
        mockMvc.perform(delete("/api/v1/account")
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isOk());

        // Second deletion attempt (using cached token)
        mockMvc.perform(delete("/api/v1/account")
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isUnauthorized()); // JWT validation fails for deleted user
    }

    @Test
    void deleteAccount_noToken_returns401() throws Exception {
        mockMvc.perform(delete("/api/v1/account"))
                .andExpect(status().isUnauthorized());
    }
}
```

---

### 3.2 Authentication Behavior After Deletion

**Test Class:** `AuthAfterDeletionIntegrationTest`

| Test ID | Scenario | Input | Expected Result |
|---------|----------|-------|-----------------|
| DEL-010 | Deleted user cannot login | Correct credentials | 401 Unauthorized |
| DEL-011 | Deleted user cannot access protected endpoints | Existing JWT | 401 Unauthorized |
| DEL-012 | JWT validation rejects deleted user | Valid token structure | 401 Unauthorized |
| DEL-013 | Deleted user cannot register with same email | Same email | 201 Created (new account) |

```java
@Nested
@DisplayName("Authentication After Deletion")
class AuthAfterDeletionTests {

    @Test
    void login_deletedUser_returns401() throws Exception {
        String email = "user@test.com";
        String password = "Password123!";
        String token = registerAndGetToken(email, password);

        // Delete account
        mockMvc.perform(delete("/api/v1/account")
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isOk());

        // Attempt login
        LoginRequest request = LoginRequest.builder()
                .email(email)
                .password(password)
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_deletedUser_returns401() throws Exception {
        String token = registerAndGetToken("user@test.com", "Password123!");

        // Delete account
        mockMvc.perform(delete("/api/v1/account")
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isOk());

        // Attempt to access protected endpoint with old token
        mockMvc.perform(get("/api/v1/account/listings")
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void register_sameEmailAfterDeletion_createsNewAccount() throws Exception {
        String email = "user@test.com";
        String password = "Password123!";
        String token = registerAndGetToken(email, password);

        // Delete account
        mockMvc.perform(delete("/api/v1/account")
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isOk());

        // Re-register with same email
        RegisterRequest request = RegisterRequest.builder()
                .email(email)
                .password("NewPassword123!")
                .firstName("New")
                .lastName("User")
                .phoneNumber("08012345679")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.user.email").value(email));
    }
}
```

---

### 3.3 Global Filtering Enforcement (Critical)

**Test Class:** `AccountDeletionIntegrationTest`

| Test ID | Scenario | Input | Expected Result |
|---------|----------|-------|-----------------|
| DEL-020 | Deleted user not returned in user queries | findByEmail | Optional.empty() |
| DEL-021 | Deleted user not returned in findById | Standard query | Optional.empty() |
| DEL-022 | Native query returns deleted user | findByIdIncludingDeleted | User returned |
| DEL-023 | System behaves as if user does not exist | Various queries | User invisible |

```java
@Nested
@DisplayName("Global Filtering Enforcement")
class GlobalFilteringTests {

    @Test
    void findByEmail_deletedUser_returnsEmpty() throws Exception {
        String email = "user@test.com";
        String token = registerAndGetToken(email, "Password123!");

        // Delete account
        mockMvc.perform(delete("/api/v1/account")
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isOk());

        // Standard query should not find deleted user
        Optional<User> user = userRepository.findByEmail(email);
        assertThat(user).isEmpty();
    }

    @Test
    void findById_deletedUser_returnsEmpty() throws Exception {
        String token = registerAndGetToken("user@test.com", "Password123!");
        User user = userRepository.findByEmail("user@test.com").orElseThrow();
        UUID userId = user.getId();

        // Delete account
        mockMvc.perform(delete("/api/v1/account")
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isOk());

        // Standard query should not find deleted user
        Optional<User> deletedUser = userRepository.findById(userId);
        assertThat(deletedUser).isEmpty();
    }

    @Test
    void findByIdIncludingDeleted_deletedUser_returnsUser() throws Exception {
        String token = registerAndGetToken("user@test.com", "Password123!");
        User user = userRepository.findByEmail("user@test.com").orElseThrow();
        UUID userId = user.getId();

        // Delete account
        mockMvc.perform(delete("/api/v1/account")
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isOk());

        // Native query should find deleted user
        Optional<User> deletedUser = userRepository.findByIdIncludingDeleted(userId);
        assertThat(deletedUser).isPresent();
        assertThat(deletedUser.get().getDeletedAt()).isNotNull();
    }
}
```

---

### 3.4 Listing Behavior After Deletion

**Test Class:** `ListingVisibilityAfterDeletionTest`

| Test ID | Scenario | Input | Expected Result |
|---------|----------|-------|-----------------|
| DEL-030 | User's listings marked as DELETED | Delete account | All listings status = DELETED |
| DEL-031 | SOLD listings remain SOLD | Delete account | SOLD status preserved |
| DEL-032 | Listings not visible in public API | GET /listings | Empty or filtered |
| DEL-033 | Listings not visible in search results | Search query | Not returned |
| DEL-034 | Listing detail returns 404 for deleted seller | GET /listings/{id} | 404 Not Found (optional) |

```java
@Nested
@DisplayName("Listing Visibility After Deletion")
class ListingVisibilityTests {

    @Test
    void deleteAccount_listingsMarkedAsDeleted() throws Exception {
        String token = registerAndGetToken("seller@test.com", "Password123!");
        User seller = userRepository.findByEmail("seller@test.com").orElseThrow();
        State state = createTestState("Lagos");

        // Create listings
        Listing draft = createTestListing(seller, ListingType.VEHICLE);
        draft.setStatus(ListingStatus.DRAFT);
        draft.setState(state);

        Listing active = createTestListing(seller, ListingType.VEHICLE);
        active.setStatus(ListingStatus.ACTIVE);
        active.setState(state);

        listingRepository.saveAll(List.of(draft, active));

        UUID draftId = draft.getId();
        UUID activeId = active.getId();

        // Delete account
        mockMvc.perform(delete("/api/v1/account")
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isOk());

        // Verify listings marked as DELETED
        entityManager.clear(); // Clear JPA cache
        Listing deletedDraft = listingRepository.findById(draftId).orElseThrow();
        Listing deletedActive = listingRepository.findById(activeId).orElseThrow();

        assertThat(deletedDraft.getStatus()).isEqualTo(ListingStatus.DELETED);
        assertThat(deletedActive.getStatus()).isEqualTo(ListingStatus.DELETED);
    }

    @Test
    void deleteAccount_soldListingsRemainSold() throws Exception {
        String token = registerAndGetToken("seller@test.com", "Password123!");
        User seller = userRepository.findByEmail("seller@test.com").orElseThrow();
        State state = createTestState("Lagos");

        Listing sold = createTestListing(seller, ListingType.VEHICLE);
        sold.setStatus(ListingStatus.SOLD);
        sold.setState(state);
        listingRepository.save(sold);

        UUID soldId = sold.getId();

        // Delete account
        mockMvc.perform(delete("/api/v1/account")
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isOk());

        // Verify SOLD status preserved
        entityManager.clear();
        Listing stillSold = listingRepository.findById(soldId).orElseThrow();
        assertThat(stillSold.getStatus()).isEqualTo(ListingStatus.SOLD);
    }

    @Test
    void getListings_deletedSeller_listingsNotVisible() throws Exception {
        String token = registerAndGetToken("seller@test.com", "Password123!");
        User seller = userRepository.findByEmail("seller@test.com").orElseThrow();
        State state = createTestState("Lagos");

        Listing listing = createTestListing(seller, ListingType.VEHICLE);
        listing.setStatus(ListingStatus.ACTIVE);
        listing.setState(state);
        listingRepository.save(listing);

        // Verify listing is visible before deletion
        mockMvc.perform(get("/api/v1/listings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1));

        // Delete account
        mockMvc.perform(delete("/api/v1/account")
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isOk());

        // Verify listing is not visible after deletion
        mockMvc.perform(get("/api/v1/listings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(0));
    }

    @Test
    void getListings_filterByState_excludesDeletedSellerListings() throws Exception {
        // Create active seller with listing
        String activeSellerToken = registerAndGetToken("active@test.com", "Password123!");
        User activeSeller = userRepository.findByEmail("active@test.com").orElseThrow();

        // Create deleted seller with listing
        String deletedSellerToken = registerAndGetToken("deleted@test.com", "Password123!");
        User deletedSeller = userRepository.findByEmail("deleted@test.com").orElseThrow();

        State state = createTestState("Lagos");

        Listing activeListing = createTestListing(activeSeller, ListingType.VEHICLE);
        activeListing.setStatus(ListingStatus.ACTIVE);
        activeListing.setState(state);

        Listing deletedSellerListing = createTestListing(deletedSeller, ListingType.VEHICLE);
        deletedSellerListing.setStatus(ListingStatus.ACTIVE);
        deletedSellerListing.setState(state);

        listingRepository.saveAll(List.of(activeListing, deletedSellerListing));

        // Delete one seller
        mockMvc.perform(delete("/api/v1/account")
                        .header("Authorization", authHeader(deletedSellerToken)))
                .andExpect(status().isOk());

        // Verify only active seller's listing is visible
        mockMvc.perform(get("/api/v1/listings")
                        .param("stateId", state.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(activeListing.getId().toString()));
    }
}
```

---

### 3.5 Favorites Behavior

**Test Class:** `AccountDeletionIntegrationTest`

| Test ID | Scenario | Input | Expected Result |
|---------|----------|-------|-----------------|
| DEL-040 | User's favorites are deleted | Delete account | No favorites remain |
| DEL-041 | No orphan favorite records | After deletion | favoriteRepository count = 0 for user |
| DEL-042 | Other users' favorites on user's listings remain | Delete seller | Buyer favorites deleted with listing |

```java
@Nested
@DisplayName("Favorites Behavior")
class FavoritesTests {

    @Test
    void deleteAccount_favoritesDeleted() throws Exception {
        // Setup: buyer favorites seller's listing
        User seller = createTestUser("seller@test.com", Role.USER);
        Listing listing = createTestListing(seller, ListingType.VEHICLE);
        listing.setStatus(ListingStatus.ACTIVE);
        listingRepository.save(listing);

        String buyerToken = registerAndGetToken("buyer@test.com", "Password123!");
        User buyer = userRepository.findByEmail("buyer@test.com").orElseThrow();

        Favorite favorite = createTestFavorite(buyer, listing);
        UUID favoriteId = favorite.getId();

        // Verify favorite exists
        assertThat(favoriteRepository.findById(favoriteId)).isPresent();

        // Delete buyer account
        mockMvc.perform(delete("/api/v1/account")
                        .header("Authorization", authHeader(buyerToken)))
                .andExpect(status().isOk());

        // Verify favorite is deleted
        entityManager.clear();
        assertThat(favoriteRepository.findById(favoriteId)).isEmpty();
    }

    @Test
    void deleteAccount_noOrphanFavoriteRecords() throws Exception {
        // Setup: buyer has multiple favorites
        User seller = createTestUser("seller@test.com", Role.USER);
        Listing listing1 = createTestListing(seller, ListingType.VEHICLE);
        Listing listing2 = createTestListing(seller, ListingType.PART);
        listing1.setStatus(ListingStatus.ACTIVE);
        listing2.setStatus(ListingStatus.ACTIVE);
        listingRepository.saveAll(List.of(listing1, listing2));

        String buyerToken = registerAndGetToken("buyer@test.com", "Password123!");
        User buyer = userRepository.findByEmail("buyer@test.com").orElseThrow();
        UUID buyerId = buyer.getId();

        createTestFavorite(buyer, listing1);
        createTestFavorite(buyer, listing2);

        // Verify favorites exist
        assertThat(favoriteRepository.countByUserId(buyerId)).isEqualTo(2);

        // Delete buyer account
        mockMvc.perform(delete("/api/v1/account")
                        .header("Authorization", authHeader(buyerToken)))
                .andExpect(status().isOk());

        // Verify no favorites remain for this user
        // Note: User is deleted, so we check by direct query
        entityManager.clear();
        long remainingFavorites = favoriteRepository.findByUserId(buyerId).size();
        assertThat(remainingFavorites).isZero();
    }
}
```

---

### 3.6 Messages / Inquiries Preservation

**Test Class:** `AccountDeletionIntegrationTest`

| Test ID | Scenario | Input | Expected Result |
|---------|----------|-------|-----------------|
| DEL-050 | Messages preserved after deletion | Delete buyer | Messages still exist |
| DEL-051 | Messages preserved when seller deleted | Delete seller | Messages still exist |
| DEL-052 | Message integrity maintained | Delete account | All message fields intact |

```java
@Nested
@DisplayName("Messages Preservation")
class MessagesTests {

    @Test
    void deleteAccount_buyerMessagesPreserved() throws Exception {
        // Setup: buyer sends inquiry
        String sellerToken = registerAndGetToken("seller@test.com", "Password123!");
        User seller = userRepository.findByEmail("seller@test.com").orElseThrow();
        Listing listing = createTestListing(seller, ListingType.VEHICLE);
        listing.setStatus(ListingStatus.ACTIVE);
        listingRepository.save(listing);

        String buyerToken = registerAndGetToken("buyer@test.com", "Password123!");
        User buyer = userRepository.findByEmail("buyer@test.com").orElseThrow();

        ContactRequest inquiry = createTestInquiry(listing, buyer, "Is this available?");
        UUID inquiryId = inquiry.getId();

        // Delete buyer account
        mockMvc.perform(delete("/api/v1/account")
                        .header("Authorization", authHeader(buyerToken)))
                .andExpect(status().isOk());

        // Verify message still exists
        entityManager.clear();
        Optional<ContactRequest> preserved = contactRequestRepository.findById(inquiryId);
        assertThat(preserved).isPresent();
        assertThat(preserved.get().getMessage()).isEqualTo("Is this available?");
    }

    @Test
    void deleteAccount_sellerMessagesPreserved() throws Exception {
        // Setup: seller has listings with inquiries
        String sellerToken = registerAndGetToken("seller@test.com", "Password123!");
        User seller = userRepository.findByEmail("seller@test.com").orElseThrow();
        Listing listing = createTestListing(seller, ListingType.VEHICLE);
        listing.setStatus(ListingStatus.ACTIVE);
        listingRepository.save(listing);

        User buyer = createTestUser("buyer@test.com", Role.USER);
        ContactRequest inquiry = createTestInquiry(listing, buyer, "Interested in buying");
        UUID inquiryId = inquiry.getId();

        // Delete seller account
        mockMvc.perform(delete("/api/v1/account")
                        .header("Authorization", authHeader(sellerToken)))
                .andExpect(status().isOk());

        // Verify message still exists (listing becomes DELETED but inquiry preserved)
        entityManager.clear();
        Optional<ContactRequest> preserved = contactRequestRepository.findById(inquiryId);
        assertThat(preserved).isPresent();
    }
}
```

---

### 3.7 Data Integrity

**Test Class:** `DeleteAccountDataIntegrityTest`

| Test ID | Scenario | Input | Expected Result |
|---------|----------|-------|-----------------|
| DEL-060 | No foreign key violations | Delete account | No DB errors |
| DEL-061 | No orphaned listing records | After deletion | Listings still have valid structure |
| DEL-062 | No orphaned image records | After deletion | Images linked to listings |
| DEL-063 | Transaction rollback on failure | Simulated error | All changes reverted |

```java
@Nested
@DisplayName("Data Integrity")
class DataIntegrityTests {

    @Test
    void deleteAccount_noForeignKeyViolations() throws Exception {
        // Setup: user with full data
        String token = registerAndGetToken("user@test.com", "Password123!");
        User user = userRepository.findByEmail("user@test.com").orElseThrow();
        State state = createTestState("Lagos");

        Listing listing = createTestListing(user, ListingType.VEHICLE);
        listing.setStatus(ListingStatus.ACTIVE);
        listing.setState(state);
        listingRepository.save(listing);

        // Add image to listing
        ListingImage image = ListingImage.builder()
                .listing(listing)
                .imageUrl("https://s3.amazonaws.com/test/image.jpg")
                .s3Key("listings/test/image.jpg")
                .displayOrder(1)
                .primary(true)
                .build();
        listingImageRepository.save(image);

        // Delete should not throw FK violation
        assertDoesNotThrow(() -> {
            mockMvc.perform(delete("/api/v1/account")
                            .header("Authorization", authHeader(token)))
                    .andExpect(status().isOk());
        });
    }

    @Test
    void deleteAccount_listingsStillHaveValidStructure() throws Exception {
        String token = registerAndGetToken("seller@test.com", "Password123!");
        User seller = userRepository.findByEmail("seller@test.com").orElseThrow();
        State state = createTestState("Lagos");

        Listing listing = createTestListing(seller, ListingType.VEHICLE);
        listing.setStatus(ListingStatus.ACTIVE);
        listing.setState(state);
        listingRepository.save(listing);
        UUID listingId = listing.getId();

        // Delete account
        mockMvc.perform(delete("/api/v1/account")
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isOk());

        // Verify listing structure is intact (just status changed)
        entityManager.clear();
        Listing deletedListing = listingRepository.findById(listingId).orElseThrow();
        assertThat(deletedListing.getTitle()).isNotNull();
        assertThat(deletedListing.getPrice()).isNotNull();
        assertThat(deletedListing.getState()).isNotNull();
    }
}
```

---

### 3.8 Security

**Test Class:** `AccountDeletionIntegrationTest`

| Test ID | Scenario | Input | Expected Result |
|---------|----------|-------|-----------------|
| DEL-070 | Cannot delete another user's account | Different user token | 401 Unauthorized (endpoint uses @CurrentUser) |
| DEL-071 | Only authenticated users can delete | No token | 401 Unauthorized |
| DEL-072 | Admin cannot delete other users via this endpoint | Admin token | Deletes admin's own account |

```java
@Nested
@DisplayName("Security")
class SecurityTests {

    @Test
    void deleteAccount_onlyDeletesOwnAccount() throws Exception {
        // Create two users
        String user1Token = registerAndGetToken("user1@test.com", "Password123!");
        String user2Token = registerAndGetToken("user2@test.com", "Password123!");

        User user1 = userRepository.findByEmail("user1@test.com").orElseThrow();
        UUID user1Id = user1.getId();

        // User2 tries to delete (can only delete own account)
        mockMvc.perform(delete("/api/v1/account")
                        .header("Authorization", authHeader(user2Token)))
                .andExpect(status().isOk());

        // Verify user1 is still active
        Optional<User> stillActive = userRepository.findById(user1Id);
        assertThat(stillActive).isPresent();
        assertThat(stillActive.get().isEnabled()).isTrue();
    }

    @Test
    void deleteAccount_requiresAuthentication() throws Exception {
        mockMvc.perform(delete("/api/v1/account"))
                .andExpect(status().isUnauthorized());
    }
}
```

---

### 3.9 Edge Cases

**Test Class:** `AccountDeletionIntegrationTest`

| Test ID | Scenario | Input | Expected Result |
|---------|----------|-------|-----------------|
| DEL-080 | Delete user with no listings | New user | Success |
| DEL-081 | Delete user with many listings | 50+ listings | All marked DELETED |
| DEL-082 | Delete user with no favorites | New user | Success |
| DEL-083 | Delete user with mixed data | Listings + favorites + messages | All handled correctly |

```java
@Nested
@DisplayName("Edge Cases")
class EdgeCaseTests {

    @Test
    void deleteAccount_userWithNoListings_succeeds() throws Exception {
        String token = registerAndGetToken("user@test.com", "Password123!");

        mockMvc.perform(delete("/api/v1/account")
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isOk());
    }

    @Test
    void deleteAccount_userWithManyListings_allMarkedDeleted() throws Exception {
        String token = registerAndGetToken("seller@test.com", "Password123!");
        User seller = userRepository.findByEmail("seller@test.com").orElseThrow();
        State state = createTestState("Lagos");

        // Create 20 listings
        List<Listing> listings = IntStream.range(0, 20)
                .mapToObj(i -> {
                    Listing listing = createTestListing(seller, ListingType.VEHICLE);
                    listing.setTitle("Listing " + i);
                    listing.setState(state);
                    listing.setStatus(ListingStatus.ACTIVE);
                    return listing;
                })
                .toList();
        listingRepository.saveAll(listings);

        // Delete account
        mockMvc.perform(delete("/api/v1/account")
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isOk());

        // Verify all listings marked as DELETED
        entityManager.clear();
        long deletedCount = listings.stream()
                .map(l -> listingRepository.findById(l.getId()).orElseThrow())
                .filter(l -> l.getStatus() == ListingStatus.DELETED)
                .count();
        assertThat(deletedCount).isEqualTo(20);
    }

    @Test
    void deleteAccount_mixedData_handledCorrectly() throws Exception {
        // Setup: user with listings, favorites, and messages
        String token = registerAndGetToken("user@test.com", "Password123!");
        User user = userRepository.findByEmail("user@test.com").orElseThrow();
        State state = createTestState("Lagos");

        // Own listing
        Listing ownListing = createTestListing(user, ListingType.VEHICLE);
        ownListing.setState(state);
        ownListing.setStatus(ListingStatus.ACTIVE);
        listingRepository.save(ownListing);

        // Favorite another user's listing
        User otherSeller = createTestUser("other@test.com", Role.USER);
        Listing otherListing = createTestListing(otherSeller, ListingType.PART);
        otherListing.setStatus(ListingStatus.ACTIVE);
        listingRepository.save(otherListing);
        createTestFavorite(user, otherListing);

        // Receive inquiry on own listing
        User buyer = createTestUser("buyer@test.com", Role.USER);
        createTestInquiry(ownListing, buyer, "Interested");

        UUID ownListingId = ownListing.getId();
        UUID favoriteListingId = otherListing.getId();

        // Delete account
        mockMvc.perform(delete("/api/v1/account")
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isOk());

        // Verify: own listing DELETED, favorites removed, messages preserved
        entityManager.clear();
        assertThat(listingRepository.findById(ownListingId).orElseThrow().getStatus())
                .isEqualTo(ListingStatus.DELETED);
        assertThat(favoriteRepository.existsByUserIdAndListingId(user.getId(), favoriteListingId))
                .isFalse();
        assertThat(contactRequestRepository.count()).isGreaterThan(0); // Messages preserved
    }
}
```

---

### 3.10 Cache Impact

**Test Class:** `AccountDeletionIntegrationTest`

| Test ID | Scenario | Input | Expected Result |
|---------|----------|-------|-----------------|
| DEL-090 | Cached user data not returned after deletion | Cache + delete | Cache miss or filtered |
| DEL-091 | Location cache unaffected by user deletion | Delete user | States/axes/areas still cached |

```java
@Nested
@DisplayName("Cache Impact")
class CacheImpactTests {

    @Test
    void deleteAccount_locationCacheUnaffected() throws Exception {
        // Setup: cache states
        State state = createTestState("Lagos");
        locationCacheService.getStates(); // Populate cache

        String token = registerAndGetToken("user@test.com", "Password123!");

        // Delete account
        mockMvc.perform(delete("/api/v1/account")
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isOk());

        // Verify location cache still works
        List<SimpleNode> states = locationCacheService.getStates();
        assertThat(states).hasSize(1);
        assertThat(states.get(0).getName()).isEqualTo("Lagos");
    }
}
```

---

## 4. Data Integrity Checks

| Check | Query/Assertion | Expected |
|-------|-----------------|----------|
| User enabled = false | `findByIdIncludingDeleted().isEnabled()` | false |
| User deletedAt set | `findByIdIncludingDeleted().getDeletedAt()` | != null |
| Listings status = DELETED | `findById().getStatus()` | DELETED (except SOLD) |
| Favorites removed | `countByUserId()` | 0 |
| Messages preserved | `countByListingId()` | > 0 |
| No FK violations | Transaction succeeds | No exceptions |

---

## 5. Security Validation

| Validation | Test | Expected |
|------------|------|----------|
| Authentication required | No token | 401 |
| Own account only | Different user | Success (deletes own) |
| Deleted user blocked | Login attempt | 401 |
| JWT invalidated | Use old token | 401 |

---

## 6. Risks & Gaps

### 6.1 Known Limitations

| Risk | Mitigation |
|------|------------|
| Re-registration with same email creates new user | By design (soft delete) |
| Historical data references deleted user | User data preserved, just filtered |
| Token not revoked server-side | JWT validation checks deletedAt |

### 6.2 Recommendations

1. **Token Blacklist**: Consider implementing JWT blacklist for immediate token revocation
2. **Scheduled Cleanup**: Implement job to hard-delete data after retention period
3. **Audit Log**: Log account deletion events for compliance
4. **Email Notification**: Send confirmation email on deletion

---

## 7. Test Execution

```bash
# Run all delete account tests
mvn test -Dtest=AccountDeletionIntegrationTest

# Run specific test group
mvn test -Dtest=AccountDeletionIntegrationTest$DeletionFlowTests

# Run with verbose output
mvn test -Dtest=AccountDeletionIntegrationTest -Dspring.profiles.active=test
```

---

## 8. Summary

This test plan covers:
- **50+ test scenarios** for delete account feature
- **Authentication blocking** verification
- **Global filtering enforcement** (critical)
- **Listing visibility** after deletion
- **Favorites cleanup** verification
- **Message preservation** confirmation
- **Data integrity** validation
- **Security enforcement** checks
- **Edge case handling**

**Implementation Priority:**
1. DEL-001 to DEL-005: Core deletion flow
2. DEL-010 to DEL-013: Authentication blocking
3. DEL-020 to DEL-023: Global filtering
4. DEL-030 to DEL-034: Listing visibility
5. DEL-040 to DEL-042: Favorites cleanup
6. DEL-050 to DEL-052: Message preservation
7. DEL-060 to DEL-063: Data integrity
8. DEL-070 to DEL-072: Security
9. DEL-080 to DEL-083: Edge cases
10. DEL-090 to DEL-091: Cache impact

**Estimated Implementation Time:** 1-2 days for an experienced engineer.
