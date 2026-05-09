package com.ridelist.repository.specification;

import com.ridelist.model.*;
import jakarta.persistence.criteria.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
public class ListingSpecification {

    private ListingSpecification() {
    }

    public static Specification<Listing> withFilters(
            ListingStatus status,
            ListingType listingType,
            VehicleType vehicleType,
            UUID stateId,
            UUID axisId,
            UUID areaId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            List<String> locationSlugs,
            Map<String, List<String>> attributeFilters,
            String searchQuery) {

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Exclude listings from deleted/disabled sellers
            Join<Listing, ?> sellerJoin = root.join("seller", JoinType.INNER);
            predicates.add(criteriaBuilder.isTrue(sellerJoin.get("enabled")));
            predicates.add(criteriaBuilder.isNull(sellerJoin.get("deletedAt")));

            // Status filter
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            // Listing type filter (null means "all" - no type restriction)
            if (listingType != null) {
                predicates.add(criteriaBuilder.equal(root.get("listingType"), listingType));
            }

            // Vehicle type filter (null means all vehicle types or "all" category)
            if (vehicleType != null) {
                predicates.add(criteriaBuilder.equal(root.get("vehicleType"), vehicleType));
            }

            // Text search on title (case-insensitive, partial match)
            if (searchQuery != null && searchQuery.length() >= 2) {
                String searchPattern = "%" + searchQuery.toLowerCase() + "%";
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("title")),
                        searchPattern
                ));
                log.debug("Applied text search filter: q={}", searchQuery);
            }

            // UUID-based location filters (for backward compatibility)
            if (stateId != null) {
                predicates.add(criteriaBuilder.equal(root.get("state").get("id"), stateId));
            }

            if (axisId != null) {
                predicates.add(criteriaBuilder.equal(root.get("axis").get("id"), axisId));
            }

            if (areaId != null) {
                predicates.add(criteriaBuilder.equal(root.get("area").get("id"), areaId));
            }

            // Multi-value location slug filter (OR logic)
            // Matches against state.slug, axis.slug, or area.slug
            if (locationSlugs != null && !locationSlugs.isEmpty()) {
                List<String> lowerSlugs = locationSlugs.stream()
                        .map(String::toLowerCase)
                        .toList();

                Join<Listing, State> stateJoin = root.join("state", JoinType.LEFT);
                Join<Listing, Axis> axisJoin = root.join("axis", JoinType.LEFT);
                Join<Listing, Area> areaJoin = root.join("area", JoinType.LEFT);

                Predicate stateMatch = criteriaBuilder.lower(stateJoin.get("slug")).in(lowerSlugs);
                Predicate axisMatch = criteriaBuilder.lower(axisJoin.get("slug")).in(lowerSlugs);
                Predicate areaMatch = criteriaBuilder.lower(areaJoin.get("slug")).in(lowerSlugs);

                predicates.add(criteriaBuilder.or(stateMatch, axisMatch, areaMatch));

                log.debug("Applied location slug filter with {} slugs: {}", locationSlugs.size(), locationSlugs);
            }

            // Price filters
            if (minPrice != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("price"), minPrice));
            }

            if (maxPrice != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("price"), maxPrice));
            }

            // Dynamic attribute filters with multi-value support
            // Multiple values for same attribute: OR
            // Different attributes: AND
            if (attributeFilters != null && !attributeFilters.isEmpty()) {
                for (Map.Entry<String, List<String>> entry : attributeFilters.entrySet()) {
                    String attributeSlug = entry.getKey();
                    List<String> attributeValues = entry.getValue();

                    if (attributeValues == null || attributeValues.isEmpty()) {
                        continue;
                    }

                    List<String> lowerValues = attributeValues.stream()
                            .map(String::toLowerCase)
                            .toList();

                    // EXISTS subquery: checks if listing has any of the specified values for this attribute
                    Subquery<UUID> subquery = query.subquery(UUID.class);
                    Root<ListingAttributeValue> attributeRoot = subquery.from(ListingAttributeValue.class);
                    Join<ListingAttributeValue, AttributeDefinition> attributeDefJoin =
                            attributeRoot.join("attribute");

                    subquery.select(attributeRoot.get("listing").get("id"));
                    subquery.where(
                            criteriaBuilder.and(
                                    criteriaBuilder.equal(
                                            criteriaBuilder.lower(attributeDefJoin.get("slug")),
                                            attributeSlug.toLowerCase()
                                    ),
                                    criteriaBuilder.lower(attributeRoot.get("value")).in(lowerValues),
                                    criteriaBuilder.isTrue(attributeDefJoin.get("active"))
                            )
                    );

                    predicates.add(criteriaBuilder.in(root.get("id")).value(subquery));

                    log.debug("Applied attribute filter: {}={}", attributeSlug, attributeValues);
                }
            }

            // Ensure distinct results
            query.distinct(true);

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
