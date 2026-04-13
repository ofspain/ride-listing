package com.ridelist.controller;

import com.ridelist.dto.response.ApiResponse;
import com.ridelist.dto.response.AttributeDefinitionResponse;
import com.ridelist.model.ListingType;
import com.ridelist.service.AttributeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/attributes")
@RequiredArgsConstructor
public class PublicAttributeController {

    private final AttributeService attributeService;

    @GetMapping
    public ApiResponse<List<AttributeDefinitionResponse>> getActiveAttributes(
            @RequestParam ListingType listingType) {
        List<AttributeDefinitionResponse> attributes = attributeService.getActiveAttributesByListingType(listingType);
        return ApiResponse.success(attributes);
    }

    @GetMapping("/filterable")
    public ApiResponse<List<AttributeDefinitionResponse>> getFilterableAttributes(
            @RequestParam(required = false) ListingType listingType) {
        List<AttributeDefinitionResponse> attributes = attributeService.getFilterableAttributes(listingType);
        return ApiResponse.success(attributes);
    }
}
