package com.voting.votingsystem.repository;

import com.voting.votingsystem.entity.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface VoteRepository extends JpaRepository<Vote, Long> {

    Optional<Vote> findByUserIdAndElectionId(Long userId, Long electionId);

    long countByUserId(Long userId);

    List<Vote> findByElectionId(Long electionId);

    boolean existsByElectionId(Long electionId);

    boolean existsByCandidateId(Long candidateId);

    List<Vote> findByCandidateId(Long candidateId);

    @Query("SELECT v.candidate.id, v.candidate.name, COUNT(v) " +
            "FROM Vote v " +
            "WHERE v.election.id = :electionId " +
            "GROUP BY v.candidate.id, v.candidate.name")
    List<Object[]> countVotesByElection(Long electionId);
}
