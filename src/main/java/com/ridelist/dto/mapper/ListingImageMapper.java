package com.ridelist.dto.mapper;

import com.ridelist.dto.response.ListingImageResponse;
import com.ridelist.model.ListingImage;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ListingImageMapper {

    ListingImageResponse toResponse(ListingImage image);

    List<ListingImageResponse> toResponseList(List<ListingImage> images);
}
