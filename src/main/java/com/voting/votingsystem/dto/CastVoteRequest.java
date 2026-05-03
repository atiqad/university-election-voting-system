package com.voting.votingsystem.dto;

public class CastVoteRequest {
    private Long electionId;
    private Long candidateId;

    public Long getElectionId() {
        return electionId;
    }

    public Long getCandidateId() {
        return candidateId;
    }

    public void setElectionId(Long electionId) {
        this.electionId = electionId;
    }

    public void setCandidateId(Long candidateId) {
        this.candidateId = candidateId;
    }
}

