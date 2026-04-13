package com.ridelist.repository;

import com.ridelist.model.AttributeDefinition;
import com.ridelist.model.ListingType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttributeDefinitionRepository extends JpaRepository<AttributeDefinition, UUID> {

    Optional<AttributeDefinition> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<AttributeDefinition> findByListingTypeAndActiveTrue(ListingType listingType);

    List<AttributeDefinition> findByListingType(ListingType listingType);

    List<AttributeDefinition> findByActiveTrue();

    List<AttributeDefinition> findByFilterableTrueAndActiveTrue();

    List<AttributeDefinition> findByListingTypeAndFilterableTrueAndActiveTrue(ListingType listingType);
}
