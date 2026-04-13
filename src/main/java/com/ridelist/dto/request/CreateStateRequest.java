package com.ridelist.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateStateRequest {

    @NotBlank(message = "State name is required")
    @Size(max = 100, message = "State name must not exceed 100 characters")
    private String name;
}
