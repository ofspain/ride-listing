package com.ridelist.integration;

import com.ridelist.model.*;
import com.ridelist.repository.ListingImageRepository;
import com.ridelist.service.S3Service;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("ImageService Integration Tests")
public class ImageServiceIntegrationTest extends BaseIntegrationTest {

    @MockBean
    private S3Service s3Service;

    @Autowired
    private ListingImageRepository listingImageRepository;

    // ==================== UPLOAD TESTS (IMG-001 to IMG-004) ====================

    @Nested
    @DisplayName("Upload Image Tests")
    class UploadImageTests {

        @Test
        @DisplayName("IMG-001: Upload single valid image returns URL")
        void uploadImages_singleValidFile_returnsUrl() throws Exception {
            // Setup S3 mock
            when(s3Service.uploadFile(any(), anyString()))
                    .thenReturn("https://test-bucket.s3.us-east-1.amazonaws.com/listings/test/image.jpg");

            String token = registerAndGetToken("seller@test.com", "Password123!");
            User seller = userRepository.findByEmail("seller@test.com").orElseThrow();
            Listing listing = createTestListing(seller, ListingType.VEHICLE);

            MockMultipartFile file = new MockMultipartFile(
                    "files", "image.jpg", "image/jpeg", "fake-image-data".getBytes());

            mockMvc.perform(multipart("/api/v1/account/listings/" + listing.getId() + "/images")
                            .file(file)
                            .header("Authorization", authHeader(token)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].imageUrl").exists());

            verify(s3Service, times(1)).uploadFile(any(), anyString());
        }

        @Test
        @DisplayName("IMG-002: Upload multiple images returns multiple URLs")
        void uploadImages_multipleValidFiles_returnsUrls() throws Exception {
            // Setup S3 mock to return different URLs for each call
            when(s3Service.uploadFile(any(), anyString()))
                    .thenReturn("https://test-bucket.s3.us-east-1.amazonaws.com/listings/test/image1.jpg")
                    .thenReturn("https://test-bucket.s3.us-east-1.amazonaws.com/listings/test/image2.jpg")
                    .thenReturn("https://test-bucket.s3.us-east-1.amazonaws.com/listings/test/image3.jpg");

            String token = registerAndGetToken("seller@test.com", "Password123!");
            User seller = userRepository.findByEmail("seller@test.com").orElseThrow();
            Listing listing = createTestListing(seller, ListingType.VEHICLE);

            MockMultipartFile file1 = new MockMultipartFile(
                    "files", "image1.jpg", "image/jpeg", "fake-image-data-1".getBytes());
            MockMultipartFile file2 = new MockMultipartFile(
                    "files", "image2.jpg", "image/jpeg", "fake-image-data-2".getBytes());
            MockMultipartFile file3 = new MockMultipartFile(
                    "files", "image3.jpg", "image/jpeg", "fake-image-data-3".getBytes());

            mockMvc.perform(multipart("/api/v1/account/listings/" + listing.getId() + "/images")
                            .file(file1)
                            .file(file2)
                            .file(file3)
                            .header("Authorization", authHeader(token)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.length()").value(3));

            verify(s3Service, times(3)).uploadFile(any(), anyString());
        }

        @Test
        @DisplayName("IMG-003: Upload max images (10) succeeds")
        void uploadImages_maxCount_succeeds() throws Exception {
            when(s3Service.uploadFile(any(), anyString()))
                    .thenReturn("https://test-bucket.s3.us-east-1.amazonaws.com/listings/test/image.jpg");

            String token = registerAndGetToken("seller@test.com", "Password123!");
            User seller = userRepository.findByEmail("seller@test.com").orElseThrow();
            Listing listing = createTestListing(seller, ListingType.VEHICLE);

            // Create 10 mock files
            MockMultipartFile[] files = new MockMultipartFile[10];
            for (int i = 0; i < 10; i++) {
                files[i] = new MockMultipartFile(
                        "files", "image" + i + ".jpg", "image/jpeg", ("data-" + i).getBytes());
            }

            mockMvc.perform(multipart("/api/v1/account/listings/" + listing.getId() + "/images")
                            .file(files[0]).file(files[1]).file(files[2]).file(files[3]).file(files[4])
                            .file(files[5]).file(files[6]).file(files[7]).file(files[8]).file(files[9])
                            .header("Authorization", authHeader(token)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.length()").value(10));
        }

        @Test
        @DisplayName("IMG-004: Exceed max images returns 400")
        void uploadImages_exceedMaxCount_returns400() throws Exception {
            when(s3Service.uploadFile(any(), anyString()))
                    .thenReturn("https://test-bucket.s3.us-east-1.amazonaws.com/listings/test/image.jpg");

            String token = registerAndGetToken("seller@test.com", "Password123!");
            User seller = userRepository.findByEmail("seller@test.com").orElseThrow();
            Listing listing = createTestListing(seller, ListingType.VEHICLE);

            // Create 11 mock files (exceeds max of 10)
            MockMultipartFile[] files = new MockMultipartFile[11];
            for (int i = 0; i < 11; i++) {
                files[i] = new MockMultipartFile(
                        "files", "image" + i + ".jpg", "image/jpeg", ("data-" + i).getBytes());
            }

            mockMvc.perform(multipart("/api/v1/account/listings/" + listing.getId() + "/images")
                            .file(files[0]).file(files[1]).file(files[2]).file(files[3]).file(files[4])
                            .file(files[5]).file(files[6]).file(files[7]).file(files[8]).file(files[9])
                            .file(files[10])
                            .header("Authorization", authHeader(token)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ==================== VALIDATION TESTS (IMG-005 to IMG-006) ====================

    @Nested
    @DisplayName("File Validation Tests")
    class FileValidationTests {

        @Test
        @DisplayName("IMG-005: Upload invalid format returns 400")
        void uploadImages_invalidFormat_returns400() throws Exception {
            // S3Service.validateFile() throws BadRequestException for invalid types
            when(s3Service.uploadFile(any(), anyString()))
                    .thenThrow(new com.ridelist.exception.BadRequestException("Only JPG and PNG files are allowed"));

            String token = registerAndGetToken("seller@test.com", "Password123!");
            User seller = userRepository.findByEmail("seller@test.com").orElseThrow();
            Listing listing = createTestListing(seller, ListingType.VEHICLE);

            MockMultipartFile file = new MockMultipartFile(
                    "files", "document.txt", "text/plain", "not an image".getBytes());

            mockMvc.perform(multipart("/api/v1/account/listings/" + listing.getId() + "/images")
                            .file(file)
                            .header("Authorization", authHeader(token)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("IMG-006: Upload oversized file returns 400")
        void uploadImages_oversizedFile_returns400() throws Exception {
            // S3Service.validateFile() throws BadRequestException for oversized files
            when(s3Service.uploadFile(any(), anyString()))
                    .thenThrow(new com.ridelist.exception.BadRequestException("File size exceeds maximum limit of 5MB"));

            String token = registerAndGetToken("seller@test.com", "Password123!");
            User seller = userRepository.findByEmail("seller@test.com").orElseThrow();
            Listing listing = createTestListing(seller, ListingType.VEHICLE);

            // Create a mock file that simulates being >5MB (actual validation is in S3Service)
            MockMultipartFile file = new MockMultipartFile(
                    "files", "large-image.jpg", "image/jpeg", "large-data".getBytes());

            mockMvc.perform(multipart("/api/v1/account/listings/" + listing.getId() + "/images")
                            .file(file)
                            .header("Authorization", authHeader(token)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ==================== AUTHORIZATION TESTS (IMG-007) ====================

    @Nested
    @DisplayName("Authorization Tests")
    class AuthorizationTests {

        @Test
        @DisplayName("IMG-007: Upload to non-owned listing returns 401")
        void uploadImages_nonOwner_returns401() throws Exception {
            User seller1 = createTestUser("seller1@test.com", Role.USER);
            Listing listing = createTestListing(seller1, ListingType.VEHICLE);

            // Try to upload as different user
            String otherUserToken = registerAndGetToken("other@test.com", "Password123!");

            MockMultipartFile file = new MockMultipartFile(
                    "files", "image.jpg", "image/jpeg", "fake-image-data".getBytes());

            mockMvc.perform(multipart("/api/v1/account/listings/" + listing.getId() + "/images")
                            .file(file)
                            .header("Authorization", authHeader(otherUserToken)))
                    .andExpect(status().isUnauthorized());

            // Verify S3 was never called
            verify(s3Service, never()).uploadFile(any(), anyString());
        }

        @Test
        @DisplayName("IMG-007b: Upload without authentication returns 401")
        void uploadImages_noAuth_returns401() throws Exception {
            User seller = createTestUser("seller@test.com", Role.USER);
            Listing listing = createTestListing(seller, ListingType.VEHICLE);

            MockMultipartFile file = new MockMultipartFile(
                    "files", "image.jpg", "image/jpeg", "fake-image-data".getBytes());

            mockMvc.perform(multipart("/api/v1/account/listings/" + listing.getId() + "/images")
                            .file(file))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("IMG-007c: Upload to non-existent listing returns 404")
        void uploadImages_nonExistentListing_returns404() throws Exception {
            String token = registerAndGetToken("seller@test.com", "Password123!");

            MockMultipartFile file = new MockMultipartFile(
                    "files", "image.jpg", "image/jpeg", "fake-image-data".getBytes());

            mockMvc.perform(multipart("/api/v1/account/listings/" + UUID.randomUUID() + "/images")
                            .file(file)
                            .header("Authorization", authHeader(token)))
                    .andExpect(status().isNotFound());
        }
    }

    // ==================== PRIMARY IMAGE TESTS (IMG-008) ====================

    @Nested
    @DisplayName("Primary Image Tests")
    class PrimaryImageTests {

        @Test
        @DisplayName("IMG-008: First image becomes primary")
        void uploadImages_firstImage_becomesPrimary() throws Exception {
            when(s3Service.uploadFile(any(), anyString()))
                    .thenReturn("https://test-bucket.s3.us-east-1.amazonaws.com/listings/test/image.jpg");

            String token = registerAndGetToken("seller@test.com", "Password123!");
            User seller = userRepository.findByEmail("seller@test.com").orElseThrow();
            Listing listing = createTestListing(seller, ListingType.VEHICLE);

            MockMultipartFile file = new MockMultipartFile(
                    "files", "image.jpg", "image/jpeg", "fake-image-data".getBytes());

            mockMvc.perform(multipart("/api/v1/account/listings/" + listing.getId() + "/images")
                            .file(file)
                            .header("Authorization", authHeader(token)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data[0].primary").value(true));
        }

        @Test
        @DisplayName("IMG-008b: Second image is not primary")
        void uploadImages_secondImage_isNotPrimary() throws Exception {
            when(s3Service.uploadFile(any(), anyString()))
                    .thenReturn("https://test-bucket.s3.us-east-1.amazonaws.com/listings/test/image1.jpg")
                    .thenReturn("https://test-bucket.s3.us-east-1.amazonaws.com/listings/test/image2.jpg");

            String token = registerAndGetToken("seller@test.com", "Password123!");
            User seller = userRepository.findByEmail("seller@test.com").orElseThrow();
            Listing listing = createTestListing(seller, ListingType.VEHICLE);

            // Upload first image
            MockMultipartFile file1 = new MockMultipartFile(
                    "files", "image1.jpg", "image/jpeg", "fake-image-data-1".getBytes());

            mockMvc.perform(multipart("/api/v1/account/listings/" + listing.getId() + "/images")
                            .file(file1)
                            .header("Authorization", authHeader(token)))
                    .andExpect(status().isCreated());

            // Upload second image
            MockMultipartFile file2 = new MockMultipartFile(
                    "files", "image2.jpg", "image/jpeg", "fake-image-data-2".getBytes());

            mockMvc.perform(multipart("/api/v1/account/listings/" + listing.getId() + "/images")
                            .file(file2)
                            .header("Authorization", authHeader(token)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data[0].primary").value(false));
        }
    }

    // ==================== DELETE IMAGE TESTS (IMG-009) ====================

    @Nested
    @DisplayName("Delete Image Tests")
    class DeleteImageTests {

        @Test
        @DisplayName("IMG-009: Delete primary reassigns primary to next image")
        void deleteImage_primary_reassignsPrimary() throws Exception {
            when(s3Service.uploadFile(any(), anyString()))
                    .thenReturn("https://test-bucket.s3.us-east-1.amazonaws.com/listings/test/image1.jpg")
                    .thenReturn("https://test-bucket.s3.us-east-1.amazonaws.com/listings/test/image2.jpg");
            doNothing().when(s3Service).deleteFile(anyString());

            String token = registerAndGetToken("seller@test.com", "Password123!");
            User seller = userRepository.findByEmail("seller@test.com").orElseThrow();
            Listing listing = createTestListing(seller, ListingType.VEHICLE);

            // Upload two images
            MockMultipartFile file1 = new MockMultipartFile(
                    "files", "image1.jpg", "image/jpeg", "data1".getBytes());
            MockMultipartFile file2 = new MockMultipartFile(
                    "files", "image2.jpg", "image/jpeg", "data2".getBytes());

            mockMvc.perform(multipart("/api/v1/account/listings/" + listing.getId() + "/images")
                            .file(file1)
                            .file(file2)
                            .header("Authorization", authHeader(token)))
                    .andExpect(status().isCreated());

            // Get the primary image ID
            var images = listingImageRepository.findByListingIdOrderByDisplayOrderAsc(listing.getId());
            assertThat(images).hasSize(2);

            ListingImage primaryImage = images.stream().filter(ListingImage::isPrimary).findFirst().orElseThrow();
            ListingImage secondImage = images.stream().filter(i -> !i.isPrimary()).findFirst().orElseThrow();

            // Delete the primary image
            mockMvc.perform(delete("/api/v1/account/listings/images/" + primaryImage.getId())
                            .header("Authorization", authHeader(token)))
                    .andExpect(status().isOk());

            // Verify the second image is now primary
            var updatedSecondImage = listingImageRepository.findById(secondImage.getId()).orElseThrow();
            assertThat(updatedSecondImage.isPrimary()).isTrue();
        }

        @Test
        @DisplayName("IMG-009b: Delete non-primary image keeps primary unchanged")
        void deleteImage_nonPrimary_keepsPrimary() throws Exception {
            when(s3Service.uploadFile(any(), anyString()))
                    .thenReturn("https://test-bucket.s3.us-east-1.amazonaws.com/listings/test/image1.jpg")
                    .thenReturn("https://test-bucket.s3.us-east-1.amazonaws.com/listings/test/image2.jpg");
            doNothing().when(s3Service).deleteFile(anyString());

            String token = registerAndGetToken("seller@test.com", "Password123!");
            User seller = userRepository.findByEmail("seller@test.com").orElseThrow();
            Listing listing = createTestListing(seller, ListingType.VEHICLE);

            // Upload two images
            MockMultipartFile file1 = new MockMultipartFile(
                    "files", "image1.jpg", "image/jpeg", "data1".getBytes());
            MockMultipartFile file2 = new MockMultipartFile(
                    "files", "image2.jpg", "image/jpeg", "data2".getBytes());

            mockMvc.perform(multipart("/api/v1/account/listings/" + listing.getId() + "/images")
                            .file(file1)
                            .file(file2)
                            .header("Authorization", authHeader(token)))
                    .andExpect(status().isCreated());

            var images = listingImageRepository.findByListingIdOrderByDisplayOrderAsc(listing.getId());
            ListingImage primaryImage = images.stream().filter(ListingImage::isPrimary).findFirst().orElseThrow();
            ListingImage nonPrimaryImage = images.stream().filter(i -> !i.isPrimary()).findFirst().orElseThrow();

            // Delete the non-primary image
            mockMvc.perform(delete("/api/v1/account/listings/images/" + nonPrimaryImage.getId())
                            .header("Authorization", authHeader(token)))
                    .andExpect(status().isOk());

            // Verify the primary image is still primary
            var updatedPrimaryImage = listingImageRepository.findById(primaryImage.getId()).orElseThrow();
            assertThat(updatedPrimaryImage.isPrimary()).isTrue();
        }

        @Test
        @DisplayName("IMG-009c: Cannot delete last image (min count = 1)")
        void deleteImage_lastImage_returns400() throws Exception {
            when(s3Service.uploadFile(any(), anyString()))
                    .thenReturn("https://test-bucket.s3.us-east-1.amazonaws.com/listings/test/image.jpg");

            String token = registerAndGetToken("seller@test.com", "Password123!");
            User seller = userRepository.findByEmail("seller@test.com").orElseThrow();
            Listing listing = createTestListing(seller, ListingType.VEHICLE);

            // Upload one image
            MockMultipartFile file = new MockMultipartFile(
                    "files", "image.jpg", "image/jpeg", "data".getBytes());

            mockMvc.perform(multipart("/api/v1/account/listings/" + listing.getId() + "/images")
                            .file(file)
                            .header("Authorization", authHeader(token)))
                    .andExpect(status().isCreated());

            var images = listingImageRepository.findByListingIdOrderByDisplayOrderAsc(listing.getId());
            UUID imageId = images.get(0).getId();

            // Try to delete the only image
            mockMvc.perform(delete("/api/v1/account/listings/images/" + imageId)
                            .header("Authorization", authHeader(token)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("IMG-009d: Delete image by non-owner returns 401")
        void deleteImage_nonOwner_returns401() throws Exception {
            when(s3Service.uploadFile(any(), anyString()))
                    .thenReturn("https://test-bucket.s3.us-east-1.amazonaws.com/listings/test/image1.jpg")
                    .thenReturn("https://test-bucket.s3.us-east-1.amazonaws.com/listings/test/image2.jpg");

            String sellerToken = registerAndGetToken("seller@test.com", "Password123!");
            User seller = userRepository.findByEmail("seller@test.com").orElseThrow();
            Listing listing = createTestListing(seller, ListingType.VEHICLE);

            // Upload images as owner
            MockMultipartFile file1 = new MockMultipartFile(
                    "files", "image1.jpg", "image/jpeg", "data1".getBytes());
            MockMultipartFile file2 = new MockMultipartFile(
                    "files", "image2.jpg", "image/jpeg", "data2".getBytes());

            mockMvc.perform(multipart("/api/v1/account/listings/" + listing.getId() + "/images")
                            .file(file1)
                            .file(file2)
                            .header("Authorization", authHeader(sellerToken)))
                    .andExpect(status().isCreated());

            var images = listingImageRepository.findByListingIdOrderByDisplayOrderAsc(listing.getId());
            UUID imageId = images.get(0).getId();

            // Try to delete as different user
            String otherToken = registerAndGetToken("other@test.com", "Password123!");

            mockMvc.perform(delete("/api/v1/account/listings/images/" + imageId)
                            .header("Authorization", authHeader(otherToken)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("IMG-009e: Delete non-existent image returns 404")
        void deleteImage_nonExistent_returns404() throws Exception {
            String token = registerAndGetToken("seller@test.com", "Password123!");

            mockMvc.perform(delete("/api/v1/account/listings/images/" + UUID.randomUUID())
                            .header("Authorization", authHeader(token)))
                    .andExpect(status().isNotFound());
        }
    }

    // ==================== S3 FAILURE HANDLING (IMG-010) ====================

    @Nested
    @DisplayName("S3 Failure Handling Tests")
    class S3FailureTests {

        @Test
        @DisplayName("IMG-010: S3 failure returns 500 and no images persisted")
        void uploadImages_s3Failure_returns500AndRollsBack() throws Exception {
            // Configure mock to throw exception
            when(s3Service.uploadFile(any(), anyString()))
                    .thenThrow(new RuntimeException("S3 connection failed"));

            String token = registerAndGetToken("seller@test.com", "Password123!");
            User seller = userRepository.findByEmail("seller@test.com").orElseThrow();
            Listing listing = createTestListing(seller, ListingType.VEHICLE);

            MockMultipartFile file = new MockMultipartFile(
                    "files", "image.jpg", "image/jpeg", "fake-image-data".getBytes());

            mockMvc.perform(multipart("/api/v1/account/listings/" + listing.getId() + "/images")
                            .file(file)
                            .header("Authorization", authHeader(token)))
                    .andExpect(status().isInternalServerError());

            // Verify no images were persisted
            var images = listingImageRepository.findByListingIdOrderByDisplayOrderAsc(listing.getId());
            assertThat(images).isEmpty();
        }
    }
}
