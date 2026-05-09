package com.ridelist.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketplaceStatsResponse {
    private Long totalListings;
    private Long activeListings;
    private Long totalSellers;
    private Long totalStates;
    private Long totalMakes;
    private Long newListingsToday;
}
