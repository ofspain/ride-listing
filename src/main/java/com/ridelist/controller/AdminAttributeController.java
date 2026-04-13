package com.ridelist.controller;

import com.ridelist.dto.request.AttributeCreateRequest;
import com.ridelist.dto.request.AttributeUpdateRequest;
import com.ridelist.dto.response.ApiResponse;
import com.ridelist.dto.response.AttributeDefinitionResponse;
import com.ridelist.model.ListingType;
import com.ridelist.service.AttributeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/attributes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAttributeController {

    private final AttributeService attributeService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AttributeDefinitionResponse> createAttribute(
            @Valid @RequestBody AttributeCreateRequest request) {
        AttributeDefinitionResponse response = attributeService.createAttribute(request);
        return ApiResponse.success("Attribute created successfully", response);
    }

    @PutMapping("/{id}")
    public ApiResponse<AttributeDefinitionResponse> updateAttribute(
            @PathVariable UUID id,
            @Valid @RequestBody AttributeUpdateRequest request) {
        AttributeDefinitionResponse response = attributeService.updateAttribute(id, request);
        return ApiResponse.success("Attribute updated successfully", response);
    }

    @GetMapping
    public ApiResponse<List<AttributeDefinitionResponse>> getAttributes(
            @RequestParam(required = false) ListingType listingType) {
        List<AttributeDefinitionResponse> attributes = attributeService.getAttributesByListingType(listingType);
        return ApiResponse.success(attributes);
    }

    @GetMapping("/{id}")
    public ApiResponse<AttributeDefinitionResponse> getAttribute(@PathVariable UUID id) {
        AttributeDefinitionResponse response = attributeService.getAttributeById(id);
        return ApiResponse.success(response);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAttribute(@PathVariable UUID id) {
        attributeService.deleteAttribute(id);
    }
}
