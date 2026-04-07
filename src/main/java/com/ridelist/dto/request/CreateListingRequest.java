package com.ridelist.dto.request;

import com.ridelist.model.ListingCategory;
import com.ridelist.model.ListingCondition;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateListingRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal price;

    @NotNull(message = "Category is required")
    private ListingCategory category;

    @NotNull(message = "Condition is required")
    private ListingCondition condition;

    private String brand;

    private String model;

    @Min(value = 1900, message = "Invalid year")
    @Max(value = 2100, message = "Invalid year")
    private Integer year;

    private String location;
}
