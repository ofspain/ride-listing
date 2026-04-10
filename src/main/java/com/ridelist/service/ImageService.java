package com.ridelist.service;

import com.ridelist.config.RideListProperties;
import com.ridelist.dto.mapper.ListingImageMapper;
import com.ridelist.dto.response.ListingImageResponse;
import com.ridelist.exception.BadRequestException;
import com.ridelist.exception.ResourceNotFoundException;
import com.ridelist.exception.UnauthorizedException;
import com.ridelist.model.Listing;
import com.ridelist.model.ListingImage;
import com.ridelist.repository.ListingImageRepository;
import com.ridelist.repository.ListingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ImageService {

    private final ListingImageRepository listingImageRepository;
    private final ListingRepository listingRepository;
    private final S3Service s3Service;
    private final ListingImageMapper listingImageMapper;
    private final RideListProperties imageProperties;

    @Transactional
    public List<ListingImageResponse> uploadListingImages(UUID listingId, List<MultipartFile> files, UUID sellerId) {
        log.info("Uploading {} images for listing: {}", files.size(), listingId);

        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing", "id", listingId));

        if (!listing.getSeller().getId().equals(sellerId)) {
            throw new UnauthorizedException("You are not authorized to upload images for this listing");
        }

        int currentImageCount = listingImageRepository.countByListingId(listingId);
        int newTotalCount = currentImageCount + files.size();

        validateImageCount(files.size(), currentImageCount, newTotalCount);

        List<ListingImage> uploadedImages = new ArrayList<>();
        String folder = "listings/" + listingId;

        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            String imageUrl = s3Service.uploadFile(file, folder);
            String s3Key = extractS3Key(imageUrl, folder);

            ListingImage image = ListingImage.builder()
                    .listing(listing)
                    .imageUrl(imageUrl)
                    .s3Key(s3Key)
                    .displayOrder(currentImageCount + i)
                    .primary(currentImageCount == 0 && i == 0)
                    .build();

            uploadedImages.add(listingImageRepository.save(image));
        }

        log.info("Successfully uploaded {} images for listing: {}", uploadedImages.size(), listingId);

        return uploadedImages.stream()
                .map(listingImageMapper::toResponse)
                .toList();
    }

    @Transactional
    public void deleteImage(UUID imageId, UUID sellerId) {
        log.info("Deleting image: {}", imageId);

        ListingImage image = listingImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image", "id", imageId));

        if (!image.getListing().getSeller().getId().equals(sellerId)) {
            throw new UnauthorizedException("You are not authorized to delete this image");
        }

        UUID listingId = image.getListing().getId();
        int imageCount = listingImageRepository.countByListingId(listingId);

        if (imageCount <= imageProperties.getMinCount()) {
            throw new BadRequestException(
                    String.format("Cannot delete image. Listing must have at least %d image(s)",
                            imageProperties.getMinCount()));
        }

        s3Service.deleteFile(image.getS3Key());
        listingImageRepository.delete(image);

        if (image.isPrimary()) {
            setNewPrimaryImage(listingId);
        }

        log.info("Image deleted successfully: {}", imageId);
    }

    public List<ListingImageResponse> getListingImages(UUID listingId) {
        List<ListingImage> images = listingImageRepository.findByListingIdOrderByDisplayOrderAsc(listingId);
        return images.stream()
                .map(listingImageMapper::toResponse)
                .toList();
    }

    @Transactional
    public void setPrimaryImage(UUID imageId, UUID sellerId) {
        ListingImage image = listingImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image", "id", imageId));

        if (!image.getListing().getSeller().getId().equals(sellerId)) {
            throw new UnauthorizedException("You are not authorized to modify this image");
        }

        UUID listingId = image.getListing().getId();

        listingImageRepository.findByListingIdAndPrimaryTrue(listingId)
                .ifPresent(currentPrimary -> {
                    currentPrimary.setPrimary(false);
                    listingImageRepository.save(currentPrimary);
                });

        image.setPrimary(true);
        listingImageRepository.save(image);

        log.info("Set image {} as primary for listing {}", imageId, listingId);
    }

    private void validateImageCount(int newFilesCount, int currentCount, int newTotalCount) {
        if (newFilesCount < 1) {
            throw new BadRequestException("At least one image is required");
        }

        if (newTotalCount > imageProperties.getMaxCount()) {
            throw new BadRequestException(
                    String.format("Cannot upload %d images. Maximum allowed is %d, current count is %d",
                            newFilesCount, imageProperties.getMaxCount(), currentCount));
        }
    }

    private void setNewPrimaryImage(UUID listingId) {
        List<ListingImage> remainingImages = listingImageRepository.findByListingIdOrderByDisplayOrderAsc(listingId);
        if (!remainingImages.isEmpty()) {
            ListingImage newPrimary = remainingImages.get(0);
            newPrimary.setPrimary(true);
            listingImageRepository.save(newPrimary);
        }
    }

    private String extractS3Key(String imageUrl, String folder) {
        int folderIndex = imageUrl.indexOf(folder);
        if (folderIndex != -1) {
            return imageUrl.substring(folderIndex);
        }
        return imageUrl;
    }
}
