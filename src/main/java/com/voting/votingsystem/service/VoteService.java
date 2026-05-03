package com.voting.votingsystem.service;

import com.voting.votingsystem.dto.CandidateVoteStats;
import com.voting.votingsystem.dto.ResultResponse;
import com.voting.votingsystem.entity.Candidate;
import com.voting.votingsystem.entity.Election;
import com.voting.votingsystem.entity.User;
import com.voting.votingsystem.entity.Vote;
import com.voting.votingsystem.repository.VoteRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class VoteService {

    private final VoteRepository voteRepository;
    private final ElectionService electionService;
    private final CandidateService candidateService;
    private final ActivityLogService activityLogService;

    public VoteService(VoteRepository voteRepository,
                       ElectionService electionService,
                       CandidateService candidateService,
                       ActivityLogService activityLogService) {
        this.voteRepository = voteRepository;
        this.electionService = electionService;
        this.candidateService = candidateService;
        this.activityLogService = activityLogService;
    }

    @Transactional
    public Vote castVote(Vote vote) {
        if (vote.getUser() == null || vote.getUser().getId() == null) {
            throw new RuntimeException("User is required to cast a vote.");
        }
        if (vote.getElection() == null || vote.getElection().getId() == null) {
            throw new RuntimeException("Election is required to cast a vote.");
        }
        if (vote.getCandidate() == null || vote.getCandidate().getId() == null) {
            throw new RuntimeException("Candidate is required to cast a vote.");
        }

        // Role-based verification: only students can vote.
        User voter = vote.getUser();
        String role = voter.getRole() == null ? "" : voter.getRole().trim();
        if (!"STUDENT".equalsIgnoreCase(role)) {
            throw new RuntimeException("Only STUDENT users can vote.");
        }

        // Time-controlled voting.
        Long electionId = vote.getElection().getId();
        electionService.assertElectionOpenForVoting(electionId);

        // Ensure candidate belongs to the same election.
        Candidate candidate = candidateService.getCandidateById(vote.getCandidate().getId())
                .orElseThrow(() -> new RuntimeException("Candidate not found with id: " + vote.getCandidate().getId()));
        Election election = electionService.getElectionById(electionId)
                .orElseThrow(() -> new RuntimeException("Election not found with id: " + electionId));

        if (candidate.getElection() == null || candidate.getElection().getId() == null ||
                !candidate.getElection().getId().equals(electionId)) {
            throw new RuntimeException("Candidate does not belong to the selected election.");
        }

        if (!candidate.isActive()) {
            throw new RuntimeException("Candidate is not active for voting.");
        }

        vote.setCandidate(candidate);
        vote.setElection(election);

        Long userId = vote.getUser().getId();

        boolean alreadyVoted = voteRepository.findByUserIdAndElectionId(userId, electionId).isPresent();

        if (alreadyVoted) {
            throw new RuntimeException("User has already voted in this election.");
        }

        try {
            Vote saved = voteRepository.save(vote);
            activityLogService.log(voter, "CAST_VOTE",
                    "Vote cast. voteId=" + saved.getId() +
                            ", userId=" + voter.getId() +
                            ", electionId=" + electionId +
                            ", candidateId=" + saved.getCandidate().getId());
            return saved;
        } catch (DataIntegrityViolationException ex) {
            // If two requests race, the DB unique constraint is the final safety net.
            throw new RuntimeException("User has already voted in this election.", ex);
        }
    }

    public boolean hasUserVoted(Long userId, Long electionId) {
        return voteRepository.findByUserIdAndElectionId(userId, electionId).isPresent();
    }

    public long countByUser(Long userId) {
        if (userId == null) {
            return 0L;
        }
        return voteRepository.countByUserId(userId);
    }

    public long countAll() {
        return voteRepository.count();
    }

    public List<Vote> getAllVotes() {
        return voteRepository.findAll();
    }

    public List<Vote> getVotesByElectionId(Long electionId) {
        return voteRepository.findByElectionId(electionId);
    }

    public List<Vote> getVotesByCandidateId(Long candidateId) {
        return voteRepository.findByCandidateId(candidateId);
    }

    public List<ResultResponse> getElectionResults(Long electionId) {
        List<Object[]> rows = voteRepository.countVotesByElection(electionId);
        List<Candidate> candidates = candidateService.getCandidatesByElectionId(electionId);
        Map<Long, String> imageByCandidateId = new HashMap<>();
        for (Candidate c : candidates) {
            imageByCandidateId.put(c.getId(), c.getProfileImage());
        }

        List<ResultResponse> results = new ArrayList<>();

        for (Object[] row : rows) {
            Long candidateId = (Long) row[0];
            String candidateName = (String) row[1];
            Long totalVotes = (Long) row[2];
            results.add(new ResultResponse(candidateId, candidateName, totalVotes, imageByCandidateId.get(candidateId)));
        }

        return results;
    }

    public ResultResponse getWinner(Long electionId) {
        List<ResultResponse> results = getElectionResults(electionId);

        if (results.isEmpty()) {
            return null;
        }

        ResultResponse winner = results.get(0);

        for (ResultResponse result : results) {
            if (result.getTotalVotes() > winner.getTotalVotes()) {
                winner = result;
            }
        }

        return winner;
    }

    /**
     * Demo-friendly stats: includes candidates with 0 votes (so the dashboard always looks complete).
     */
    public List<CandidateVoteStats> getElectionVoteStats(Long electionId) {
        List<Candidate> candidates = candidateService.getCandidatesByElectionId(electionId);
        Map<Long, CandidateVoteStats> statsByCandidateId = new HashMap<>();
        for (Candidate c : candidates) {
            statsByCandidateId.put(c.getId(), new CandidateVoteStats(c.getId(), c.getName(), 0L, c.getProfileImage()));
        }

        List<Object[]> rows = voteRepository.countVotesByElection(electionId);
        for (Object[] row : rows) {
            Long candidateId = (Long) row[0];
            String candidateName = (String) row[1];
            Long totalVotes = (Long) row[2];

            CandidateVoteStats existing = statsByCandidateId.get(candidateId);
            if (existing != null) {
                existing.setTotalVotes(totalVotes);
            } else {
                // Fallback: if the candidate list didn't include this id for some reason, still show stats.
                statsByCandidateId.put(candidateId, new CandidateVoteStats(candidateId, candidateName, totalVotes, null));
            }
        }

        return new ArrayList<>(statsByCandidateId.values());
    }

    public ResultResponse getWinnerFromStats(Long electionId) {
        List<CandidateVoteStats> stats = getElectionVoteStats(electionId);
        if (stats.isEmpty()) {
            return null;
        }

        CandidateVoteStats top = null;
        long max = -1;
        for (CandidateVoteStats s : stats) {
            if (s.getTotalVotes() != null && s.getTotalVotes() > max) {
                max = s.getTotalVotes();
                top = s;
            }
        }

        if (top == null || max <= 0) {
            return null;
        }

        // Detect ties for clear demo output.
        List<String> tiedNames = new ArrayList<>();
        List<Long> tiedIds = new ArrayList<>();
        for (CandidateVoteStats s : stats) {
            if (s.getTotalVotes() != null && s.getTotalVotes() == max) {
                tiedNames.add(s.getCandidateName());
                tiedIds.add(s.getCandidateId());
            }
        }

        if (tiedNames.size() > 1) {
            String winnerName = "Tie: " + String.join(", ", tiedNames);
            return new ResultResponse(null, winnerName, max, null);
        }

        Candidate winner = candidateService.getCandidateById(top.getCandidateId()).orElse(null);
        String image = winner == null ? null : winner.getProfileImage();
        return new ResultResponse(top.getCandidateId(), top.getCandidateName(), max, image);
    }
}
