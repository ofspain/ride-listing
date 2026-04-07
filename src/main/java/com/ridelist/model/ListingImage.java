package com.ridelist.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "listing_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ListingImage extends BaseEntity {

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @Column(name = "s3_key", nullable = false)
    private String s3Key;

    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "is_primary")
    @Builder.Default
    private boolean primary = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;
}
