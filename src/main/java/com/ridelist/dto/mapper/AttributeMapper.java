package com.ridelist.dto.mapper;

import com.ridelist.dto.response.AttributeDefinitionResponse;
import com.ridelist.dto.response.AttributeValueResponse;
import com.ridelist.model.AttributeDefinition;
import com.ridelist.model.ListingAttributeValue;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AttributeMapper {

    AttributeDefinitionResponse toAttributeDefinitionResponse(AttributeDefinition attributeDefinition);

    List<AttributeDefinitionResponse> toAttributeDefinitionResponseList(List<AttributeDefinition> attributeDefinitions);

    @Mapping(target = "attributeId", source = "attribute.id")
    @Mapping(target = "attributeName", source = "attribute.name")
    @Mapping(target = "attributeSlug", source = "attribute.slug")
    AttributeValueResponse toAttributeValueResponse(ListingAttributeValue listingAttributeValue);

    List<AttributeValueResponse> toAttributeValueResponseList(List<ListingAttributeValue> listingAttributeValues);
}
