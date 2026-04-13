package com.ridelist.repository;

import com.ridelist.model.ListingAttributeValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ListingAttributeValueRepository extends JpaRepository<ListingAttributeValue, UUID> {

    List<ListingAttributeValue> findByListingId(UUID listingId);

    @Modifying
    @Query("DELETE FROM ListingAttributeValue lav WHERE lav.listing.id = :listingId")
    void deleteByListingId(@Param("listingId") UUID listingId);

    @Query("SELECT lav FROM ListingAttributeValue lav " +
            "JOIN FETCH lav.attribute " +
            "WHERE lav.listing.id = :listingId")
    List<ListingAttributeValue> findByListingIdWithAttribute(@Param("listingId") UUID listingId);

    boolean existsByListingIdAndAttributeId(UUID listingId, UUID attributeId);
}
