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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

    long countBySellerId(UUID sellerId);

    List<Listing> findTop10ByStatusOrderByCreatedAtDesc(ListingStatus status);

    @Modifying
    @Query("UPDATE Listing l SET l.status = :newStatus WHERE l.seller.id = :sellerId AND l.status NOT IN (:excludeStatuses)")
    int updateStatusBySellerIdAndStatusNotIn(
            @Param("sellerId") UUID sellerId,
            @Param("newStatus") ListingStatus newStatus,
            @Param("excludeStatuses") List<ListingStatus> excludeStatuses);
}
