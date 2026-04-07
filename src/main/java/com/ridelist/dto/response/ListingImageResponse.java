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
public class ListingImageResponse {

    private UUID id;
    private String imageUrl;
    private Integer displayOrder;
    private boolean primary;
}
