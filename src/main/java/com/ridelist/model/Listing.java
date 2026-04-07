package com.ridelist.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "listings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Listing extends BaseEntity {

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ListingCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ListingCondition condition = ListingCondition.GOOD;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ListingStatus status = ListingStatus.DRAFT;

    private String brand;

    private String model;

    private Integer year;

    private String location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @OrderBy("displayOrder ASC")
    private List<ListingImage> images = new ArrayList<>();

    @Column(name = "view_count")
    @Builder.Default
    private Long viewCount = 0L;

    public void addImage(ListingImage image) {
        images.add(image);
        image.setListing(this);
    }

    public void removeImage(ListingImage image) {
        images.remove(image);
        image.setListing(null);
    }
}
