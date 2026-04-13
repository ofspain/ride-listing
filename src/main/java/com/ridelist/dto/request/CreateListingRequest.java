package com.ridelist.dto.request;

import com.ridelist.model.ListingCategory;
import com.ridelist.model.ListingCondition;
import com.ridelist.model.ListingType;
import com.ridelist.model.VehicleType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class CreateListingRequest {

    @NotNull(message = "Listing type is required")
    private ListingType listingType;

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal price;

    @NotNull(message = "State is required")
    private UUID stateId;

    private UUID axisId;

    private UUID areaId;

    @Size(max = 500, message = "Address line must not exceed 500 characters")
    private String addressLine;

    @NotNull(message = "Category is required")
    private ListingCategory category;

    @NotNull(message = "Condition is required")
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
