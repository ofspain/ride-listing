package com.ridelist.controller;

import com.ridelist.dto.request.ChangeListingStatusRequest;
import com.ridelist.dto.response.ApiResponse;
import com.ridelist.dto.response.ListingResponse;
import com.ridelist.dto.response.ListingSummaryResponse;
import com.ridelist.dto.response.PagedResponse;
import com.ridelist.model.ListingCategory;
import com.ridelist.model.ListingStatus;
import com.ridelist.model.ListingType;
import com.ridelist.security.UserPrincipal;
import com.ridelist.service.AdminListingService;
import com.ridelist.util.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/listings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Listings", description = "Admin endpoints for listing management")
public class AdminListingController {

    private final AdminListingService adminListingService;

    @GetMapping
    @Operation(summary = "Get all listings (admin)", description = "Returns paginated listings across ALL sellers with optional filters")
    public ResponseEntity<ApiResponse<PagedResponse<ListingSummaryResponse>>> getListings(
            @Parameter(description = "Filter by title or seller name (case-insensitive)")
            @RequestParam(required = false) String search,
            @Parameter(description = "Filter by listing status")
            @RequestParam(required = false) ListingStatus status,
            @Parameter(description = "Filter by listing type")
            @RequestParam(required = false) ListingType listingType,
            @Parameter(description = "Filter by category")
            @RequestParam(required = false) ListingCategory category,
            @Parameter(description = "Page number (0-indexed)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field and direction (e.g., 'createdAt,desc')")
            @RequestParam(defaultValue = "createdAt,desc") String sort) {

        Pageable pageable = createPageable(page, size, sort);

        PagedResponse<ListingSummaryResponse> response = adminListingService.adminGetListings(
                search, status, listingType, category, pageable);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Change listing status", description = "Changes the status of any listing. Enforces allowed status transitions.")
    public ResponseEntity<ApiResponse<ListingResponse>> changeListingStatus(
            @PathVariable UUID id,
            @Valid @RequestBody ChangeListingStatusRequest request,
            @CurrentUser UserPrincipal principal) {

        ListingResponse response = adminListingService.adminChangeListingStatus(
                id, request.getStatus(), principal.getId());

        return ResponseEntity.ok(ApiResponse.success("Listing status updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete listing", description = "Soft deletes a listing by setting its status to DELETED")
    public ResponseEntity<ApiResponse<Void>> deleteListing(
            @PathVariable UUID id,
            @CurrentUser UserPrincipal principal) {

        adminListingService.adminDeleteListing(id, principal.getId());

        return ResponseEntity.ok(ApiResponse.success("Listing deleted successfully", null));
    }

    private Pageable createPageable(int page, int size, String sort) {
        String[] sortParams = sort.split(",");
        String sortField = sortParams[0];
        Sort.Direction direction = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        return PageRequest.of(page, size, Sort.by(direction, sortField));
    }
}
