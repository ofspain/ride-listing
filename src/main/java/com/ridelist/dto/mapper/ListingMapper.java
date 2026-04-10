package com.ridelist.dto.mapper;

import com.ridelist.dto.request.CreateListingRequest;
import com.ridelist.dto.request.UpdateListingRequest;
import com.ridelist.dto.response.ListingResponse;
import com.ridelist.dto.response.ListingSummaryResponse;
import com.ridelist.model.Listing;
import com.ridelist.model.ListingImage;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {UserMapper.class, ListingImageMapper.class})
public interface ListingMapper {

//    @Mapping(target = "id", ignore = true)
//    @Mapping(target = "createdAt", ignore = true)
//    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "seller", ignore = true)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "viewCount", ignore = true)
    Listing toEntity(CreateListingRequest request);

    ListingResponse toResponse(Listing listing);

    @Mapping(target = "primaryImageUrl", expression = "java(getPrimaryImageUrl(listing))")
    ListingSummaryResponse toSummaryResponse(Listing listing);

//    @Mapping(target = "id", ignore = true)
//    @Mapping(target = "createdAt", ignore = true)
//    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "listingType", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "seller", ignore = true)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "viewCount", ignore = true)
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
}
