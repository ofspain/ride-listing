package com.ridelist.integration;

import com.ridelist.model.*;
import com.ridelist.repository.ContactRequestRepository;
import com.ridelist.repository.ListingImageRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for data integrity constraints.
 * Tests cascading deletes, unique constraints, and foreign key validation.
 *
 * Test IDs: DATA-001 to DATA-022
 */
@DisplayName("Data Integrity Integration Tests")
class DataIntegrityIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ListingImageRepository listingImageRepository;

    @Autowired
    private ContactRequestRepository contactRequestRepository;

    @Autowired
    private EntityManager entityManager;

    // ==================== CASCADING DELETES ====================

    @Nested
    @DisplayName("Cascading Delete Tests")
    class CascadingDeleteTests {

        @Test
        @DisplayName("DATA-001: Delete listing cascades to images")
        void deleteListing_cascadesToImages() {
            // Given: listing with images
            User seller = createTestUser("seller@test.com", Role.USER);
            Listing listing = createTestListing(seller, ListingType.VEHICLE);

            ListingImage image1 = ListingImage.builder()
                    .listing(listing)
                    .imageUrl("https://s3.amazonaws.com/test/image1.jpg")
                    .s3Key("listings/test/image1.jpg")
                    .displayOrder(1)
                    .primary(true)
                    .build();
            listingImageRepository.save(image1);

            ListingImage image2 = ListingImage.builder()
                    .listing(listing)
                    .imageUrl("https://s3.amazonaws.com/test/image2.jpg")
                    .s3Key("listings/test/image2.jpg")
                    .displayOrder(2)
                    .primary(false)
                    .build();
            listingImageRepository.save(image2);

            UUID listingId = listing.getId();
            UUID image1Id = image1.getId();
            UUID image2Id = image2.getId();

            // Verify images exist
            assertThat(listingImageRepository.findById(image1Id)).isPresent();
            assertThat(listingImageRepository.findById(image2Id)).isPresent();

            // When: delete listing
            listingRepository.delete(listing);
            listingRepository.flush();
            entityManager.clear(); // Clear persistence context to see DB state

            // Then: images also deleted (via ON DELETE CASCADE in DB)
            assertThat(listingImageRepository.findById(image1Id)).isEmpty();
            assertThat(listingImageRepository.findById(image2Id)).isEmpty();
            assertThat(listingRepository.findById(listingId)).isEmpty();
        }

        @Test
        @DisplayName("DATA-002: Delete listing cascades to favorites")
        void deleteListing_cascadesToFavorites() {
            // Given: listing with favorites from multiple users
            User seller = createTestUser("seller@test.com", Role.USER);
            User buyer1 = createTestUser("buyer1@test.com", Role.USER);
            User buyer2 = createTestUser("buyer2@test.com", Role.USER);
            Listing listing = createTestListing(seller, ListingType.VEHICLE);

            Favorite fav1 = Favorite.builder().user(buyer1).listing(listing).build();
            Favorite fav2 = Favorite.builder().user(buyer2).listing(listing).build();
            favoriteRepository.save(fav1);
            favoriteRepository.save(fav2);

            UUID listingId = listing.getId();
            UUID fav1Id = fav1.getId();
            UUID fav2Id = fav2.getId();

            // Verify favorites exist
            assertThat(favoriteRepository.findById(fav1Id)).isPresent();
            assertThat(favoriteRepository.findById(fav2Id)).isPresent();

            // When: delete listing
            listingRepository.delete(listing);
            listingRepository.flush();
            entityManager.clear();

            // Then: favorites also deleted (via ON DELETE CASCADE in DB)
            assertThat(favoriteRepository.findById(fav1Id)).isEmpty();
            assertThat(favoriteRepository.findById(fav2Id)).isEmpty();
        }

        @Test
        @DisplayName("DATA-002b: Delete listing cascades to contact requests")
        void deleteListing_cascadesToContactRequests() {
            // Given: listing with contact requests
            User seller = createTestUser("seller@test.com", Role.USER);
            User buyer = createTestUser("buyer@test.com", Role.USER);
            Listing listing = createTestListing(seller, ListingType.VEHICLE);

            ContactRequest request = ContactRequest.builder()
                    .listing(listing)
                    .buyer(buyer)
                    .senderName("Test Buyer")
                    .senderPhone("08012345678")
                    .message("Is this available?")
                    .build();
            contactRequestRepository.save(request);

            UUID requestId = request.getId();

            // Verify contact request exists
            assertThat(contactRequestRepository.findById(requestId)).isPresent();

            // When: delete listing
            listingRepository.delete(listing);
            listingRepository.flush();
            entityManager.clear();

            // Then: contact requests also deleted (via ON DELETE CASCADE in DB)
            assertThat(contactRequestRepository.findById(requestId)).isEmpty();
        }

        @Test
        @DisplayName("DATA-003: Delete state cascades to axes and areas")
        void deleteState_cascadesToAxesAndAreas() {
            // Given: state with axes and areas
            State state = createTestState("Lagos");
            Axis axis1 = createTestAxis("Mainland", state);
            Axis axis2 = createTestAxis("Island", state);
            Area area1 = createTestArea("Yaba", axis1);
            Area area2 = createTestArea("Surulere", axis1);
            Area area3 = createTestArea("Victoria Island", axis2);

            UUID stateId = state.getId();
            UUID axis1Id = axis1.getId();
            UUID axis2Id = axis2.getId();
            UUID area1Id = area1.getId();
            UUID area2Id = area2.getId();
            UUID area3Id = area3.getId();

            // Verify all exist
            assertThat(axisRepository.findById(axis1Id)).isPresent();
            assertThat(areaRepository.findById(area1Id)).isPresent();

            // When: delete state
            stateRepository.delete(state);
            stateRepository.flush();
            entityManager.clear();

            // Then: all axes and areas deleted (via ON DELETE CASCADE in DB)
            assertThat(stateRepository.findById(stateId)).isEmpty();
            assertThat(axisRepository.findById(axis1Id)).isEmpty();
            assertThat(axisRepository.findById(axis2Id)).isEmpty();
            assertThat(areaRepository.findById(area1Id)).isEmpty();
            assertThat(areaRepository.findById(area2Id)).isEmpty();
            assertThat(areaRepository.findById(area3Id)).isEmpty();
        }

        @Test
        @DisplayName("DATA-003b: Delete axis cascades to areas only")
        void deleteAxis_cascadesToAreasOnly() {
            // Given: state with axis and areas
            State state = createTestState("Lagos");
            Axis axis = createTestAxis("Mainland", state);
            Area area1 = createTestArea("Yaba", axis);
            Area area2 = createTestArea("Surulere", axis);

            UUID stateId = state.getId();
            UUID axisId = axis.getId();
            UUID area1Id = area1.getId();
            UUID area2Id = area2.getId();

            // When: delete axis only
            axisRepository.delete(axis);
            axisRepository.flush();
            entityManager.clear();

            // Then: areas deleted (via ON DELETE CASCADE), state remains
            assertThat(stateRepository.findById(stateId)).isPresent();
            assertThat(axisRepository.findById(axisId)).isEmpty();
            assertThat(areaRepository.findById(area1Id)).isEmpty();
            assertThat(areaRepository.findById(area2Id)).isEmpty();
        }

        @Test
        @DisplayName("DATA-004: Delete user cascades to listings")
        void deleteUser_cascadesToListings() {
            // Given: user with listings
            User seller = createTestUser("seller@test.com", Role.USER);
            Listing listing1 = createTestListing(seller, ListingType.VEHICLE);
            Listing listing2 = createTestListing(seller, ListingType.PART);

            UUID userId = seller.getId();
            UUID listing1Id = listing1.getId();
            UUID listing2Id = listing2.getId();

            // Verify listings exist
            assertThat(listingRepository.findById(listing1Id)).isPresent();
            assertThat(listingRepository.findById(listing2Id)).isPresent();

            // When: delete user
            userRepository.delete(seller);
            userRepository.flush();
            entityManager.clear();

            // Then: listings also deleted (via ON DELETE CASCADE in DB)
            assertThat(userRepository.findById(userId)).isEmpty();
            assertThat(listingRepository.findById(listing1Id)).isEmpty();
            assertThat(listingRepository.findById(listing2Id)).isEmpty();
        }

        @Test
        @DisplayName("DATA-004b: Delete user cascades to favorites")
        void deleteUser_cascadesToFavorites() {
            // Given: user with favorites
            User seller = createTestUser("seller@test.com", Role.USER);
            User buyer = createTestUser("buyer@test.com", Role.USER);
            Listing listing = createTestListing(seller, ListingType.VEHICLE);

            Favorite favorite = Favorite.builder().user(buyer).listing(listing).build();
            favoriteRepository.save(favorite);

            UUID buyerId = buyer.getId();
            UUID favoriteId = favorite.getId();

            // Verify favorite exists
            assertThat(favoriteRepository.findById(favoriteId)).isPresent();

            // When: delete buyer (who favorited)
            userRepository.delete(buyer);
            userRepository.flush();
            entityManager.clear();

            // Then: favorites also deleted (via ON DELETE CASCADE), but listing remains
            assertThat(userRepository.findById(buyerId)).isEmpty();
            assertThat(favoriteRepository.findById(favoriteId)).isEmpty();
            assertThat(listingRepository.findById(listing.getId())).isPresent();
        }
    }

    // ==================== UNIQUE CONSTRAINTS ====================

    @Nested
    @DisplayName("Unique Constraint Tests")
    class UniqueConstraintTests {

        @Test
        @DisplayName("DATA-010: Duplicate email throws constraint violation")
        void duplicateEmail_throwsConstraintViolation() {
            // Given: user with email
            createTestUser("duplicate@test.com", Role.USER);

            // When & Then: creating another user with same email fails
            assertThatThrownBy(() -> {
                User duplicate = User.builder()
                        .email("duplicate@test.com")
                        .password("$2a$10$encrypted")
                        .firstName("Another")
                        .lastName("User")
                        .phoneNumber("08087654321")
                        .role(Role.USER)
                        .accountType(AccountType.INDIVIDUAL)
                        .build();
                userRepository.saveAndFlush(duplicate);
            }).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("DATA-011: Duplicate favorite throws constraint violation")
        void duplicateFavorite_throwsConstraintViolation() {
            // Given: existing favorite
            User seller = createTestUser("seller@test.com", Role.USER);
            User buyer = createTestUser("buyer@test.com", Role.USER);
            Listing listing = createTestListing(seller, ListingType.VEHICLE);

            favoriteRepository.saveAndFlush(Favorite.builder().user(buyer).listing(listing).build());

            // When & Then: creating duplicate favorite fails
            assertThatThrownBy(() -> {
                favoriteRepository.saveAndFlush(Favorite.builder().user(buyer).listing(listing).build());
            }).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("DATA-012: Duplicate state slug throws constraint violation")
        void duplicateStateSlug_throwsConstraintViolation() {
            // Given: state with slug
            createTestState("Lagos"); // creates slug "lagos"

            // When & Then: creating state with same slug fails
            assertThatThrownBy(() -> {
                State duplicate = State.builder()
                        .name("Lagos Duplicate")
                        .slug("lagos") // same slug
                        .build();
                stateRepository.saveAndFlush(duplicate);
            }).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("DATA-012b: Duplicate axis slug throws constraint violation")
        void duplicateAxisSlug_throwsConstraintViolation() {
            // Given: axis with slug
            State state = createTestState("Lagos");
            createTestAxis("Mainland", state); // creates slug "mainland"

            // When & Then: creating axis with same slug fails
            assertThatThrownBy(() -> {
                Axis duplicate = Axis.builder()
                        .name("Mainland Duplicate")
                        .slug("mainland") // same slug
                        .state(state)
                        .build();
                axisRepository.saveAndFlush(duplicate);
            }).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("DATA-013: Duplicate attribute slug throws constraint violation")
        void duplicateAttributeSlug_throwsConstraintViolation() {
            // Given: attribute with slug
            createTestAttribute("Engine Type", ListingType.VEHICLE, true); // slug "engine-type"

            // When & Then: creating attribute with same slug fails
            assertThatThrownBy(() -> {
                AttributeDefinition duplicate = AttributeDefinition.builder()
                        .name("Engine Type Duplicate")
                        .slug("engine-type") // same slug
                        .listingTypes(java.util.Set.of(ListingType.VEHICLE))
                        .filterable(false)
                        .required(false)
                        .active(true)
                        .build();
                attributeRepository.saveAndFlush(duplicate);
            }).isInstanceOf(DataIntegrityViolationException.class);
        }
    }

    // ==================== FOREIGN KEY VALIDATION ====================

    @Nested
    @DisplayName("Foreign Key Validation Tests")
    class ForeignKeyValidationTests {

        @Test
        @DisplayName("DATA-020: Listing with non-existent state throws violation")
        void createListing_invalidStateId_throws() {
            // Given: seller and a fake state reference
            User seller = createTestUser("seller@test.com", Role.USER);

            Listing listing = Listing.builder()
                    .title("Test Listing")
                    .description("Test description")
                    .price(BigDecimal.valueOf(100000))
                    .seller(seller)
                    .listingType(ListingType.VEHICLE)
                    .vehicleType(VehicleType.MOTORCYCLE)
                    .category(ListingCategory.MOTORCYCLE)
                    .status(ListingStatus.DRAFT)
                    .condition(ListingCondition.GOOD)
                    .build();

            // Create fake state reference with non-existent ID
            State fakeState = new State();
            fakeState.setId(UUID.randomUUID());
            listing.setState(fakeState);

            // When & Then: saving fails due to FK violation
            assertThatThrownBy(() -> {
                listingRepository.saveAndFlush(listing);
            }).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("DATA-021: Axis with non-existent state throws violation")
        void createAxis_invalidStateId_throws() {
            // Given: fake state reference
            State fakeState = new State();
            fakeState.setId(UUID.randomUUID());

            Axis axis = Axis.builder()
                    .name("Test Axis")
                    .slug("test-axis")
                    .state(fakeState)
                    .build();

            // When & Then: saving fails due to FK violation
            assertThatThrownBy(() -> {
                axisRepository.saveAndFlush(axis);
            }).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("DATA-021b: Area with non-existent axis throws violation")
        void createArea_invalidAxisId_throws() {
            // Given: fake axis reference
            Axis fakeAxis = new Axis();
            fakeAxis.setId(UUID.randomUUID());

            Area area = Area.builder()
                    .name("Test Area")
                    .slug("test-area")
                    .axis(fakeAxis)
                    .build();

            // When & Then: saving fails due to FK violation
            assertThatThrownBy(() -> {
                areaRepository.saveAndFlush(area);
            }).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("DATA-022: Favorite with non-existent listing throws violation")
        void createFavorite_invalidListingId_throws() {
            // Given: user and fake listing reference
            User user = createTestUser("user@test.com", Role.USER);

            Listing fakeListing = new Listing();
            fakeListing.setId(UUID.randomUUID());

            Favorite favorite = Favorite.builder()
                    .user(user)
                    .listing(fakeListing)
                    .build();

            // When & Then: saving fails due to FK violation
            assertThatThrownBy(() -> {
                favoriteRepository.saveAndFlush(favorite);
            }).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("DATA-022b: Favorite with non-existent user throws violation")
        void createFavorite_invalidUserId_throws() {
            // Given: listing and fake user reference
            User seller = createTestUser("seller@test.com", Role.USER);
            Listing listing = createTestListing(seller, ListingType.VEHICLE);

            User fakeUser = new User();
            fakeUser.setId(UUID.randomUUID());

            Favorite favorite = Favorite.builder()
                    .user(fakeUser)
                    .listing(listing)
                    .build();

            // When & Then: saving fails due to FK violation
            assertThatThrownBy(() -> {
                favoriteRepository.saveAndFlush(favorite);
            }).isInstanceOf(DataIntegrityViolationException.class);
        }
    }

    // ==================== NOT NULL CONSTRAINTS ====================

    @Nested
    @DisplayName("Not Null Constraint Tests")
    class NotNullConstraintTests {

        @Test
        @DisplayName("Listing without title throws constraint violation")
        void listing_withoutTitle_throws() {
            User seller = createTestUser("seller@test.com", Role.USER);

            Listing listing = Listing.builder()
                    // title is null
                    .description("Test")
                    .price(BigDecimal.valueOf(100000))
                    .seller(seller)
                    .listingType(ListingType.VEHICLE)
                    .vehicleType(VehicleType.MOTORCYCLE)
                    .category(ListingCategory.MOTORCYCLE)
                    .status(ListingStatus.DRAFT)
                    .build();

            assertThatThrownBy(() -> {
                listingRepository.saveAndFlush(listing);
            }).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("User without email throws constraint violation")
        void user_withoutEmail_throws() {
            User user = User.builder()
                    // email is null
                    .password("$2a$10$encrypted")
                    .firstName("Test")
                    .lastName("User")
                    .role(Role.USER)
                    .accountType(AccountType.INDIVIDUAL)
                    .build();

            assertThatThrownBy(() -> {
                userRepository.saveAndFlush(user);
            }).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("State without name throws constraint violation")
        void state_withoutName_throws() {
            State state = State.builder()
                    // name is null
                    .slug("test-slug")
                    .build();

            assertThatThrownBy(() -> {
                stateRepository.saveAndFlush(state);
            }).isInstanceOf(DataIntegrityViolationException.class);
        }
    }
}
