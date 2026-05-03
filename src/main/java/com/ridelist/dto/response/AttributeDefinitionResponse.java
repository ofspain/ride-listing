package com.ridelist.dto.response;

import com.ridelist.model.ListingType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttributeDefinitionResponse {

    private UUID id;
    private String name;
    private String slug;
    private Set<ListingType> listingTypes;
    private String iconUrl;
    private List<String> acceptableValues;
    private Boolean filterable;
    private Boolean required;
    private Boolean active;
}
