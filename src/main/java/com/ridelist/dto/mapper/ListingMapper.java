package com.ridelist.dto.mapper;

import com.ridelist.dto.request.CreateListingRequest;
import com.ridelist.dto.request.UpdateListingRequest;
import com.ridelist.dto.response.ListingResponse;
import com.ridelist.dto.response.ListingSummaryResponse;
import com.ridelist.model.Listing;
import com.ridelist.model.ListingImage;
import com.ridelist.util.SlugUtil;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {UserMapper.class, ListingImageMapper.class, LocationMapper.class, CategorizationMapper.class, AttributeMapper.class})
public interface ListingMapper {

    @Mapping(target = "status", ignore = true)
    @Mapping(target = "seller", ignore = true)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "viewCount", ignore = true)
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "axis", ignore = true)
    @Mapping(target = "area", ignore = true)
    @Mapping(target = "make", ignore = true)
    @Mapping(target = "vehicleModel", ignore = true)
    @Mapping(target = "modelYear", ignore = true)
    @Mapping(target = "attributes", ignore = true)
    Listing toEntity(CreateListingRequest request);

    ListingResponse toResponse(Listing listing);

    @Mapping(target = "primaryImageUrl", expression = "java(getPrimaryImageUrl(listing))")
    ListingSummaryResponse toSummaryResponse(Listing listing);

    @Mapping(target = "listingType", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "seller", ignore = true)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "viewCount", ignore = true)
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "axis", ignore = true)
    @Mapping(target = "area", ignore = true)
    @Mapping(target = "make", ignore = true)
    @Mapping(target = "vehicleModel", ignore = true)
    @Mapping(target = "modelYear", ignore = true)
    @Mapping(target = "attributes", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest(UpdateListingRequest request, @MappingTarget Listing listing);

    default String getPrimaryImageUrl(Listing listing) {
        if (listing.getImages() == null || listing.getImages().isEmpty()) {
            return null;
        }
        return listing.getImages().stream()
                .filter(ListingImage::isPrimary)
                .findFirst()
                .map(ListingImage::getImageUrl)
                .orElse(listing.getImages().get(0).getImageUrl());
    }

    @AfterMapping
    default void populateSeoFields(Listing listing, @MappingTarget ListingSummaryResponse response) {
        String categoryPath = SlugUtil.toCategoryPath(listing.getListingType(), listing.getVehicleType());
        String stateSlug = listing.getState() != null ? listing.getState().getSlug() : null;
        String axisSlug = listing.getAxis() != null ? listing.getAxis().getSlug() : null;
        String areaSlug = listing.getArea() != null ? listing.getArea().getSlug() : null;

        String canonicalUrl = SlugUtil.toListingUrl(
                listing.getListingType(),
                listing.getVehicleType(),
                stateSlug,
                axisSlug,
                areaSlug,
                listing.getListingNumber(),
                listing.getTitle()
        );

        response.setListingNumber(listing.getListingNumber());
        response.setSlug(listing.getSlug());
        response.setCanonicalUrl(canonicalUrl);
        response.setCategoryPath(categoryPath);
    }

    @AfterMapping
    default void populateSeoFields(Listing listing, @MappingTarget ListingResponse response) {
        String categoryPath = SlugUtil.toCategoryPath(listing.getListingType(), listing.getVehicleType());
        String stateSlug = listing.getState() != null ? listing.getState().getSlug() : null;
        String axisSlug = listing.getAxis() != null ? listing.getAxis().getSlug() : null;
        String areaSlug = listing.getArea() != null ? listing.getArea().getSlug() : null;

        String canonicalUrl = SlugUtil.toListingUrl(
                listing.getListingType(),
                listing.getVehicleType(),
                stateSlug,
                axisSlug,
                areaSlug,
                listing.getListingNumber(),
                listing.getTitle()
        );

        response.setListingNumber(listing.getListingNumber());
        response.setSlug(listing.getSlug());
        response.setCanonicalUrl(canonicalUrl);
        response.setCategoryPath(categoryPath);

        // Browse path segments for breadcrumb navigation
        if (stateSlug != null) {
            response.setStatePath("/" + categoryPath + "/" + stateSlug);

            if (axisSlug != null) {
                response.setAxisPath("/" + categoryPath + "/" + stateSlug + "/" + axisSlug);

                if (areaSlug != null) {
                    response.setAreaPath("/" + categoryPath + "/" + stateSlug + "/" + axisSlug + "/" + areaSlug);
                }
            }
        }
    }
}
