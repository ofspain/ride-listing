package com.ridelist.dto.request;

import com.ridelist.model.ListingStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangeListingStatusRequest {

    @NotNull(message = "Status is required")
    private ListingStatus status;
}
