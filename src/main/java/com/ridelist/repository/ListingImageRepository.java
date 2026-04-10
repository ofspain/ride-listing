package com.ridelist.repository;

import com.ridelist.model.ListingImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ListingImageRepository extends JpaRepository<ListingImage, UUID> {

    List<ListingImage> findByListingIdOrderByDisplayOrderAsc(UUID listingId);

    Optional<ListingImage> findByListingIdAndPrimaryTrue(UUID listingId);

    int countByListingId(UUID listingId);

    @Modifying
    @Query("DELETE FROM ListingImage li WHERE li.listing.id = :listingId")
    void deleteByListingId(@Param("listingId") UUID listingId);

    Optional<ListingImage> findByS3Key(String s3Key);
}
