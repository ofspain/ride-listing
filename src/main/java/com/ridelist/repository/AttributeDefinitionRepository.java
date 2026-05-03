package com.ridelist.repository;

import com.ridelist.model.AttributeDefinition;
import com.ridelist.model.ListingType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttributeDefinitionRepository extends JpaRepository<AttributeDefinition, UUID> {

    Optional<AttributeDefinition> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @Query("SELECT a FROM AttributeDefinition a WHERE :listingType MEMBER OF a.listingTypes AND a.active = true")
    List<AttributeDefinition> findByListingTypeAndActiveTrue(@Param("listingType") ListingType listingType);

    @Query("SELECT a FROM AttributeDefinition a WHERE :listingType MEMBER OF a.listingTypes")
    List<AttributeDefinition> findByListingType(@Param("listingType") ListingType listingType);

    List<AttributeDefinition> findByActiveTrue();

    List<AttributeDefinition> findByFilterableTrueAndActiveTrue();

    @Query("SELECT a FROM AttributeDefinition a WHERE :listingType MEMBER OF a.listingTypes AND a.filterable = true AND a.active = true")
    List<AttributeDefinition> findByListingTypeAndFilterableTrueAndActiveTrue(@Param("listingType") ListingType listingType);
}
