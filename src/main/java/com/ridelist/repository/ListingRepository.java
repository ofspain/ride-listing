package com.ridelist.repository;

import com.ridelist.model.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Modifying;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;

@Repository
public interface ListingRepository extends JpaRepository<Listing, UUID>, JpaSpecificationExecutor<Listing> {

    Page<Listing> findByListingType(ListingType listingType, Pageable pageable);

    Page<Listing> findByStateId(UUID stateId, Pageable pageable);

    Page<Listing> findByAxisId(UUID axisId, Pageable pageable);

    Page<Listing> findByAreaId(UUID areaId, Pageable pageable);

    Page<Listing> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    Page<Listing> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    Page<Listing> findByDescriptionContainingIgnoreCase(String description, Pageable pageable);

    Page<Listing> findByStatus(ListingStatus status, Pageable pageable);

    Page<Listing> findBySellerId(UUID sellerId, Pageable pageable);

    Page<Listing> findByVehicleType(VehicleType vehicleType, Pageable pageable);

    Page<Listing> findByCategory(ListingCategory category, Pageable pageable);

    @Query("SELECT l FROM Listing l WHERE l.status = :status AND " +
           "(LOWER(l.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(l.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Listing> searchByKeyword(@Param("keyword") String keyword,
                                   @Param("status") ListingStatus status,
                                   Pageable pageable);

    @Query("SELECT l FROM Listing l WHERE l.status = :status " +
           "AND (:listingType IS NULL OR l.listingType = :listingType) " +
           "AND (:vehicleType IS NULL OR l.vehicleType = :vehicleType) " +
           "AND (:stateId IS NULL OR l.state.id = :stateId) " +
           "AND (:axisId IS NULL OR l.axis.id = :axisId) " +
           "AND (:areaId IS NULL OR l.area.id = :areaId) " +
           "AND (:minPrice IS NULL OR l.price >= :minPrice) " +
           "AND (:maxPrice IS NULL OR l.price <= :maxPrice)")
    Page<Listing> findWithFilters(@Param("status") ListingStatus status,
                                   @Param("listingType") ListingType listingType,
                                   @Param("vehicleType") VehicleType vehicleType,
                                   @Param("stateId") UUID stateId,
                                   @Param("axisId") UUID axisId,
                                   @Param("areaId") UUID areaId,
                                   @Param("minPrice") BigDecimal minPrice,
                                   @Param("maxPrice") BigDecimal maxPrice,
                                   Pageable pageable);

    Optional<Listing> findByIdAndSellerId(UUID id, UUID sellerId);

    Optional<Listing> findByListingNumber(Integer listingNumber);

    long countBySellerId(UUID sellerId);

    long countBySellerIdAndStatusIn(UUID sellerId, List<ListingStatus> statuses);

    List<Listing> findTop10ByStatusOrderByCreatedAtDesc(ListingStatus status);

    @Modifying
    @Query("UPDATE Listing l SET l.status = :newStatus WHERE l.seller.id = :sellerId AND l.status NOT IN (:excludeStatuses)")
    int updateStatusBySellerIdAndStatusNotIn(
            @Param("sellerId") UUID sellerId,
            @Param("newStatus") ListingStatus newStatus,
            @Param("excludeStatuses") List<ListingStatus> excludeStatuses);

    long countByStatus(ListingStatus status);

    @Query("SELECT COUNT(l) FROM Listing l WHERE l.createdAt >= :since")
    long countByCreatedAtAfter(@Param("since") LocalDateTime since);

    @Query("SELECT l.status, COUNT(l) FROM Listing l GROUP BY l.status")
    List<Object[]> countGroupByStatus();

    @Query("SELECT l FROM Listing l JOIN FETCH l.seller ORDER BY l.createdAt DESC")
    List<Listing> findTop10RecentWithSeller(Pageable pageable);

    @Query(value = """
        SELECT
            COUNT(l.id) as total_listings,
            SUM(CASE WHEN l.status IN ('ACTIVE', 'PUBLISHED') THEN 1 ELSE 0 END) as active_listings,
            COUNT(DISTINCT l.seller_id) as total_sellers,
            COUNT(DISTINCT CASE WHEN l.status IN ('ACTIVE', 'PUBLISHED') THEN l.state_id END) as total_states,
            COUNT(DISTINCT CASE WHEN l.status IN ('ACTIVE', 'PUBLISHED') THEN l.make_id END) as total_makes,
            SUM(CASE WHEN l.created_at >= :yesterday THEN 1 ELSE 0 END) as new_listings_today
        FROM listings l
        WHERE l.status != 'DELETED'
        """, nativeQuery = true)
    Object[] computeMarketplaceStats(@Param("yesterday") LocalDateTime yesterday);

