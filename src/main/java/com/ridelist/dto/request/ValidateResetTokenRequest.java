package com.ridelist.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ValidateResetTokenRequest {

    @NotBlank
    private String token;
}
