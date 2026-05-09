package com.ridelist.repository;

import com.ridelist.model.Area;
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
public interface AreaRepository extends JpaRepository<Area, UUID> {

    Optional<Area> findBySlug(String slug);

    List<Area> findBySlugIn(List<String> slugs);

    @Query("SELECT ar FROM Area ar WHERE ar.slug = :slug AND ar.axis.slug = :axisSlug")
    Optional<Area> findBySlugAndAxisSlug(@Param("slug") String slug, @Param("axisSlug") String axisSlug);

    boolean existsBySlug(String slug);

    List<Area> findByAxisId(UUID axisId);

    Page<Area> findByAxisId(UUID axisId, Pageable pageable);

    boolean existsByAxisIdAndName(UUID axisId, String name);
}
