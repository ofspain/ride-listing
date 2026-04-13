package com.ridelist.service;

import com.ridelist.dto.mapper.AttributeMapper;
import com.ridelist.dto.request.AttributeCreateRequest;
import com.ridelist.dto.request.AttributeUpdateRequest;
import com.ridelist.dto.response.AttributeDefinitionResponse;
import com.ridelist.exception.DuplicateResourceException;
import com.ridelist.exception.ResourceNotFoundException;
import com.ridelist.model.AttributeDefinition;
import com.ridelist.model.ListingType;
import com.ridelist.repository.AttributeDefinitionRepository;
import com.ridelist.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AttributeService {

    private final AttributeDefinitionRepository attributeDefinitionRepository;
    private final AttributeMapper attributeMapper;

    @Transactional
    public AttributeDefinitionResponse createAttribute(AttributeCreateRequest request) {
        log.info("Creating attribute: {}", request.getName());

        String slug = SlugUtil.toSlug(request.getName());

        if (attributeDefinitionRepository.existsBySlug(slug)) {
            throw new DuplicateResourceException("Attribute with slug '" + slug + "' already exists");
        }

        AttributeDefinition attribute = AttributeDefinition.builder()
                .name(request.getName())
                .slug(slug)
                .listingType(request.getListingType())
                .filterable(request.getFilterable() != null ? request.getFilterable() : true)
                .required(request.getRequired() != null ? request.getRequired() : false)
                .active(true)
                .build();

        AttributeDefinition savedAttribute = attributeDefinitionRepository.save(attribute);
        log.info("Created attribute with id: {}", savedAttribute.getId());

        return attributeMapper.toAttributeDefinitionResponse(savedAttribute);
    }

    @Transactional
    public AttributeDefinitionResponse updateAttribute(UUID attributeId, AttributeUpdateRequest request) {
        log.info("Updating attribute: {}", attributeId);

        AttributeDefinition attribute = attributeDefinitionRepository.findById(attributeId)
                .orElseThrow(() -> new ResourceNotFoundException("AttributeDefinition", "id", attributeId));

        if (request.getName() != null && !request.getName().isBlank()) {
            String newSlug = SlugUtil.toSlug(request.getName());
            if (!newSlug.equals(attribute.getSlug()) && attributeDefinitionRepository.existsBySlug(newSlug)) {
                throw new DuplicateResourceException("Attribute with slug '" + newSlug + "' already exists");
            }
            attribute.setName(request.getName());
            attribute.setSlug(newSlug);
        }

        if (request.getFilterable() != null) {
            attribute.setFilterable(request.getFilterable());
        }

        if (request.getRequired() != null) {
            attribute.setRequired(request.getRequired());
        }

        if (request.getActive() != null) {
            attribute.setActive(request.getActive());
        }

        AttributeDefinition updatedAttribute = attributeDefinitionRepository.save(attribute);
        log.info("Updated attribute: {}", attributeId);

        return attributeMapper.toAttributeDefinitionResponse(updatedAttribute);
    }

    public List<AttributeDefinitionResponse> getAttributesByListingType(ListingType listingType) {
        log.debug("Fetching attributes for listing type: {}", listingType);

        List<AttributeDefinition> attributes = listingType != null
                ? attributeDefinitionRepository.findByListingType(listingType)
                : attributeDefinitionRepository.findAll();

        return attributeMapper.toAttributeDefinitionResponseList(attributes);
    }

    public List<AttributeDefinitionResponse> getActiveAttributesByListingType(ListingType listingType) {
        log.debug("Fetching active attributes for listing type: {}", listingType);

        List<AttributeDefinition> attributes = attributeDefinitionRepository
                .findByListingTypeAndActiveTrue(listingType);

        return attributeMapper.toAttributeDefinitionResponseList(attributes);
    }

    public List<AttributeDefinitionResponse> getFilterableAttributes(ListingType listingType) {
        log.debug("Fetching filterable attributes for listing type: {}", listingType);

        List<AttributeDefinition> attributes = listingType != null
                ? attributeDefinitionRepository.findByListingTypeAndFilterableTrueAndActiveTrue(listingType)
                : attributeDefinitionRepository.findByFilterableTrueAndActiveTrue();

        return attributeMapper.toAttributeDefinitionResponseList(attributes);
    }

    public AttributeDefinitionResponse getAttributeById(UUID attributeId) {
        AttributeDefinition attribute = attributeDefinitionRepository.findById(attributeId)
                .orElseThrow(() -> new ResourceNotFoundException("AttributeDefinition", "id", attributeId));

        return attributeMapper.toAttributeDefinitionResponse(attribute);
    }

    @Transactional
    public void deleteAttribute(UUID attributeId) {
        log.info("Deleting attribute: {}", attributeId);

        if (!attributeDefinitionRepository.existsById(attributeId)) {
            throw new ResourceNotFoundException("AttributeDefinition", "id", attributeId);
        }

        attributeDefinitionRepository.deleteById(attributeId);
        log.info("Deleted attribute: {}", attributeId);
    }
}
