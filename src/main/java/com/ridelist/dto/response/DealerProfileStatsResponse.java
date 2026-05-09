package com.ridelist.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DealerProfileStatsResponse {
    private Long totalListingViews;
    private Long viewsThisWeek;
    private Long viewsLastWeek;
    private Long totalFavoritesReceived;
}
