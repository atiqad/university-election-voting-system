package com.voting.votingsystem.controller;

import com.voting.votingsystem.service.CandidateService;
import com.voting.votingsystem.service.ElectionService;
import com.voting.votingsystem.service.UserService;
import com.voting.votingsystem.service.VoteService;
import com.voting.votingsystem.entity.Vote;
import com.voting.votingsystem.entity.User;
import com.voting.votingsystem.entity.Candidate;
import com.voting.votingsystem.entity.Election;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Comparator;
import java.util.List;

@Controller
public class PageController {

    private final ElectionService electionService;
    private final CandidateService candidateService;
    private final VoteService voteService;
    private final UserService userService;
    private final Clock clock;

    private static final DateTimeFormatter DISPLAY_DT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a", Locale.ENGLISH);

    public PageController(ElectionService electionService,
                          CandidateService candidateService,
                          VoteService voteService,
                          UserService userService,
                          Clock clock) {
        this.electionService = electionService;
        this.candidateService = candidateService;
        this.voteService = voteService;
        this.userService = userService;
        this.clock = clock;
    }

    @GetMapping({"/login", "/login/"})
    public String loginPage() {
        return "redirect:/student-login";
    }

    @GetMapping("/student-login")
    public String studentLoginPage() {
        return "student-login";
    }

    @GetMapping("/admin-login")
    public String adminLoginPage() {
        return "admin-login";
    }

    @GetMapping({"/register", "/register/"})
    public String registerPage() {
        return "register";
    }

    @PostMapping({"/register", "/register/"})
    public String registerStudent(@RequestParam String fullName,
                                  @RequestParam String regNo,
                                  @RequestParam String email,
                                  @RequestParam String password,
                                  RedirectAttributes redirectAttributes) {
        try {
            User user = new User();
            user.setFullName(fullName);
            user.setRegNo(regNo);
            user.setEmail(email);
            user.setPassword(password); // encoded in UserService.saveUser()
            user.setRole("STUDENT");

            userService.saveUser(user);
            redirectAttributes.addFlashAttribute("successMessage", "Registration successful. Please login.");
            return "redirect:/login";
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/register";
        }
    }

    @GetMapping("/student-dashboard")
    public String studentDashboard(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            // Should not happen because SecurityConfig requires authentication, but keep it safe.
            return "redirect:/login";
        }

        User user = userService.getUserByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Logged-in user not found: " + auth.getName()));

        model.addAttribute("studentName", user.getFullName());
        model.addAttribute("studentEmail", user.getEmail());
        model.addAttribute("lastUpdated", formatDisplay(LocalDateTime.now(clock)));

        long activeElections = electionService.countActive();
        long totalElections = electionService.countAll();
        long votesCount = voteService.countByUser(user.getId());

        model.addAttribute("activeElections", activeElections);
        model.addAttribute("totalElections", totalElections);
        model.addAttribute("votesCount", votesCount);

