package com.voting.votingsystem.repository;

import com.voting.votingsystem.entity.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CandidateRepository extends JpaRepository<Candidate, Long> {

    List<Candidate> findByElectionId(Long electionId);

    @Query("SELECT c FROM Candidate c WHERE c.active IS NULL OR c.active = true")
    List<Candidate> findVisibleCandidates();

    @Query("SELECT c FROM Candidate c WHERE c.election.id = :electionId AND (c.active IS NULL OR c.active = true)")
    List<Candidate> findVisibleCandidatesByElectionId(@Param("electionId") Long electionId);

    boolean existsByElectionId(Long electionId);
}
