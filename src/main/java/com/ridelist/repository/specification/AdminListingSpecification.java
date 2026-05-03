package com.ridelist.repository.specification;

import com.ridelist.model.*;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AdminListingSpecification {

    private AdminListingSpecification() {
    }

    public static Specification<Listing> withFilters(
            String search,
            ListingStatus status,
            ListingType listingType,
            ListingCategory category) {

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Search by title or seller name (case-insensitive LIKE)
            if (search != null && !search.isBlank()) {
                String searchPattern = "%" + search.toLowerCase() + "%";

                Join<Listing, User> sellerJoin = root.join("seller", JoinType.LEFT);

                Predicate titleMatch = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("title")), searchPattern);
                Predicate sellerFirstNameMatch = criteriaBuilder.like(
                        criteriaBuilder.lower(sellerJoin.get("firstName")), searchPattern);
                Predicate sellerLastNameMatch = criteriaBuilder.like(
                        criteriaBuilder.lower(sellerJoin.get("lastName")), searchPattern);

                predicates.add(criteriaBuilder.or(titleMatch, sellerFirstNameMatch, sellerLastNameMatch));
            }

            // Status filter (admins can see ALL statuses including DELETED)
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            // Listing type filter
            if (listingType != null) {
                predicates.add(criteriaBuilder.equal(root.get("listingType"), listingType));
            }

            // Category filter
            if (category != null) {
                predicates.add(criteriaBuilder.equal(root.get("category"), category));
            }

            // Ensure distinct results
            query.distinct(true);

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
