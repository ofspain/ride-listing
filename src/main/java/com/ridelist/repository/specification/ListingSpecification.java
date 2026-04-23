package com.ridelist.repository.specification;

import com.ridelist.model.*;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
            Map<String, String> attributeFilters) {

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

            // Listing type filter
            if (listingType != null) {
                predicates.add(criteriaBuilder.equal(root.get("listingType"), listingType));
            }

            // Vehicle type filter
            if (vehicleType != null) {
                predicates.add(criteriaBuilder.equal(root.get("vehicleType"), vehicleType));
            }

            // Location filters
            if (stateId != null) {
                predicates.add(criteriaBuilder.equal(root.get("state").get("id"), stateId));
            }

            if (axisId != null) {
                predicates.add(criteriaBuilder.equal(root.get("axis").get("id"), axisId));
            }

            if (areaId != null) {
                predicates.add(criteriaBuilder.equal(root.get("area").get("id"), areaId));
            }

            // Price filters
            if (minPrice != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("price"), minPrice));
            }

            if (maxPrice != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("price"), maxPrice));
            }

            // Dynamic attribute filters
            if (attributeFilters != null && !attributeFilters.isEmpty()) {
                for (Map.Entry<String, String> entry : attributeFilters.entrySet()) {
                    String attributeSlug = entry.getKey();
                    String attributeValue = entry.getValue();

                    // Create a subquery to check for matching attribute values
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
                                    criteriaBuilder.equal(
                                            criteriaBuilder.lower(attributeRoot.get("value")),
                                            attributeValue.toLowerCase()
                                    ),
                                    criteriaBuilder.isTrue(attributeDefJoin.get("active"))
                            )
                    );

                    predicates.add(criteriaBuilder.in(root.get("id")).value(subquery));
                }
            }

            // Ensure distinct results
            query.distinct(true);

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
