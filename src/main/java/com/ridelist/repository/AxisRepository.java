package com.ridelist.repository;

import com.ridelist.model.Axis;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AxisRepository extends JpaRepository<Axis, UUID> {

    Optional<Axis> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<Axis> findByStateId(UUID stateId);

    Page<Axis> findByStateId(UUID stateId, Pageable pageable);

    boolean existsByStateIdAndName(UUID stateId, String name);
}
