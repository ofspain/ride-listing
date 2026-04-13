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
public class CreateVehicleModelRequest {

    @NotBlank(message = "Model name is required")
    @Size(max = 100, message = "Model name must not exceed 100 characters")
    private String name;

    @NotNull(message = "Make ID is required")
    private UUID makeId;
}
