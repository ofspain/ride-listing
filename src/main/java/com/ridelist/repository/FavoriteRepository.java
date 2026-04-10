package com.ridelist.repository;

import com.ridelist.model.Favorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, UUID> {

    Page<Favorite> findByUserId(UUID userId, Pageable pageable);

    List<Favorite> findByUserId(UUID userId);

    boolean existsByUserIdAndListingId(UUID userId, UUID listingId);

    Optional<Favorite> findByUserIdAndListingId(UUID userId, UUID listingId);

    @Modifying
    @Query("DELETE FROM Favorite f WHERE f.user.id = :userId AND f.listing.id = :listingId")
    void deleteByUserIdAndListingId(@Param("userId") UUID userId, @Param("listingId") UUID listingId);

    long countByUserId(UUID userId);

    long countByListingId(UUID listingId);

    @Query("SELECT f.listing.id FROM Favorite f WHERE f.user.id = :userId")
    List<UUID> findListingIdsByUserId(@Param("userId") UUID userId);
}
