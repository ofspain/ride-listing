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
public class CreateMakeRequest {

    @NotBlank(message = "Make name is required")
    @Size(max = 100, message = "Make name must not exceed 100 characters")
    private String name;
}
