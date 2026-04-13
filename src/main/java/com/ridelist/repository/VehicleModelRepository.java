package com.ridelist.repository;

import com.ridelist.model.VehicleModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VehicleModelRepository extends JpaRepository<VehicleModel, UUID> {

    Optional<VehicleModel> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsByName(String name);

    List<VehicleModel> findByMakeId(UUID makeId);
}