    @Query(value = """
        SELECT
            COUNT(l.id) as total,
            SUM(CASE WHEN l.status = 'PUBLISHED' THEN 1 ELSE 0 END) as published,
            SUM(CASE WHEN l.status = 'ACTIVE' THEN 1 ELSE 0 END) as active,
            SUM(CASE WHEN l.status = 'DRAFT' THEN 1 ELSE 0 END) as draft,
            SUM(CASE WHEN l.status = 'SOLD' THEN 1 ELSE 0 END) as sold,
            SUM(CASE WHEN l.status = 'EXPIRED' THEN 1 ELSE 0 END) as expired,
            SUM(CASE WHEN l.created_at >= :weekStart THEN 1 ELSE 0 END) as new_this_week,
            SUM(CASE WHEN l.status = 'SOLD' AND l.updated_at >= :weekStart THEN 1 ELSE 0 END) as sold_this_week,
            SUM(CASE WHEN l.created_at >= :monthStart THEN 1 ELSE 0 END) as new_this_month,
            SUM(CASE WHEN l.status = 'SOLD' AND l.updated_at >= :monthStart THEN 1 ELSE 0 END) as sold_this_month
        FROM listings l
        WHERE l.seller_id = :sellerId
          AND l.status != 'DELETED'
        """, nativeQuery = true)
    Object[] computeDealerListingStats(
            @Param("sellerId") UUID sellerId,
            @Param("weekStart") LocalDateTime weekStart,
            @Param("monthStart") LocalDateTime monthStart);

    @Query("SELECT l.status, COUNT(l) FROM Listing l WHERE l.seller.id = :sellerId AND l.status <> 'DELETED' GROUP BY l.status")
    List<Object[]> countBySellerGroupByStatus(@Param("sellerId") UUID sellerId);

    @Query("SELECT l FROM Listing l LEFT JOIN FETCH l.state LEFT JOIN FETCH l.axis LEFT JOIN FETCH l.area " +
           "WHERE l.status IN ('ACTIVE', 'PUBLISHED') ORDER BY l.createdAt DESC")
    List<Listing> findRecentActiveForSitemap(Pageable pageable);

    @Query("SELECT new com.ridelist.dto.response.LocationCount(" +
           "l.state.name, l.state.slug, COUNT(l)) " +
           "FROM Listing l " +
           "WHERE l.listingType = :listingType " +
           "AND (:vehicleType IS NULL OR l.vehicleType = :vehicleType) " +
           "AND l.status IN (com.ridelist.model.ListingStatus.ACTIVE, com.ridelist.model.ListingStatus.PUBLISHED) " +
           "AND l.state IS NOT NULL " +
           "GROUP BY l.state.id, l.state.name, l.state.slug " +
           "ORDER BY COUNT(l) DESC")
    List<com.ridelist.dto.response.LocationCount> countActiveListingsByCategory(
            @Param("listingType") ListingType listingType,
            @Param("vehicleType") VehicleType vehicleType
    );

    @Query("SELECT new com.ridelist.dto.response.LocationCount(" +
           "l.axis.name, l.axis.slug, COUNT(l)) " +
           "FROM Listing l " +
           "WHERE l.state.id = :stateId " +
           "AND l.listingType = :listingType " +
           "AND (:vehicleType IS NULL OR l.vehicleType = :vehicleType) " +
           "AND l.status IN (com.ridelist.model.ListingStatus.ACTIVE, com.ridelist.model.ListingStatus.PUBLISHED) " +
           "AND l.axis IS NOT NULL " +
           "GROUP BY l.axis.id, l.axis.name, l.axis.slug " +
           "ORDER BY COUNT(l) DESC")
    List<com.ridelist.dto.response.LocationCount> countActiveListingsByStateAndCategory(
            @Param("stateId") UUID stateId,
            @Param("listingType") ListingType listingType,
            @Param("vehicleType") VehicleType vehicleType
    );

    @Query("SELECT new com.ridelist.dto.response.LocationCount(" +
           "l.area.name, l.area.slug, COUNT(l)) " +
           "FROM Listing l " +
           "WHERE l.axis.id = :axisId " +
           "AND l.listingType = :listingType " +
           "AND (:vehicleType IS NULL OR l.vehicleType = :vehicleType) " +
           "AND l.status IN (com.ridelist.model.ListingStatus.ACTIVE, com.ridelist.model.ListingStatus.PUBLISHED) " +
           "AND l.area IS NOT NULL " +
           "GROUP BY l.area.id, l.area.name, l.area.slug " +
           "ORDER BY COUNT(l) DESC")
    List<com.ridelist.dto.response.LocationCount> countActiveListingsByAxisAndCategory(
            @Param("axisId") UUID axisId,
            @Param("listingType") ListingType listingType,
            @Param("vehicleType") VehicleType vehicleType
    );

    @Query(value = "SELECT nextval('listing_number_seq')", nativeQuery = true)
    Long getNextListingNumber();
}
