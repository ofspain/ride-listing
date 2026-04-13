package com.ridelist.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttributeValueResponse {

    private UUID id;
    private UUID attributeId;
    private String attributeName;
    private String attributeSlug;
    private String value;
}
