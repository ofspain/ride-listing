package com.ridelist.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "listing_attribute_values",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_listing_attribute", columnNames = {"listing_id", "attribute_id"})
        },
        indexes = {
                @Index(name = "idx_listing_attribute_value", columnList = "attribute_id, value"),
                @Index(name = "idx_listing_attribute_listing", columnList = "listing_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ListingAttributeValue {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attribute_id", nullable = false)
    private AttributeDefinition attribute;

    @Column(nullable = false)
    private String value;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
