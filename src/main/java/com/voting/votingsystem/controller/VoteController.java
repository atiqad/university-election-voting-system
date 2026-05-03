package com.voting.votingsystem.controller;

import com.voting.votingsystem.dto.CastVoteRequest;
import com.voting.votingsystem.dto.ResultResponse;
import com.voting.votingsystem.entity.Candidate;
import com.voting.votingsystem.entity.Election;
import com.voting.votingsystem.entity.User;
import com.voting.votingsystem.entity.Vote;
import com.voting.votingsystem.service.CandidateService;
import com.voting.votingsystem.service.ElectionService;
import com.voting.votingsystem.service.UserService;
import com.voting.votingsystem.service.VoteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/votes")
public class VoteController {

    private final VoteService voteService;
    private final UserService userService;
    private final CandidateService candidateService;
    private final ElectionService electionService;

    public VoteController(VoteService voteService,
                          UserService userService,
                          CandidateService candidateService,
                          ElectionService electionService) {
        this.voteService = voteService;
        this.userService = userService;
        this.candidateService = candidateService;
        this.electionService = electionService;
    }

    @PostMapping("/cast")
    public ResponseEntity<?> castVote(@RequestBody CastVoteRequest request, Authentication authentication) {
        try {
            User user = userService.getUserByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("Logged-in user not found: " + authentication.getName()));
            Candidate candidate = candidateService.getCandidateById(request.getCandidateId())
                    .orElseThrow(() -> new RuntimeException("Candidate not found with id: " + request.getCandidateId()));
            Election election = electionService.getElectionById(request.getElectionId())
                    .orElseThrow(() -> new RuntimeException("Election not found with id: " + request.getElectionId()));

            Vote vote = new Vote();
            vote.setUser(user);
            vote.setCandidate(candidate);
            vote.setElection(election);

            Vote savedVote = voteService.castVote(vote);
            return ResponseEntity.ok(savedVote);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * Legacy endpoint that accepts a full Vote object (not recommended; keep only for compatibility/testing).
     */
    @PostMapping("/cast-legacy")
    public ResponseEntity<?> castVoteLegacy(@RequestBody Vote vote) {
        try {
            Vote savedVote = voteService.castVote(vote);
            return ResponseEntity.ok(savedVote);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/all")
    public List<Vote> getAllVotes() {
        return voteService.getAllVotes();
    }

    @GetMapping("/election/{electionId}")
    public List<Vote> getVotesByElectionId(@PathVariable Long electionId) {
        return voteService.getVotesByElectionId(electionId);
    }

    @GetMapping("/candidate/{candidateId}")
    public List<Vote> getVotesByCandidateId(@PathVariable Long candidateId) {
        return voteService.getVotesByCandidateId(candidateId);
    }

    @GetMapping("/results/{electionId}")
    public List<ResultResponse> getElectionResults(@PathVariable Long electionId) {
        return voteService.getElectionResults(electionId);
    }
}
