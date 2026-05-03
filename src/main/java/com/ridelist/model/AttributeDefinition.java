package com.ridelist.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "attribute_definitions", indexes = {
        @Index(name = "idx_attribute_definition_slug", columnList = "slug", unique = true)
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

    @ElementCollection
    @CollectionTable(
            name = "attribute_listing_types",
            joinColumns = @JoinColumn(name = "attribute_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "listing_type")
    @Builder.Default
    private Set<ListingType> listingTypes = new HashSet<>();

    @Column(name = "icon_url", length = 500)
    private String iconUrl;

    @ElementCollection
    @CollectionTable(
            name = "attribute_acceptable_values",
            joinColumns = @JoinColumn(name = "attribute_id")
    )
    @Column(name = "value", length = 100)
    @OrderColumn(name = "display_order")
    @Builder.Default
    private List<String> acceptableValues = new ArrayList<>();

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
