package com.ridelist.repository;

import com.ridelist.model.ModelYear;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ModelYearRepository extends JpaRepository<ModelYear, UUID> {

    Optional<ModelYear> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsByName(String name);

    List<ModelYear> findByVehicleModelId(UUID vehicleModelId);
}
