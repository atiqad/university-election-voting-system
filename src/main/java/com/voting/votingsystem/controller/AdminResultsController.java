package com.voting.votingsystem.controller;

import com.voting.votingsystem.dto.CandidateVoteStats;
import com.voting.votingsystem.dto.ResultResponse;
import com.voting.votingsystem.entity.Election;
import com.voting.votingsystem.service.ElectionService;
import com.voting.votingsystem.service.VoteService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Comparator;
import java.util.List;

@Controller
@RequestMapping("/admin/results")
public class AdminResultsController {

    private final ElectionService electionService;
    private final VoteService voteService;

    public AdminResultsController(ElectionService electionService, VoteService voteService) {
        this.electionService = electionService;
        this.voteService = voteService;
    }

    @GetMapping
    public String resultsDashboard(@RequestParam(required = false) Long electionId, Model model) {
        List<Election> elections = electionService.getAllElections();
        model.addAttribute("elections", elections);

        Election selected = null;
        if (electionId != null) {
            selected = electionService.getElectionById(electionId).orElse(null);
        }
        if (selected == null) {
            selected = electionService.getActiveElection().orElse(null);
        }
        if (selected == null && !elections.isEmpty()) {
            selected = elections.stream().min(Comparator.comparing(Election::getId)).orElse(null);
        }

        model.addAttribute("election", selected);

        if (selected == null) {
            model.addAttribute("stats", List.of());
            model.addAttribute("winner", null);
            model.addAttribute("totalVotes", 0L);
            return "admin-results";
        }

        List<CandidateVoteStats> stats = voteService.getElectionVoteStats(selected.getId());
        ResultResponse winner = voteService.getWinnerFromStats(selected.getId());

        // Sort for full ranking view (highest votes first).
        stats.sort(Comparator
                .comparing((CandidateVoteStats s) -> s.getTotalVotes() == null ? 0L : s.getTotalVotes())
                .reversed()
                .thenComparing(s -> s.getCandidateName() == null ? "" : s.getCandidateName(), String.CASE_INSENSITIVE_ORDER));

        long totalVotes = 0;
        for (CandidateVoteStats s : stats) {
            if (s.getTotalVotes() != null) {
                totalVotes += s.getTotalVotes();
            }
        }

        model.addAttribute("stats", stats);
        model.addAttribute("winner", winner);
        model.addAttribute("totalVotes", totalVotes);
        return "admin-results";
    }
}
