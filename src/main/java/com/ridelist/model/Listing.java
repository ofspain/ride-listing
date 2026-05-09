package com.ridelist.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "listings", indexes = {
        @Index(name = "idx_listing_state_id", columnList = "state_id"),
        @Index(name = "idx_listing_axis_id", columnList = "axis_id"),
        @Index(name = "idx_listing_area_id", columnList = "area_id"),
        @Index(name = "idx_listing_make_id", columnList = "make_id"),
        @Index(name = "idx_listing_vehicle_model_id", columnList = "vehicle_model_id"),
        @Index(name = "idx_listing_model_year_id", columnList = "model_year_id"),
        @Index(name = "idx_listings_listing_number", columnList = "listing_number"),
        @Index(name = "idx_listings_slug", columnList = "slug"),
        @Index(name = "idx_listings_number_slug", columnList = "listing_number, slug")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Listing extends BaseEntity {

    @Column(name = "listing_number", nullable = false, unique = true, updatable = false)
    private Integer listingNumber;

    @Column(name = "slug", nullable = false, length = 300)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(name = "listing_type", nullable = false)
    private ListingType listingType;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "state_id")
    private State state;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "axis_id")
    private Axis axis;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "area_id")
    private Area area;

    @Column(name = "address_line")
    private String addressLine;

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

    // Vehicle-specific fields
    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type")
    private VehicleType vehicleType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "make_id")
    private Make make;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_model_id")
    private VehicleModel vehicleModel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_year_id")
    private ModelYear modelYear;

    // Part-specific fields
    @Column(name = "part_name")
    private String partName;

    @Column(name = "part_category")
    private String partCategory;

    @Column(columnDefinition = "TEXT")
    private String compatibility;

    private String location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @OrderBy("displayOrder ASC")
    private List<ListingImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ListingAttributeValue> attributes = new ArrayList<>();

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
