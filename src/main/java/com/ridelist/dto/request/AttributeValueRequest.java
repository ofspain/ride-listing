package com.ridelist.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttributeValueRequest {

    @NotNull(message = "Attribute ID is required")
    private UUID attributeId;

    @NotBlank(message = "Attribute value is required")
    @Size(max = 500, message = "Attribute value must not exceed 500 characters")
    private String value;
}
