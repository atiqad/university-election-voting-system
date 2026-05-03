package com.voting.votingsystem.entity;

import jakarta.persistence.*;

@Entity
@Table(
        name = "votes",
        uniqueConstraints = {
                // Reliable one-student-one-vote rule at DB level (race-condition safe).
                @UniqueConstraint(name = "uk_votes_user_election", columnNames = {"user_id", "election_id"})
        },
        indexes = {
                @Index(name = "ix_votes_user", columnList = "user_id"),
                @Index(name = "ix_votes_election", columnList = "election_id"),
                @Index(name = "ix_votes_candidate", columnList = "candidate_id")
        }
)
public class Vote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    @ManyToOne
    @JoinColumn(name = "election_id", nullable = false)
    private Election election;

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Candidate getCandidate() {
        return candidate;
    }

    public Election getElection() {
        return election;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setCandidate(Candidate candidate) {
        this.candidate = candidate;
    }

    public void setElection(Election election) {
        this.election = election;
    }
}
