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
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListingResponse {

    private UUID id;
    private String title;
    private String description;
    private BigDecimal price;
    private ListingCategory category;
    private ListingCondition condition;
    private ListingStatus status;
    private String brand;
    private String model;
    private Integer year;
    private String location;
    private Long viewCount;
    private UserSummaryResponse seller;
    private List<ListingImageResponse> images;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
