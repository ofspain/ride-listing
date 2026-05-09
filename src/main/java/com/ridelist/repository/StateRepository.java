package com.ridelist.repository;

import com.ridelist.model.State;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StateRepository extends JpaRepository<State, UUID> {

    Optional<State> findBySlug(String slug);

    List<State> findBySlugIn(List<String> slugs);

    boolean existsBySlug(String slug);

    boolean existsByName(String name);
}
