package com.ridelist.repository;

import com.ridelist.model.AccountType;
import com.ridelist.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    java.util.List<User> findByState(String state);

    java.util.List<User> findByAccountType(AccountType accountType);

    /**
     * Find user by email including deleted users (bypasses @Where filter).
     * Used for authentication to provide appropriate error messages.
     */
    @Query(value = "SELECT * FROM users WHERE email = :email", nativeQuery = true)
    Optional<User> findByEmailIncludingDeleted(@Param("email") String email);

    /**
     * Find user by ID including deleted users (bypasses @Where filter).
     * Used for JWT validation to properly reject deleted users.
     */
    @Query(value = "SELECT * FROM users WHERE id = :id", nativeQuery = true)
    Optional<User> findByIdIncludingDeleted(@Param("id") UUID id);
}
