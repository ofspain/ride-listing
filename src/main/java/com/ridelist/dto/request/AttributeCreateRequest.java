package com.ridelist.dto.request;

import com.ridelist.model.ListingType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttributeCreateRequest {

    @NotBlank(message = "Attribute name is required")
    @Size(max = 100, message = "Attribute name must not exceed 100 characters")
    private String name;

    @NotEmpty(message = "At least one listing type is required")
    private Set<ListingType> listingTypes;

    @URL(message = "Must be a valid URL")
    private String iconUrl;

    @NotEmpty(message = "At least one acceptable value must be provided")
    private List<String> acceptableValues;

    @Builder.Default
    private Boolean filterable = true;

    @Builder.Default
    private Boolean required = false;
}
