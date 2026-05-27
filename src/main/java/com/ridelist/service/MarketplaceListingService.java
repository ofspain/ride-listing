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
import com.ridelist.repository.*;
import com.ridelist.repository.specification.ListingSpecification;
import com.ridelist.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MarketplaceListingService {

    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final StateRepository stateRepository;
    private final AxisRepository axisRepository;
    private final AreaRepository areaRepository;
    private final MakeRepository makeRepository;
    private final VehicleModelRepository vehicleModelRepository;
    private final ModelYearRepository modelYearRepository;
    private final ListingMapper listingMapper;
    private final ListingAttributeService listingAttributeService;
    private final LocationHubService locationHubService;

    @Transactional
    public ListingResponse createListing(CreateListingRequest request, UUID sellerId) {
        log.info("Creating listing for seller: {}", sellerId);

        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", sellerId));

        validateListingRequest(request);

        Listing listing = listingMapper.toEntity(request);
        listing.setSeller(seller);
        listing.setStatus(ListingStatus.DRAFT);

        // Resolve and set location entities
        setLocationEntities(listing, request.getStateId(), request.getAxisId(), request.getAreaId());

        // Resolve and set categorization entities for vehicle listings
        if (request.getListingType() == ListingType.VEHICLE) {
            setCategorizationEntities(listing, request.getMakeId(), request.getVehicleModelId(), request.getModelYearId());
        }

        // Generate slug before first save
        String slug = SlugUtil.toListingSlug(listing.getTitle());
        listing.setSlug(slug);

        // Generate unique listing number before saving

        Long nextListingNumber = listingRepository.getNextListingNumber();
        listing.setListingNumber(nextListingNumber.intValue());

        Listing savedListing = listingRepository.save(listing);
        UUID savedId = savedListing.getId();

        // Re-fetch to get the database-generated listing_number
        savedListing = listingRepository.findById(savedId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing", "id", savedId));

        log.info("Created listing #{} with id: {}", savedListing.getListingNumber(), savedListing.getId());

        // Save dynamic attributes
        if (request.getAttributes() != null && !request.getAttributes().isEmpty()) {
            listingAttributeService.saveAttributes(savedListing, request.getAttributes());
        }

        return listingMapper.toResponse(savedListing);
    }

    @Transactional
    public ListingResponse updateListing(UUID listingId, UpdateListingRequest request, UUID sellerId) {
        log.info("Updating listing: {} for seller: {}", listingId, sellerId);

        Listing listing = getListingForOwner(listingId, sellerId);

        if (listing.getStatus() == ListingStatus.SOLD || listing.getStatus() == ListingStatus.DELETED) {
            throw new BadRequestException("Cannot update a listing that is sold or deleted");
        }

        // Regenerate slug if title has changed
        if (request.getTitle() != null && !request.getTitle().equals(listing.getTitle())) {
            String oldSlug = listing.getSlug();
            String newSlug = SlugUtil.toListingSlug(request.getTitle());
            listing.setSlug(newSlug);
            log.info("Listing #{} slug updated: {} → {}", listing.getListingNumber(), oldSlug, newSlug);
        }

        listingMapper.updateEntityFromRequest(request, listing);

        // Update location entities if provided
        if (request.getStateId() != null || request.getAxisId() != null || request.getAreaId() != null) {
            UUID stateId = request.getStateId() != null ? request.getStateId() :
                    (listing.getState() != null ? listing.getState().getId() : null);
            UUID axisId = request.getAxisId() != null ? request.getAxisId() :
                    (listing.getAxis() != null ? listing.getAxis().getId() : null);
            UUID areaId = request.getAreaId() != null ? request.getAreaId() :
                    (listing.getArea() != null ? listing.getArea().getId() : null);
            setLocationEntities(listing, stateId, axisId, areaId);
        }

        // Update categorization entities if provided (for vehicle listings)
        if (listing.getListingType() == ListingType.VEHICLE &&
                (request.getMakeId() != null || request.getVehicleModelId() != null || request.getModelYearId() != null)) {
            UUID makeId = request.getMakeId() != null ? request.getMakeId() :
                    (listing.getMake() != null ? listing.getMake().getId() : null);
            UUID vehicleModelId = request.getVehicleModelId() != null ? request.getVehicleModelId() :
                    (listing.getVehicleModel() != null ? listing.getVehicleModel().getId() : null);
            UUID modelYearId = request.getModelYearId() != null ? request.getModelYearId() :
                    (listing.getModelYear() != null ? listing.getModelYear().getId() : null);
            setCategorizationEntities(listing, makeId, vehicleModelId, modelYearId);
        }

        // Update dynamic attributes if provided
        if (request.getAttributes() != null) {
            listingAttributeService.saveAttributes(listing, request.getAttributes());
        }

        Listing updatedListing = listingRepository.save(listing);
        log.info("Updated listing #{} ({})", updatedListing.getListingNumber(), listingId);

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
        log.info("Published listing #{} ({})", publishedListing.getListingNumber(), listingId);

        locationHubService.evictLocationHubCache();

        return listingMapper.toResponse(publishedListing);
    }

    @Transactional
    public ListingResponse markAsSold(UUID listingId, UUID sellerId) {
        log.info("Marking listing #{} as sold for seller: {}", listingId, sellerId);

        Listing listing = getListingForOwner(listingId, sellerId);

        if (listing.getStatus() != ListingStatus.ACTIVE) {
            throw new BadRequestException("Only active listings can be marked as sold");
        }

        listing.setStatus(ListingStatus.SOLD);
        Listing soldListing = listingRepository.save(listing);
        log.info("Marked listing #{} ({}) as sold", soldListing.getListingNumber(), listingId);

        locationHubService.evictLocationHubCache();

        return listingMapper.toResponse(soldListing);
    }

    public PagedResponse<ListingSummaryResponse> getListings(
            ListingType listingType,
            VehicleType vehicleType,
            UUID stateId,
            UUID axisId,
            UUID areaId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            List<String> locationSlugs,
            Map<String, List<String>> attributeFilters,
            String searchQuery,
            Pageable pageable) {

        log.debug("Fetching listings with filters - listingType: {}, vehicleType: {}, stateId: {}, axisId: {}, areaId: {}, priceRange: {}-{}, locationSlugs: {}, attributeFilters: {}, q: {}",
                listingType, vehicleType, stateId, axisId, areaId, minPrice, maxPrice, locationSlugs, attributeFilters, searchQuery);

        Specification<Listing> spec = ListingSpecification.withFilters(
                ListingStatus.ACTIVE,
                listingType,
                vehicleType,
                stateId,
                axisId,
                areaId,
                minPrice,
                maxPrice,
                locationSlugs,
                attributeFilters,
                searchQuery
        );

        Page<Listing> listingsPage = listingRepository.findAll(spec, pageable);

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

    public ListingResponse getListingByNumber(Integer listingNumber) {
        log.debug("Fetching listing by number: {}", listingNumber);

        Listing listing = listingRepository.findByListingNumber(listingNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Listing", "number", listingNumber));

        if (listing.getStatus() == ListingStatus.DELETED) {
            throw new ResourceNotFoundException("Listing", "number", listingNumber);
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
        if (listing.getState() == null) {
            throw new BadRequestException("State is required to publish a listing");
        }
    }

    private void setLocationEntities(Listing listing, UUID stateId, UUID axisId, UUID areaId) {
        State state = null;
        Axis axis = null;
        Area area = null;

        // Resolve state
        if (stateId != null) {
            state = stateRepository.findById(stateId)
                    .orElseThrow(() -> new ResourceNotFoundException("State", "id", stateId));
            listing.setState(state);
        }

        // Resolve axis and validate it belongs to the state
        if (axisId != null) {
            axis = axisRepository.findById(axisId)
                    .orElseThrow(() -> new ResourceNotFoundException("Axis", "id", axisId));

            if (state != null && !axis.getState().getId().equals(state.getId())) {
                throw new BadRequestException("Axis does not belong to the selected state");
            }
            listing.setAxis(axis);
        }

        // Resolve area and validate it belongs to the axis
        if (areaId != null) {
            area = areaRepository.findById(areaId)
                    .orElseThrow(() -> new ResourceNotFoundException("Area", "id", areaId));

            if (axis != null && !area.getAxis().getId().equals(axis.getId())) {
                throw new BadRequestException("Area does not belong to the selected axis");
            }
            listing.setArea(area);
        }
    }

    private void setCategorizationEntities(Listing listing, UUID makeId, UUID vehicleModelId, UUID modelYearId) {
        Make make = null;
        VehicleModel vehicleModel = null;

        // Resolve make
        if (makeId != null) {
            make = makeRepository.findById(makeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Make", "id", makeId));
            listing.setMake(make);
        }

        // Resolve vehicle model and validate it belongs to the make
        if (vehicleModelId != null) {
            vehicleModel = vehicleModelRepository.findById(vehicleModelId)
                    .orElseThrow(() -> new ResourceNotFoundException("VehicleModel", "id", vehicleModelId));

            if (make != null && !vehicleModel.getMake().getId().equals(make.getId())) {
                throw new BadRequestException("Vehicle model does not belong to the selected make");
            }
            listing.setVehicleModel(vehicleModel);
        }

        // Resolve model year and validate it belongs to the vehicle model
        if (modelYearId != null) {
            ModelYear modelYear = modelYearRepository.findById(modelYearId)
                    .orElseThrow(() -> new ResourceNotFoundException("ModelYear", "id", modelYearId));

            if (vehicleModel != null && !modelYear.getVehicleModel().getId().equals(vehicleModel.getId())) {
                throw new BadRequestException("Model year does not belong to the selected vehicle model");
            }
            listing.setModelYear(modelYear);
        }
    }

    private PagedResponse<ListingSummaryResponse> toPagedResponse(Page<Listing> listingsPage) {
        PagedResponse<ListingSummaryResponse> response = PagedResponse.<ListingSummaryResponse>builder()
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

       // log.info("Listing response: {}", response);

        return response;
    }
}
