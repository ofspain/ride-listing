package com.ridelist.dto.request;

import com.ridelist.model.ListingCategory;
import com.ridelist.model.ListingCondition;
import com.ridelist.model.VehicleType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateListingRequest {

    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal price;

    private UUID stateId;

    private UUID axisId;

    private UUID areaId;

    @Size(max = 500, message = "Address line must not exceed 500 characters")
    private String addressLine;

    private ListingCategory category;

    private ListingCondition condition;

    // Vehicle-specific fields
    private VehicleType vehicleType;
    private UUID makeId;
    private UUID vehicleModelId;
    private UUID modelYearId;

    // Part-specific fields
    private String partName;
    private String partCategory;
    private String compatibility;

    private String location;

    // Dynamic attributes
    private List<AttributeValueRequest> attributes;
}