        // Dashboard highlight: the currently open election for voting (if any).
        Election activeElection = electionService.getActiveElection().orElse(null);
        if (activeElection != null) {
            model.addAttribute("activeElectionTitle", activeElection.getTitle());
            model.addAttribute("activeElectionStatus", activeElection.isCurrentlyOpen() ? "Open" : "Completed");
            model.addAttribute("activeElectionId", activeElection.getId());
        }
        return "student-dashboard";
    }

    @GetMapping("/admin-dashboard")
    public String adminDashboard(Model model) {
        long totalElections = electionService.countAll();
        long activeElections = electionService.countActive();
        long totalCandidates = candidateService.countAll();
        long totalVotes = voteService.countAll();
        long registeredStudents = userService.countByRole("STUDENT");

        model.addAttribute("totalElections", totalElections);
        model.addAttribute("activeElections", activeElections);
        model.addAttribute("totalCandidates", totalCandidates);
        model.addAttribute("totalVotes", totalVotes);
        model.addAttribute("registeredStudents", registeredStudents);
        return "admin-dashboard";
    }

    @GetMapping("/elections-page")
    public String electionsPage(Model model) {
        model.addAttribute("elections", electionService.getAllElections());
        return "elections-page";
    }

    @GetMapping("/candidates-page")
    public String candidatesPage(Authentication authentication, Model model) {
        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        model.addAttribute("candidates", isAdmin
                ? candidateService.getAllCandidates()
                : candidateService.getAllActiveCandidates());
        return "candidates-page";
    }

    @GetMapping("/vote-page")
    public String votePage(@RequestParam(required = false) Long electionId,
                           Authentication authentication,
                           Model model) {
        // Logged-in student (email is used as username in SecurityConfig).
        User user = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Logged-in user not found: " + authentication.getName()));

        // Choose election: query param -> active election -> first election.
        List<Election> elections = electionService.getAllElections();
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

        model.addAttribute("elections", elections);
        model.addAttribute("election", selected);

        LocalDateTime now = LocalDateTime.now(clock);
        model.addAttribute("serverNow", formatDisplay(now));
        model.addAttribute("serverZone", clock.getZone().toString());
        model.addAttribute("electionStart", selected == null ? null : formatDisplay(selected.getStartDate()));
        model.addAttribute("electionEnd", selected == null ? null : formatDisplay(selected.getEndDate()));

        if (selected == null) {
            model.addAttribute("candidates", List.of());
            model.addAttribute("votingBlockedReason", "No election found. Ask the admin to create and activate an election.");
            return "vote-page";
        }

        // Used only for UI state (disable/hide vote button). Do not show an error on page load.
        boolean alreadyVoted = voteService.hasUserVoted(user.getId(), selected.getId());
        model.addAttribute("alreadyVoted", alreadyVoted);
        // Backwards/transition-friendly: keep old attribute name too.
        model.addAttribute("hasVoted", alreadyVoted);

        // Block voting by time window / active status.
        // UX: if the student already voted, show ONLY the "already voted" message (no extra "election closed" noise).
        if (!alreadyVoted) {
            boolean open = electionService.isElectionOpenForVoting(selected);
            if (!open) {
                model.addAttribute("votingBlockedReason", buildVotingBlockedReason(selected, now));
            }
        }

        model.addAttribute("selectedElectionId", selected.getId());
        model.addAttribute("candidates", candidateService.getActiveCandidatesByElectionId(selected.getId()));
        return "vote-page";
    }

    private static String buildVotingBlockedReason(Election election, LocalDateTime now) {
        // Note: ElectionService may have auto-closed the election if it expired, so we need to infer
        // a useful reason (inactive vs not started yet vs ended) from the timestamps.
        if (election == null) {
            return "Voting is currently closed for this election.";
        }

        LocalDateTime start = election.getStartDate();
        LocalDateTime end = election.getEndDate();

        if (start != null && start.isAfter(now)) {
            return "Voting has not started yet. Starts at " + formatDisplay(start) +
                    " (server time: " + formatDisplay(now) + ").";
        }

        if (end != null && end.isBefore(now)) {
            return "Voting has ended. Ended at " + formatDisplay(end) +
                    " (server time: " + formatDisplay(now) + ").";
        }

        if (!election.isActive()) {
            return "Voting is closed because this election is not active.";
        }

        return "Voting is currently closed for this election.";
    }

    private static String formatDisplay(LocalDateTime dt) {
        return dt == null ? null : dt.format(DISPLAY_DT);
    }

    @PostMapping("/submit-vote")
    public String submitVote(@RequestParam Long candidateId,
                             @RequestParam Long electionId,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        try {
            User user = userService.getUserByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("Logged-in user not found: " + authentication.getName()));

            // UX rule: only show the "already voted" message when the student actually submits again.
            // Keep backend validation in VoteService as the source of truth.
            if (voteService.hasUserVoted(user.getId(), electionId)) {
                redirectAttributes.addFlashAttribute("errorMessage", "You have already voted in this election");
                return "redirect:/vote-page?electionId=" + electionId;
            }

            Candidate candidate = candidateService.getCandidateById(candidateId)
                    .orElseThrow(() -> new RuntimeException("Candidate not found with id: " + candidateId));
            Election election = electionService.getElectionById(electionId)
                    .orElseThrow(() -> new RuntimeException("Election not found with id: " + electionId));

            Vote vote = new Vote();
            vote.setUser(user);
            vote.setCandidate(candidate);
            vote.setElection(election);

            voteService.castVote(vote);

            redirectAttributes.addFlashAttribute("successMessage", "Vote submitted successfully");
        } catch (RuntimeException ex) {
            // Includes duplicate-vote error from VoteService: "User has already voted in this election."
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        // PRG pattern: avoid duplicate submissions on refresh.
        return "redirect:/vote-page?electionId=" + electionId;
    }

    @GetMapping("/student-results-page")
    public String studentResultsPage(@RequestParam(required = false) Long electionId, Model model) {
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
            model.addAttribute("results", List.of());
            model.addAttribute("winner", null);
            return "student-results";
        }

        model.addAttribute("results", voteService.getElectionResults(selected.getId()));
        model.addAttribute("winner", voteService.getWinnerFromStats(selected.getId()));
        return "student-results";
    }

    // Backwards-compatible route (older templates used /results-page).
    @GetMapping("/results-page")
    public String resultsPageRedirect(Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        return isAdmin ? "redirect:/admin/results" : "redirect:/student-results-page";
    }
}
