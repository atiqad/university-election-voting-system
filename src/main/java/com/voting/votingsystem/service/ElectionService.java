package com.voting.votingsystem.service;

import com.voting.votingsystem.entity.Election;
import com.voting.votingsystem.repository.CandidateRepository;
import com.voting.votingsystem.repository.ElectionRepository;
import com.voting.votingsystem.repository.VoteRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ElectionService {

    private final ElectionRepository electionRepository;
    private final CandidateRepository candidateRepository;
    private final VoteRepository voteRepository;
    private final ActivityLogService activityLogService;
    private final Clock clock;

    public ElectionService(ElectionRepository electionRepository,
                           CandidateRepository candidateRepository,
                           VoteRepository voteRepository,
                           ActivityLogService activityLogService,
                           Clock clock) {
        this.electionRepository = electionRepository;
        this.candidateRepository = candidateRepository;
        this.voteRepository = voteRepository;
        this.activityLogService = activityLogService;
        this.clock = clock;
    }

    public Election saveElection(Election election) {
        validateElectionTitle(election);
        validateElectionDates(election.getStartDate(), election.getEndDate());
        // Keep the persisted "active" as the admin-enabled flag. The live open/closed state is derived
        // from time window checks (see isElectionOpenForVoting).
        Election saved = electionRepository.save(election);
        activityLogService.log("CREATE_ELECTION",
                "Election created. id=" + saved.getId() +
                        ", title=" + safe(saved.getTitle()) +
                        ", startDate=" + saved.getStartDate() +
                        ", endDate=" + saved.getEndDate() +
                        ", active=" + saved.isActive());
        applyCurrentlyOpen(saved);
        return saved;
    }

    public List<Election> getAllElections() {
        List<Election> elections = electionRepository.findAll();
        elections.forEach(this::applyCurrentlyOpen);
        return elections;
    }

    public Optional<Election> getElectionById(Long id) {
        Optional<Election> election = electionRepository.findById(id);
        election.ifPresent(this::applyCurrentlyOpen);
        return election;
    }

    public Optional<Election> getActiveElection() {
        // "Active election" for students should mean currently open for voting (time window + enabled flag),
        // not just the stored boolean which can be stale or represent admin intent.
        List<Election> enabled = electionRepository.findByActive(true);
        for (Election e : enabled) {
            applyCurrentlyOpen(e);
        }
        return enabled.stream().filter(Election::isCurrentlyOpen).findFirst();
    }

    public List<Election> getElectionsByStatus(boolean active) {
        List<Election> elections = electionRepository.findByActive(active);
        elections.forEach(this::applyCurrentlyOpen);
        return elections;
    }

    public long countAll() {
        return electionRepository.count();
    }

    public long countActive() {
        return electionRepository.countByActiveTrue();
    }

    public Election updateElection(Long id, Election updatedElection) {
        return electionRepository.findById(id).map(election -> {
            validateElectionTitle(updatedElection);
            validateElectionDates(updatedElection.getStartDate(), updatedElection.getEndDate());
            election.setTitle(updatedElection.getTitle());
            election.setStartDate(updatedElection.getStartDate());
            election.setEndDate(updatedElection.getEndDate());
            election.setActive(updatedElection.isActive());
            Election saved = electionRepository.save(election);
            activityLogService.log("UPDATE_ELECTION",
                    "Election updated. id=" + saved.getId() +
                            ", title=" + safe(saved.getTitle()) +
                            ", startDate=" + saved.getStartDate() +
                            ", endDate=" + saved.getEndDate() +
                            ", active=" + saved.isActive());
            applyCurrentlyOpen(saved);
            return saved;
        }).orElseThrow(() -> new RuntimeException("Election not found with id: " + id));
    }

    /**
     * Admin control: flips the stored enabled flag (active=true/false) and persists it.
     * Note: Voting still depends on the time window (start/end).
     */
    public Election toggleElectionStatus(Long id) {
        Election election = electionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Election not found with id: " + id));

        election.setActive(!election.isActive());
        Election saved = electionRepository.save(election);
        activityLogService.log("TOGGLE_ELECTION_STATUS",
                "Election status toggled. id=" + saved.getId() +
                        ", title=" + safe(saved.getTitle()) +
                        ", active=" + saved.isActive());
        applyCurrentlyOpen(saved);
        return saved;
    }

    public void deleteElection(Long id) {
        if (candidateRepository.existsByElectionId(id)) {
            throw new RuntimeException("Cannot delete election because it has candidates. Delete candidates first.");
        }
        if (voteRepository.existsByElectionId(id)) {
            throw new RuntimeException("Cannot delete election because it already has votes. Deleting votes is not allowed in this demo.");
        }

        Election existing = electionRepository.findById(id).orElse(null);
        electionRepository.deleteById(id);
        activityLogService.log("DELETE_ELECTION",
                "Election deleted. id=" + id + ", title=" + (existing == null ? null : safe(existing.getTitle())));
    }

    public boolean isElectionOpenForVoting(Election election) {
        // "active" is the admin-enabled flag (stored). The real open/closed state must be derived
        // from the time window to avoid stale state bugs when deadlines are changed.
        if (!election.isActive()) {
            applyCurrentlyOpen(election);
            return false;
        }

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime start = election.getStartDate();
        LocalDateTime end = election.getEndDate();

        boolean started = (start == null) || !now.isBefore(start);
        boolean notEnded = (end == null) || !now.isAfter(end);

        boolean open = started && notEnded;
        election.setCurrentlyOpen(open);
        return open;
    }

    public void assertElectionOpenForVoting(Long electionId) {
        Election election = electionRepository.findById(electionId)
                .orElseThrow(() -> new RuntimeException("Election not found with id: " + electionId));

        if (!isElectionOpenForVoting(election)) {
            LocalDateTime now = LocalDateTime.now(clock);
            if (!election.isActive()) {
                throw new RuntimeException("Election is not active.");
            }
            if (election.getStartDate() != null && now.isBefore(election.getStartDate())) {
                throw new RuntimeException("Election has not started yet.");
            }
            throw new RuntimeException("Election has ended.");
        }
    }

    private void applyCurrentlyOpen(Election election) {
        if (election == null) {
            return;
        }
        if (!election.isActive()) {
            election.setCurrentlyOpen(false);
            return;
        }

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime start = election.getStartDate();
        LocalDateTime end = election.getEndDate();

        boolean started = (start == null) || !now.isBefore(start);
        boolean notEnded = (end == null) || !now.isAfter(end);
        election.setCurrentlyOpen(started && notEnded);
    }

    private void validateElectionDates(LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null && !start.isBefore(end)) {
            throw new RuntimeException("startDate must be before endDate.");
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s.replace("\n", " ").replace("\r", " ").trim();
    }

    private static void validateElectionTitle(Election election) {
        if (election == null) {
            throw new RuntimeException("Election is required.");
        }
        if (election.getTitle() == null || election.getTitle().trim().isEmpty()) {
            throw new RuntimeException("Election title is required.");
        }
    }
}
