package com.ridelist.dto.response;

import com.ridelist.model.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListingSummaryResponse {

    private UUID id;
    private ListingType listingType;
    private String title;
    private BigDecimal price;

    // Location hierarchy
    private StateResponse state;
    private AxisResponse axis;
    private AreaResponse area;

    private ListingCategory category;
    private ListingCondition condition;
    private ListingStatus status;
    private VehicleType vehicleType;
    private MakeResponse make;
    private VehicleModelResponse vehicleModel;
    private String location;
    private String primaryImageUrl;
    private LocalDateTime createdAt;
}
