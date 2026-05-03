package com.ridelist.repository.specification;

import com.ridelist.model.AccountType;
import com.ridelist.model.User;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UserSpecification {

    public static Specification<User> searchUsers(
            String search,
            AccountType accountType,
            Boolean enabled,
            LocalDateTime deletedFrom,
            LocalDateTime deletedTo
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Search across multiple fields
            if (search != null && !search.trim().isEmpty()) {
                String pattern = "%" + search.toLowerCase() + "%";

                predicates.add(
                        cb.or(
                                cb.like(cb.lower(root.get("phoneNumber")), pattern),
                                cb.like(cb.lower(root.get("firstName")), pattern),
                                cb.like(cb.lower(root.get("lastName")), pattern),
                                cb.like(cb.lower(root.get("email")), pattern)
                        )
                );
            }

            // Account type filter
            if (accountType != null) {
                predicates.add(cb.equal(root.get("accountType"), accountType));
            }

            // Enabled filter
            if (enabled != null) {
                predicates.add(cb.equal(root.get("enabled"), enabled));
            }

            // Deleted date range filter
            if (deletedFrom != null || deletedTo != null) {
                predicates.add(cb.isNotNull(root.get("deletedAt")));

                if (deletedFrom != null) {
                    predicates.add(
                            cb.greaterThanOrEqualTo(root.get("deletedAt"), deletedFrom)
                    );
                }

                if (deletedTo != null) {
                    predicates.add(
                            cb.lessThanOrEqualTo(root.get("deletedAt"), deletedTo)
                    );
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
