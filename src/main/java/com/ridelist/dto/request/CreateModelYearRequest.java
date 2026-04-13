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
public class CreateModelYearRequest {

    @NotBlank(message = "Year name is required")
    @Size(max = 10, message = "Year name must not exceed 10 characters")
    private String name;

    @NotNull(message = "Vehicle Model ID is required")
    private UUID vehicleModelId;
}
