package com.ridelist.repository;

import com.ridelist.model.Axis;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AxisRepository extends JpaRepository<Axis, UUID> {

    Optional<Axis> findBySlug(String slug);

    @Query("SELECT ax FROM Axis ax WHERE ax.slug = :slug AND ax.state.slug = :stateSlug")
    Optional<Axis> findBySlugAndStateSlug(@Param("slug") String slug, @Param("stateSlug") String stateSlug);

    boolean existsBySlug(String slug);

    List<Axis> findByStateId(UUID stateId);

    Page<Axis> findByStateId(UUID stateId, Pageable pageable);

    boolean existsByStateIdAndName(UUID stateId, String name);
}
