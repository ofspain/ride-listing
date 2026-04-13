package com.ridelist.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "attribute_definitions", indexes = {
        @Index(name = "idx_attribute_definition_slug", columnList = "slug", unique = true),
        @Index(name = "idx_attribute_definition_listing_type", columnList = "listing_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttributeDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(name = "listing_type", nullable = false)
    private ListingType listingType;

    @Column(nullable = false)
    @Builder.Default
    private Boolean filterable = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean required = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
