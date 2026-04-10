package com.ridelist.service;

import com.ridelist.dto.mapper.ListingMapper;
import com.ridelist.dto.request.CreateListingRequest;
import com.ridelist.dto.request.UpdateListingRequest;
import com.ridelist.dto.response.ListingResponse;
import com.ridelist.dto.response.ListingSummaryResponse;
import com.ridelist.dto.response.PagedResponse;
import com.ridelist.exception.BadRequestException;
import com.ridelist.exception.ResourceNotFoundException;
import com.ridelist.exception.UnauthorizedException;
import com.ridelist.model.*;
import com.ridelist.repository.ListingRepository;
import com.ridelist.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MarketplaceListingService {

    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final ListingMapper listingMapper;

    @Transactional
    public ListingResponse createListing(CreateListingRequest request, UUID sellerId) {
        log.info("Creating listing for seller: {}", sellerId);

        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", sellerId));

        validateListingRequest(request);

        Listing listing = listingMapper.toEntity(request);
        listing.setSeller(seller);
        listing.setStatus(ListingStatus.DRAFT);

        Listing savedListing = listingRepository.save(listing);
        log.info("Created listing with id: {}", savedListing.getId());

        return listingMapper.toResponse(savedListing);
    }

    @Transactional
    public ListingResponse updateListing(UUID listingId, UpdateListingRequest request, UUID sellerId) {
        log.info("Updating listing: {} for seller: {}", listingId, sellerId);

        Listing listing = getListingForOwner(listingId, sellerId);

        if (listing.getStatus() == ListingStatus.SOLD || listing.getStatus() == ListingStatus.DELETED) {
            throw new BadRequestException("Cannot update a listing that is sold or deleted");
        }

        listingMapper.updateEntityFromRequest(request, listing);

        Listing updatedListing = listingRepository.save(listing);
        log.info("Updated listing: {}", listingId);

        return listingMapper.toResponse(updatedListing);
    }

    @Transactional
    public ListingResponse publishListing(UUID listingId, UUID sellerId) {
        log.info("Publishing listing: {} for seller: {}", listingId, sellerId);

        Listing listing = getListingForOwner(listingId, sellerId);

        if (listing.getStatus() != ListingStatus.DRAFT) {
            throw new BadRequestException("Only draft listings can be published");
        }

        validateListingForPublish(listing);

        listing.setStatus(ListingStatus.ACTIVE);
        Listing publishedListing = listingRepository.save(listing);
        log.info("Published listing: {}", listingId);

        return listingMapper.toResponse(publishedListing);
    }

    @Transactional
    public ListingResponse markAsSold(UUID listingId, UUID sellerId) {
        log.info("Marking listing as sold: {} for seller: {}", listingId, sellerId);

        Listing listing = getListingForOwner(listingId, sellerId);

        if (listing.getStatus() != ListingStatus.ACTIVE) {
            throw new BadRequestException("Only active listings can be marked as sold");
        }

        listing.setStatus(ListingStatus.SOLD);
        Listing soldListing = listingRepository.save(listing);
        log.info("Marked listing as sold: {}", listingId);

        return listingMapper.toResponse(soldListing);
    }

    public PagedResponse<ListingSummaryResponse> getListings(
            ListingType listingType,
            VehicleType vehicleType,
            String state,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable) {

        log.debug("Fetching listings with filters - listingType: {}, vehicleType: {}, state: {}, priceRange: {}-{}",
                listingType, vehicleType, state, minPrice, maxPrice);

        Page<Listing> listingsPage = listingRepository.findWithFilters(
                ListingStatus.ACTIVE,
                listingType,
                vehicleType,
                state,
                minPrice,
                maxPrice,
                pageable
        );

        return toPagedResponse(listingsPage);
    }

    public ListingResponse getListingById(UUID listingId) {
        log.debug("Fetching listing by id: {}", listingId);

        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing", "id", listingId));

        if (listing.getStatus() == ListingStatus.DELETED) {
            throw new ResourceNotFoundException("Listing", "id", listingId);
        }

        return listingMapper.toResponse(listing);
    }

    public PagedResponse<ListingSummaryResponse> getSellerListings(UUID sellerId, Pageable pageable) {
        log.debug("Fetching listings for seller: {}", sellerId);

        Page<Listing> listingsPage = listingRepository.findBySellerId(sellerId, pageable);
        return toPagedResponse(listingsPage);
    }

    private Listing getListingForOwner(UUID listingId, UUID sellerId) {
        return listingRepository.findByIdAndSellerId(listingId, sellerId)
                .orElseThrow(() -> {
                    if (listingRepository.existsById(listingId)) {
                        return new UnauthorizedException("You are not authorized to modify this listing");
                    }
                    return new ResourceNotFoundException("Listing", "id", listingId);
                });
    }

    private void validateListingRequest(CreateListingRequest request) {
        if (request.getListingType() == ListingType.VEHICLE) {
            if (request.getVehicleType() == null) {
                throw new BadRequestException("Vehicle type is required for vehicle listings");
            }
        } else if (request.getListingType() == ListingType.PART) {
            if (request.getPartName() == null || request.getPartName().isBlank()) {
                throw new BadRequestException("Part name is required for part listings");
            }
        }
    }

    private void validateListingForPublish(Listing listing) {
        if (listing.getTitle() == null || listing.getTitle().isBlank()) {
            throw new BadRequestException("Title is required to publish a listing");
        }
        if (listing.getPrice() == null || listing.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Valid price is required to publish a listing");
        }
        if (listing.getState() == null || listing.getState().isBlank()) {
            throw new BadRequestException("State is required to publish a listing");
        }
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
