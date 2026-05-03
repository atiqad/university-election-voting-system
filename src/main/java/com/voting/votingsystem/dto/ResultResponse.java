package com.voting.votingsystem.dto;

public class ResultResponse {

    private Long candidateId;
    private String candidateName;
    private Long totalVotes;
  
    
    private String candidateImage;

    public ResultResponse() {
    }

    public ResultResponse(String candidateName, Long totalVotes) {
        this.candidateName = candidateName;
        this.totalVotes = totalVotes;
    }

    public ResultResponse(Long candidateId, String candidateName, Long totalVotes, String candidateImage) {
        this.candidateId = candidateId;
        this.candidateName = candidateName;
        this.totalVotes = totalVotes;
        this.candidateImage = candidateImage;
    }

    public Long getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(Long candidateId) {
        this.candidateId = candidateId;
    }

    public String getCandidateName() {
        return candidateName;
    }

    public void setCandidateName(String candidateName) {
        this.candidateName = candidateName;
    }

    public Long getTotalVotes() {
        return totalVotes;
    }

    public void setTotalVotes(Long totalVotes) {
        this.totalVotes = totalVotes;
    }

    public String getCandidateImage() {
        return candidateImage;
    }

    public void setCandidateImage(String candidateImage) {
        this.candidateImage = candidateImage;
    }
}
