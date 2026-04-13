package com.ridelist.dto.request;

import com.ridelist.model.ListingType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttributeCreateRequest {

    @NotBlank(message = "Attribute name is required")
    @Size(max = 100, message = "Attribute name must not exceed 100 characters")
    private String name;

    @NotNull(message = "Listing type is required")
    private ListingType listingType;

    @Builder.Default
    private Boolean filterable = true;

    @Builder.Default
    private Boolean required = false;
}
