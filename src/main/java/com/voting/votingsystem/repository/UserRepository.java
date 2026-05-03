package com.voting.votingsystem.repository;

import com.voting.votingsystem.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByRegNo(String regNo);

    Optional<User> findByEmail(String email);

    long countByRole(String role);
}
