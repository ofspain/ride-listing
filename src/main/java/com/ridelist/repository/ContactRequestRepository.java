package com.ridelist.repository;

import com.ridelist.model.ContactRequest;
import com.ridelist.model.ContactStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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

    long countByStatus(ContactStatus status);

    @Query(value = """
        SELECT
            COUNT(cr.id) as total,
            SUM(CASE WHEN cr.status = 'PENDING' THEN 1 ELSE 0 END) as unread,
            SUM(CASE WHEN cr.created_at >= :weekStart THEN 1 ELSE 0 END) as this_week,
            SUM(CASE WHEN cr.created_at >= :lastWeekStart AND cr.created_at < :weekStart THEN 1 ELSE 0 END) as last_week,
            SUM(CASE WHEN cr.created_at >= :monthStart THEN 1 ELSE 0 END) as this_month,
            SUM(CASE WHEN cr.created_at >= :lastMonthStart AND cr.created_at < :monthStart THEN 1 ELSE 0 END) as last_month
        FROM contact_requests cr
        JOIN listings l ON cr.listing_id = l.id
        WHERE l.seller_id = :sellerId
        """, nativeQuery = true)
    Object[] computeDealerInquiryStats(
            @Param("sellerId") UUID sellerId,
            @Param("weekStart") LocalDateTime weekStart,
            @Param("lastWeekStart") LocalDateTime lastWeekStart,
            @Param("monthStart") LocalDateTime monthStart,
            @Param("lastMonthStart") LocalDateTime lastMonthStart);

    @Query(value = """
        SELECT l.id, l.title, COUNT(cr.id) as inquiry_count
        FROM listings l
        JOIN contact_requests cr ON cr.listing_id = l.id
        WHERE l.seller_id = :sellerId
        GROUP BY l.id, l.title
        ORDER BY inquiry_count DESC
        LIMIT 5
        """, nativeQuery = true)
    List<Object[]> findTop5ListingsByInquiryCount(@Param("sellerId") UUID sellerId);
}
