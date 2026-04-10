package com.ridelist.repository;

import com.ridelist.model.AccountType;
import com.ridelist.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    java.util.List<User> findByState(String state);

    java.util.List<User> findByAccountType(AccountType accountType);
}
