package com.voting.votingsystem.repository;

import com.voting.votingsystem.entity.Election;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ElectionRepository extends JpaRepository<Election, Long> {

    Optional<Election> findByActiveTrue();

    List<Election> findByActive(boolean active);

    long countByActiveTrue();
}
