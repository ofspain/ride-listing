package com.ridelist.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttributeUpdateRequest {

    @Size(max = 100, message = "Attribute name must not exceed 100 characters")
    private String name;

    private Boolean filterable;

    private Boolean required;

    private Boolean active;
}
