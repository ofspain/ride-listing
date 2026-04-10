package com.ridelist.repository;

import com.ridelist.model.ContactRequest;
import com.ridelist.model.ContactStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ContactRequestRepository extends JpaRepository<ContactRequest, UUID> {

    Page<ContactRequest> findByListingId(UUID listingId, Pageable pageable);

    Page<ContactRequest> findByBuyerId(UUID buyerId, Pageable pageable);

    @Query("SELECT cr FROM ContactRequest cr WHERE cr.listing.seller.id = :sellerId")
    Page<ContactRequest> findBySellerIdThroughListing(@Param("sellerId") UUID sellerId, Pageable pageable);

    List<ContactRequest> findByListingIdAndStatus(UUID listingId, ContactStatus status);

    long countByListingIdAndStatus(UUID listingId, ContactStatus status);

    boolean existsByListingIdAndBuyerId(UUID listingId, UUID buyerId);
}
