package com.ridelist.service;

import com.ridelist.dto.mapper.AttributeMapper;
import com.ridelist.dto.request.AttributeValueRequest;
import com.ridelist.dto.response.AttributeValueResponse;
import com.ridelist.exception.BadRequestException;
import com.ridelist.exception.ResourceNotFoundException;
import com.ridelist.model.AttributeDefinition;
import com.ridelist.model.Listing;
import com.ridelist.model.ListingAttributeValue;
import com.ridelist.repository.AttributeDefinitionRepository;
import com.ridelist.repository.ListingAttributeValueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ListingAttributeService {

    private final ListingAttributeValueRepository listingAttributeValueRepository;
    private final AttributeDefinitionRepository attributeDefinitionRepository;
    private final AttributeMapper attributeMapper;

    @Transactional
    public List<AttributeValueResponse> saveAttributes(Listing listing, List<AttributeValueRequest> attributeRequests) {
        log.info("Saving {} attributes for listing: {}",
                attributeRequests != null ? attributeRequests.size() : 0, listing.getId());

        if (attributeRequests == null || attributeRequests.isEmpty()) {
            // Clear existing attributes if none provided
            listing.getAttributes().clear();
            return Collections.emptyList();
        }

        // Validate no duplicate attribute IDs in request
        Set<UUID> attributeIds = new HashSet<>();
        for (AttributeValueRequest request : attributeRequests) {
            if (!attributeIds.add(request.getAttributeId())) {
                throw new BadRequestException("Duplicate attribute ID in request: " + request.getAttributeId());
            }
        }

        // Fetch all attribute definitions
        List<AttributeDefinition> attributes = attributeDefinitionRepository.findAllById(attributeIds);
        Map<UUID, AttributeDefinition> attributeMap = attributes.stream()
                .collect(Collectors.toMap(AttributeDefinition::getId, a -> a));

        // Validate all attributes exist and are active
        for (UUID attributeId : attributeIds) {
            AttributeDefinition attribute = attributeMap.get(attributeId);
            if (attribute == null) {
                throw new ResourceNotFoundException("AttributeDefinition", "id", attributeId);
            }
            if (!attribute.getActive()) {
                throw new BadRequestException("Attribute '" + attribute.getName() + "' is not active");
            }
            if (!attribute.getListingTypes().contains(listing.getListingType())) {
                throw new BadRequestException("Attribute '" + attribute.getName() +
                        "' does not apply to " + listing.getListingType() + " listings");
            }
        }

        // Validate required attributes
        List<AttributeDefinition> requiredAttributes = attributeDefinitionRepository
                .findByListingTypeAndActiveTrue(listing.getListingType())
                .stream()
                .filter(AttributeDefinition::getRequired)
                .toList();

        for (AttributeDefinition required : requiredAttributes) {
            if (!attributeIds.contains(required.getId())) {
                throw new BadRequestException("Required attribute '" + required.getName() + "' is missing");
            }
        }

        // Clear existing attributes and add new ones
        listing.getAttributes().clear();

        List<ListingAttributeValue> newValues = new ArrayList<>();
        for (AttributeValueRequest request : attributeRequests) {
            AttributeDefinition attribute = attributeMap.get(request.getAttributeId());
            String trimmedValue = request.getValue().trim();

            // Validate value is in acceptable values list
            if (!attribute.getAcceptableValues().isEmpty() &&
                    !attribute.getAcceptableValues().contains(trimmedValue)) {
                throw new BadRequestException(
                        "Invalid value '" + trimmedValue + "' for attribute '" + attribute.getName() +
                                "'. Acceptable values: " + attribute.getAcceptableValues());
            }

            ListingAttributeValue value = ListingAttributeValue.builder()
                    .listing(listing)
                    .attribute(attribute)
                    .value(trimmedValue)
                    .build();

            listing.getAttributes().add(value);
            newValues.add(value);
        }

        log.info("Saved {} attributes for listing: {}", newValues.size(), listing.getId());
        return attributeMapper.toAttributeValueResponseList(newValues);
    }

    public List<AttributeValueResponse> getAttributesForListing(UUID listingId) {
        log.debug("Fetching attributes for listing: {}", listingId);

        List<ListingAttributeValue> values = listingAttributeValueRepository
                .findByListingIdWithAttribute(listingId);

        return attributeMapper.toAttributeValueResponseList(values);
    }

    @Transactional
    public void deleteAttributesForListing(UUID listingId) {
        log.info("Deleting all attributes for listing: {}", listingId);
        listingAttributeValueRepository.deleteByListingId(listingId);
    }
}
