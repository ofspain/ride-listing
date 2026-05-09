package com.ridelist.service;

import com.ridelist.dto.mapper.ListingMapper;
import com.ridelist.dto.response.ListingResponse;
import com.ridelist.dto.response.ListingSummaryResponse;
import com.ridelist.dto.response.PagedResponse;
import com.ridelist.exception.BadRequestException;
import com.ridelist.exception.ResourceNotFoundException;
import com.ridelist.model.*;
import com.ridelist.repository.ListingRepository;
import com.ridelist.repository.specification.AdminListingSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdminListingService {

    private final ListingRepository listingRepository;
    private final ListingMapper listingMapper;
    private final LocationHubService locationHubService;

    private static final Map<ListingStatus, Set<ListingStatus>> ALLOWED_TRANSITIONS = Map.of(
            ListingStatus.DRAFT, Set.of(ListingStatus.PUBLISHED, ListingStatus.ACTIVE, ListingStatus.DELETED),
            ListingStatus.PUBLISHED, Set.of(ListingStatus.ACTIVE, ListingStatus.EXPIRED, ListingStatus.DELETED),
            ListingStatus.ACTIVE, Set.of(ListingStatus.SOLD, ListingStatus.EXPIRED, ListingStatus.DELETED),
            ListingStatus.SOLD, Set.of(ListingStatus.DELETED),
            ListingStatus.EXPIRED, Set.of(ListingStatus.ACTIVE, ListingStatus.DELETED),
            ListingStatus.DELETED, Set.of()
    );

    public PagedResponse<ListingSummaryResponse> adminGetListings(
            String search,
            ListingStatus status,
            ListingType listingType,
            ListingCategory category,
            Pageable pageable) {

        log.debug("Admin fetching listings with filters - search: {}, status: {}, listingType: {}, category: {}",
                search, status, listingType, category);

        Specification<Listing> spec = AdminListingSpecification.withFilters(
                search,
                status,
                listingType,
                category
        );

        Page<Listing> listingsPage = listingRepository.findAll(spec, pageable);

        return toPagedResponse(listingsPage);
    }

    @Transactional
    public ListingResponse adminChangeListingStatus(UUID listingId, ListingStatus newStatus, UUID adminId) {
        log.info("Admin {} changing listing {} status to {}", adminId, listingId, newStatus);

        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing", "id", listingId));

        ListingStatus currentStatus = listing.getStatus();

        if (!isValidTransition(currentStatus, newStatus)) {
            throw new BadRequestException(
                    String.format("Cannot transition from %s to %s", currentStatus, newStatus));
        }

        listing.setStatus(newStatus);
        Listing updatedListing = listingRepository.save(listing);

        log.info("Admin {} changed listing {} status from {} to {}", adminId, listingId, currentStatus, newStatus);

        locationHubService.evictLocationHubCache();

        return listingMapper.toResponse(updatedListing);
    }

    @Transactional
    public void adminDeleteListing(UUID listingId, UUID adminId) {
        log.info("Admin {} deleting listing {}", adminId, listingId);

        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing", "id", listingId));

        listing.setStatus(ListingStatus.DELETED);
        listingRepository.save(listing);

        log.info("Admin {} deleted listing {}", adminId, listingId);

        locationHubService.evictLocationHubCache();
    }

    private boolean isValidTransition(ListingStatus from, ListingStatus to) {
        Set<ListingStatus> allowedStatuses = ALLOWED_TRANSITIONS.get(from);
        return allowedStatuses != null && allowedStatuses.contains(to);
    }

    private PagedResponse<ListingSummaryResponse> toPagedResponse(Page<Listing> listingsPage) {
        return PagedResponse.<ListingSummaryResponse>builder()
                .content(listingsPage.getContent().stream()
                        .map(listingMapper::toSummaryResponse)
                        .toList())
                .page(listingsPage.getNumber())
                .size(listingsPage.getSize())
                .totalElements(listingsPage.getTotalElements())
                .totalPages(listingsPage.getTotalPages())
                .first(listingsPage.isFirst())
                .last(listingsPage.isLast())
                .build();
    }
}
