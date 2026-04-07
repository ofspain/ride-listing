package com.ridelist.dto.response;

import com.ridelist.model.ListingCategory;
import com.ridelist.model.ListingCondition;
import com.ridelist.model.ListingStatus;
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
    private String title;
    private BigDecimal price;
    private ListingCategory category;
    private ListingCondition condition;
    private ListingStatus status;
    private String location;
    private String primaryImageUrl;
    private LocalDateTime createdAt;
}
