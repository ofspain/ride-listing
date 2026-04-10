package com.ridelist.controller;

import com.ridelist.dto.request.CreateListingRequest;
import com.ridelist.dto.request.UpdateListingRequest;
import com.ridelist.dto.response.ApiResponse;
import com.ridelist.dto.response.ListingImageResponse;
import com.ridelist.dto.response.ListingResponse;
import com.ridelist.dto.response.ListingSummaryResponse;
import com.ridelist.dto.response.PagedResponse;
import com.ridelist.model.ListingType;
import com.ridelist.model.VehicleType;
import com.ridelist.security.UserPrincipal;
import com.ridelist.service.ImageService;
import com.ridelist.service.MarketplaceListingService;
import com.ridelist.util.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ListingController {

    private final MarketplaceListingService listingService;
    private final ImageService imageService;

    // ==================== PUBLIC ENDPOINTS ====================

    @GetMapping("/api/v1/listings")
    public ResponseEntity<ApiResponse<PagedResponse<ListingSummaryResponse>>> getListings(
            @RequestParam(required = false) ListingType listingType,
            @RequestParam(required = false) VehicleType vehicleType,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        PagedResponse<ListingSummaryResponse> response = listingService.getListings(
                listingType, vehicleType, state, minPrice, maxPrice, pageable);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/api/v1/listings/{id}")
    public ResponseEntity<ApiResponse<ListingResponse>> getListingById(@PathVariable UUID id) {
        ListingResponse response = listingService.getListingById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ==================== AUTHENTICATED ENDPOINTS ====================

    @GetMapping("/api/v1/account/listings")
    public ResponseEntity<ApiResponse<PagedResponse<ListingSummaryResponse>>> getMyListings(
            @CurrentUser UserPrincipal principal,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        PagedResponse<ListingSummaryResponse> response = listingService.getSellerListings(principal.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/api/v1/account/listings")
    public ResponseEntity<ApiResponse<ListingResponse>> createListing(
            @CurrentUser UserPrincipal principal,
            @Valid @RequestBody CreateListingRequest request) {

        ListingResponse response = listingService.createListing(request, principal.getId());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Listing created successfully", response));
    }

    @PutMapping("/api/v1/account/listings/{id}")
    public ResponseEntity<ApiResponse<ListingResponse>> updateListing(
            @CurrentUser UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateListingRequest request) {

        ListingResponse response = listingService.updateListing(id, request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Listing updated successfully", response));
    }

    @PostMapping("/api/v1/account/listings/{id}/publish")
    public ResponseEntity<ApiResponse<ListingResponse>> publishListing(
            @CurrentUser UserPrincipal principal,
            @PathVariable UUID id) {

        ListingResponse response = listingService.publishListing(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Listing published successfully", response));
    }

    @PostMapping("/api/v1/account/listings/{id}/mark-sold")
    public ResponseEntity<ApiResponse<ListingResponse>> markAsSold(
            @CurrentUser UserPrincipal principal,
            @PathVariable UUID id) {

        ListingResponse response = listingService.markAsSold(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Listing marked as sold", response));
    }

    @PostMapping(value = "/api/v1/account/listings/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<List<ListingImageResponse>>> uploadImages(
            @CurrentUser UserPrincipal principal,
            @PathVariable UUID id,
            @RequestParam("files") List<MultipartFile> files) {

        List<ListingImageResponse> response = imageService.uploadListingImages(id, files, principal.getId());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Images uploaded successfully", response));
    }

    @DeleteMapping("/api/v1/account/listings/images/{imageId}")
    public ResponseEntity<ApiResponse<Void>> deleteImage(
            @CurrentUser UserPrincipal principal,
            @PathVariable UUID imageId) {

        imageService.deleteImage(imageId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Image deleted successfully", null));
    }
}
