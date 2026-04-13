package com.ridelist.repository;

import com.ridelist.model.State;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StateRepository extends JpaRepository<State, UUID> {

    Optional<State> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsByName(String name);
}
