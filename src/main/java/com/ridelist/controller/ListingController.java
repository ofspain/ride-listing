package com.ridelist.controller;

import com.ridelist.dto.request.CreateListingRequest;
import com.ridelist.dto.request.UpdateListingRequest;
import com.ridelist.dto.response.*;
import com.ridelist.exception.ResourceNotFoundException;
import com.ridelist.model.ListingType;
import com.ridelist.model.VehicleType;
import com.ridelist.security.UserPrincipal;
import com.ridelist.model.Listing;
import com.ridelist.repository.ListingRepository;
import com.ridelist.service.ImageService;
import com.ridelist.service.LocationHubService;
import com.ridelist.service.LocationService;
import com.ridelist.service.MarketplaceListingService;
import com.ridelist.util.CurrentUser;
import com.ridelist.util.SlugUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ListingController {

    private final MarketplaceListingService listingService;
    private final ImageService imageService;
    private final LocationService locationService;
    private final LocationHubService locationHubService;
    private final ListingRepository listingRepository;

    // ==================== PUBLIC ENDPOINTS ====================

    private static final String ATTRIBUTE_FILTER_PREFIX = "attr_";

    @GetMapping("/api/v1/listings")
    public ResponseEntity<ApiResponse<PagedResponse<ListingSummaryResponse>>> getListings(
            @RequestParam(required = false) ListingType listingType,
            @RequestParam(required = false) VehicleType vehicleType,
            @RequestParam(required = false) UUID stateId,
            @RequestParam(required = false) UUID axisId,
            @RequestParam(required = false) UUID areaId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(name = "location", required = false) List<String> locationSlugs,
            @RequestParam(name = "q", required = false) String searchQuery,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest request) {

        // Extract attribute filters from query params (e.g., attr_engine-type=150cc&attr_engine-type=200cc)
        Map<String, List<String>> attributeFilters = extractAttributeFilters(request);

        PagedResponse<ListingSummaryResponse> response = listingService.getListings(
                listingType, vehicleType, stateId, axisId, areaId, minPrice, maxPrice,
                locationSlugs, attributeFilters, searchQuery, pageable);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private Map<String, List<String>> extractAttributeFilters(HttpServletRequest request) {
        Map<String, List<String>> attributeFilters = new HashMap<>();
        request.getParameterMap().forEach((key, values) -> {
            if (key.startsWith(ATTRIBUTE_FILTER_PREFIX) && values.length > 0) {
                String attributeSlug = key.substring(ATTRIBUTE_FILTER_PREFIX.length());
                attributeFilters.put(attributeSlug, Arrays.asList(values));
            }
        });
        return attributeFilters;
    }

    @GetMapping("/api/v1/listings/ref/{ref}")
    public ResponseEntity<ApiResponse<ListingResponse>> getListingByRef(@PathVariable String ref) {
        Integer listingNumber = SlugUtil.extractListingNumber(ref);

        if (listingNumber == null) {
            throw new ResourceNotFoundException("Listing", "ref", ref);
        }

        ListingResponse response = listingService.getListingByNumber(listingNumber);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/api/v1/listings/browse/{categoryPath}")
    public ResponseEntity<ApiResponse<PagedResponse<ListingSummaryResponse>>> browseListings(
            @PathVariable String categoryPath,
            @RequestParam(required = false) String stateSlug,
            @RequestParam(required = false) String axisSlug,
            @RequestParam(required = false) String areaSlug,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(name = "location", required = false) List<String> locationSlugs,
            @RequestParam(name = "q", required = false) String searchQuery,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest request) {

        SlugUtil.CategoryResolution category = SlugUtil.resolveCategoryPath(categoryPath);

        if (category == null) {
            throw new ResourceNotFoundException("Category", "path", categoryPath);
        }

        LocationResolution location = locationService.resolveSlugPath(stateSlug, axisSlug, areaSlug);

        Map<String, List<String>> attributeFilters = extractAttributeFilters(request);

        PagedResponse<ListingSummaryResponse> response = listingService.getListings(
                category.listingType(),
                category.vehicleType(),
                location.stateId(),
                location.axisId(),
                location.areaId(),
                minPrice,
                maxPrice,
                locationSlugs,
                attributeFilters,
                searchQuery,
                pageable
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * SEO-friendly browse endpoint with location path segments.
     * URL pattern: /browse/{category}/{state}/{axis?}/{area?}
     *
     * Examples:
     * - /browse/motorcycles/lagos
     * - /browse/motorcycles/lagos/ikeja
     * - /browse/motorcycles/lagos/ikeja/opebi
     * - /browse/all/lagos (all categories in Lagos)
     * - /browse/all/lagos/ikeja (all categories in Ikeja, Lagos)
     *
     * The locationPath is parsed as: state-slug/axis-slug/area-slug
     * When categoryPath is "all", no listingType or vehicleType filter is applied.
     */
    @GetMapping("/api/v1/listings/browse/{categoryPath}/{locationPath:.*}")
    public ResponseEntity<ApiResponse<PagedResponse<ListingSummaryResponse>>> browseListingsWithLocationPath(
            @PathVariable String categoryPath,
            @PathVariable String locationPath,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(name = "location", required = false) List<String> locationSlugs,
            @RequestParam(name = "q", required = false) String searchQuery,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest request) {

        SlugUtil.CategoryResolution category = SlugUtil.resolveCategoryPath(categoryPath);

        if (category == null) {
            throw new ResourceNotFoundException("Category", "path", categoryPath);
        }

        // Parse locationPath: "lagos" or "lagos/ikeja" or "lagos/ikeja/opebi"
        String[] locationSegments = locationPath.split("/");
        String stateSlug = locationSegments.length > 0 ? locationSegments[0] : null;
        String axisSlug = locationSegments.length > 1 ? locationSegments[1] : null;
        String areaSlug = locationSegments.length > 2 ? locationSegments[2] : null;

        LocationResolution location = locationService.resolveSlugPath(stateSlug, axisSlug, areaSlug);

        Map<String, List<String>> attributeFilters = extractAttributeFilters(request);

        PagedResponse<ListingSummaryResponse> response = listingService.getListings(
                category.listingType(),
                category.vehicleType(),
                location.stateId(),
                location.axisId(),
                location.areaId(),
                minPrice,
                maxPrice,
                locationSlugs,
                attributeFilters,
                searchQuery,
                pageable
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/api/v1/listings/browse/{categoryPath}/meta")
    public ResponseEntity<ApiResponse<BrowsePageMeta>> getBrowsePageMeta(
            @PathVariable String categoryPath,
            @RequestParam(required = false) String stateSlug,
            @RequestParam(required = false) String axisSlug,
            @RequestParam(required = false) String areaSlug) {

        SlugUtil.CategoryResolution category = SlugUtil.resolveCategoryPath(categoryPath);

        if (category == null) {
            throw new ResourceNotFoundException("Category", "path", categoryPath);
        }

        String locationLabel = buildLocationLabel(stateSlug, axisSlug, areaSlug);
        String categoryLabel = buildCategoryLabel(category);

        String title = categoryLabel + " for Sale in " + locationLabel;
        String description = buildDescription(categoryLabel, locationLabel, category);

        // For "all" category, omit "all" from canonical URL for cleaner SEO
        String canonicalUrl = buildCanonicalUrl(categoryPath, category.isAll(), stateSlug, axisSlug, areaSlug);

        BrowsePageMeta meta = new BrowsePageMeta(title, description, canonicalUrl, locationLabel, categoryLabel);
        return ResponseEntity.ok(ApiResponse.success(meta));
    }

    @GetMapping("/api/v1/listings/browse/{categoryPath}/locations")
    public ResponseEntity<ApiResponse<LocationHubResponse>> getLocationHub(
            @PathVariable String categoryPath,
            @RequestParam(required = false) String stateSlug,
            @RequestParam(required = false) String axisSlug) {

        SlugUtil.CategoryResolution category = SlugUtil.resolveCategoryPath(categoryPath);

        if (category == null) {
            throw new ResourceNotFoundException("Category", "path", categoryPath);
        }

        LocationHubResponse response = locationHubService.getLocationHub(
                categoryPath,
                category.listingType(),
                category.vehicleType(),
                stateSlug,
                axisSlug
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/api/v1/listings/{idOrRef}")
    public ResponseEntity<ApiResponse<ListingResponse>> getListing(@PathVariable String idOrRef) {
        if (isUUID(idOrRef)) {
            ListingResponse response = listingService.getListingById(UUID.fromString(idOrRef));
            return ResponseEntity.ok(ApiResponse.success(response));
        }

        Integer listingNumber = SlugUtil.extractListingNumber(idOrRef);
        if (listingNumber != null) {
            ListingResponse response = listingService.getListingByNumber(listingNumber);
            return ResponseEntity.ok(ApiResponse.success(response));
        }

        throw new ResourceNotFoundException("Listing", "id", idOrRef);
    }

    private boolean isUUID(String value) {
        return value != null && value.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    }

    private String buildLocationLabel(String stateSlug, String axisSlug, String areaSlug) {
        if (areaSlug != null && !areaSlug.isBlank()) {
            return SlugUtil.fromSlug(areaSlug) + ", " + SlugUtil.fromSlug(stateSlug);
        }
        if (axisSlug != null && !axisSlug.isBlank()) {
            return SlugUtil.fromSlug(axisSlug) + ", " + SlugUtil.fromSlug(stateSlug);
        }
        if (stateSlug != null && !stateSlug.isBlank()) {
            return SlugUtil.fromSlug(stateSlug);
        }
        return "Nigeria";
    }

    private String buildCategoryLabel(SlugUtil.CategoryResolution category) {
        if (category.isAll()) {
            return "All Listings";
        }
        if (category.listingType() == ListingType.PART) {
            return "Spare Parts & Accessories";
        }
        if (category.vehicleType() == null) {
            return "Vehicles";
        }
        return switch (category.vehicleType()) {
            case MOTORCYCLE -> "Motorcycles";
            case TRICYCLE -> "Tricycles";
            case BICYCLE -> "Bicycles";
        };
    }

    private String buildDescription(String categoryLabel, String locationLabel, SlugUtil.CategoryResolution category) {
        StringBuilder desc = new StringBuilder();

        if (category.isAll()) {
            desc.append("Browse motorcycles, tricycles, bicycles, and spare parts for sale");
        } else {
            desc.append("Browse ").append(categoryLabel.toLowerCase());

            if (category.vehicleType() == VehicleType.MOTORCYCLE) {
                desc.append(" (okada bikes)");
            } else if (category.vehicleType() == VehicleType.TRICYCLE) {
                desc.append(" (keke napep)");
            } else if (category.listingType() == ListingType.PART) {
                desc.append(" and accessories");
            }

            desc.append(" for sale");
        }

        if (!locationLabel.equals("Nigeria")) {
            desc.append(" in ").append(locationLabel);
        } else {
            desc.append(" in Nigeria");
        }

        desc.append(" on RideList. ");
        desc.append("New and tokunbo (fairly used) options from verified dealers.");

        return desc.toString();
    }

    private String buildCanonicalUrl(String categoryPath, boolean isAllCategory, String stateSlug, String axisSlug, String areaSlug) {
        StringBuilder url = new StringBuilder();

        // For "all" category, omit the category from URL for cleaner SEO (e.g., /lagos instead of /all/lagos)
        if (!isAllCategory) {
            url.append("/").append(categoryPath);
        }

        if (stateSlug != null && !stateSlug.isBlank()) {
            url.append("/").append(stateSlug);

            if (axisSlug != null && !axisSlug.isBlank()) {
                url.append("/").append(axisSlug);

                if (areaSlug != null && !areaSlug.isBlank()) {
                    url.append("/").append(areaSlug);
                }
            }
        }

        // Handle root "all" case - return "/" or just the location path
        if (url.isEmpty()) {
            return "/";
        }

        return url.toString();
    }

    @GetMapping("/api/v1/listings/sitemap-data")
    public ResponseEntity<ApiResponse<SitemapData>> getSitemapData() {
        List<SitemapEntry> entries = listingRepository
                .findRecentActiveForSitemap(PageRequest.of(0, 1000))
                .stream()
                .map(this::toSitemapEntry)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(new SitemapData(entries)));
    }

    private SitemapEntry toSitemapEntry(Listing listing) {
        String canonicalUrl = SlugUtil.toListingUrl(
                listing.getListingType(),
                listing.getVehicleType(),
                listing.getState() != null ? listing.getState().getSlug() : null,
                listing.getAxis() != null ? listing.getAxis().getSlug() : null,
                listing.getArea() != null ? listing.getArea().getSlug() : null,
                listing.getListingNumber(),
                listing.getTitle()
        );
        return new SitemapEntry(canonicalUrl, listing.getUpdatedAt());
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
