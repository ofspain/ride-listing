package com.ridelist.repository;

import com.ridelist.model.Make;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MakeRepository extends JpaRepository<Make, UUID> {

    Optional<Make> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsByName(String name);
}
