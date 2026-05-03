package com.ridelist.dto.request;

import com.ridelist.model.ListingType;
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
public class AttributeUpdateRequest {

    @Size(max = 100, message = "Attribute name must not exceed 100 characters")
    private String name;

    private Set<ListingType> listingTypes;

    @URL(message = "Must be a valid URL")
    private String iconUrl;

    private List<String> acceptableValues;

    private Boolean filterable;

    private Boolean required;

    private Boolean active;
}
