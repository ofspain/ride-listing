package com.ridelist.dto.response;

import com.ridelist.model.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListingResponse {

    private UUID id;
    private ListingType listingType;
    private String title;
    private String description;
    private BigDecimal price;
    private String state;
    private ListingCategory category;
    private ListingCondition condition;
    private ListingStatus status;

    // Vehicle-specific fields
    private VehicleType vehicleType;
    private String make;
    private String model;
    private Integer year;

    // Part-specific fields
    private String partName;
    private String partCategory;
    private String compatibility;

    private String location;
    private Long viewCount;
    private UserSummaryResponse seller;
    private List<ListingImageResponse> images;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
