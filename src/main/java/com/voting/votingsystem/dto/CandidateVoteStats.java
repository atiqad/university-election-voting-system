package com.voting.votingsystem.dto;

public class CandidateVoteStats {
    private Long candidateId;
    private String candidateName;
    private Long totalVotes;
    /**
     * Web path stored for the candidate image, usually like "/images/abc.png".
     * May be null/blank if no image exists.
     */
    private String candidateImage;

    public CandidateVoteStats(Long candidateId, String candidateName, Long totalVotes) {
        this.candidateId = candidateId;
        this.candidateName = candidateName;
        this.totalVotes = totalVotes;
    }

    public CandidateVoteStats(Long candidateId, String candidateName, Long totalVotes, String candidateImage) {
        this.candidateId = candidateId;
        this.candidateName = candidateName;
        this.totalVotes = totalVotes;
        this.candidateImage = candidateImage;
    }

    public Long getCandidateId() {
        return candidateId;
    }

    public String getCandidateName() {
        return candidateName;
    }

    public Long getTotalVotes() {
        return totalVotes;
    }

    public String getCandidateImage() {
        return candidateImage;
    }

    public void setCandidateId(Long candidateId) {
        this.candidateId = candidateId;
    }

    public void setCandidateName(String candidateName) {
        this.candidateName = candidateName;
    }

    public void setTotalVotes(Long totalVotes) {
        this.totalVotes = totalVotes;
    }

    public void setCandidateImage(String candidateImage) {
        this.candidateImage = candidateImage;
    }
}
