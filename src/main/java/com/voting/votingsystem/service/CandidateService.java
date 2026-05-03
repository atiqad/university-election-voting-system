package com.voting.votingsystem.service;

import com.voting.votingsystem.entity.Candidate;
import com.voting.votingsystem.repository.CandidateRepository;
import com.voting.votingsystem.repository.VoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CandidateService {

    private final CandidateRepository candidateRepository;
    private final VoteRepository voteRepository;
    private final ActivityLogService activityLogService;
    private final Path imageDir = Paths.get("src/main/resources/static/images");

    public CandidateService(CandidateRepository candidateRepository,
                            VoteRepository voteRepository,
                            ActivityLogService activityLogService) {
        this.candidateRepository = candidateRepository;
        this.voteRepository = voteRepository;
        this.activityLogService = activityLogService;
    }

    public Candidate saveCandidate(Candidate candidate) {
        validateCandidate(candidate);
        Candidate saved = candidateRepository.save(candidate);
        activityLogService.log("ADD_CANDIDATE",
                "Candidate created. id=" + saved.getId() +
                        ", name=" + safe(saved.getName()) +
                        ", electionId=" + (saved.getElection() == null ? null : saved.getElection().getId()));
        return saved;
    }

    public Candidate saveCandidateWithImage(Candidate candidate, MultipartFile imageFile) {
        validateCandidate(candidate);
        if (imageFile != null && !imageFile.isEmpty()) {
            candidate.setProfileImage(storeImage(imageFile));
        }
        Candidate saved = candidateRepository.save(candidate);
        activityLogService.log("ADD_CANDIDATE",
                "Candidate created. id=" + saved.getId() +
                        ", name=" + safe(saved.getName()) +
                        ", electionId=" + (saved.getElection() == null ? null : saved.getElection().getId()));
        return saved;
    }

    public List<Candidate> getAllCandidates() {
        return candidateRepository.findAll();
    }

    public List<Candidate> getAllActiveCandidates() {
        return candidateRepository.findVisibleCandidates();
    }

    public Optional<Candidate> getCandidateById(Long id) {
        return candidateRepository.findById(id);
    }

    public List<Candidate> getCandidatesByElectionId(Long electionId) {
        return candidateRepository.findByElectionId(electionId);
    }

    public List<Candidate> getActiveCandidatesByElectionId(Long electionId) {
        return candidateRepository.findVisibleCandidatesByElectionId(electionId);
    }

    public long countAll() {
        return candidateRepository.count();
    }

    public Candidate updateCandidate(Long id, Candidate updatedCandidate) {
        return candidateRepository.findById(id).map(candidate -> {
            validateCandidate(updatedCandidate);
            candidate.setName(updatedCandidate.getName());
            candidate.setDepartment(updatedCandidate.getDepartment());
            candidate.setProgramName(updatedCandidate.getProgramName());
            candidate.setContactNumber(updatedCandidate.getContactNumber());
            candidate.setEmail(updatedCandidate.getEmail());
            candidate.setBio(updatedCandidate.getBio());
            candidate.setProfileImage(updatedCandidate.getProfileImage());
            candidate.setPosition(updatedCandidate.getPosition());
            candidate.setActive(updatedCandidate.isActive());
            candidate.setElection(updatedCandidate.getElection());
            Candidate saved = candidateRepository.save(candidate);
            activityLogService.log("UPDATE_CANDIDATE",
                    "Candidate updated. id=" + saved.getId() +
                            ", name=" + safe(saved.getName()) +
                            ", electionId=" + (saved.getElection() == null ? null : saved.getElection().getId()));
            return saved;
        }).orElseThrow(() -> new RuntimeException("Candidate not found with id: " + id));
    }

    public Candidate updateCandidateWithImage(Long id, Candidate updatedCandidate, MultipartFile imageFile) {
        return candidateRepository.findById(id).map(candidate -> {
            validateCandidate(updatedCandidate);
            candidate.setName(updatedCandidate.getName());
            candidate.setDepartment(updatedCandidate.getDepartment());
            candidate.setProgramName(updatedCandidate.getProgramName());
            candidate.setContactNumber(updatedCandidate.getContactNumber());
            candidate.setEmail(updatedCandidate.getEmail());
            candidate.setBio(updatedCandidate.getBio());
            candidate.setPosition(updatedCandidate.getPosition());
            candidate.setActive(updatedCandidate.isActive());
            candidate.setElection(updatedCandidate.getElection());

            if (imageFile != null && !imageFile.isEmpty()) {
                candidate.setProfileImage(storeImage(imageFile));
            } else if (updatedCandidate.getProfileImage() != null && !updatedCandidate.getProfileImage().isBlank()) {
                // Allows keeping an existing path when the edit form doesn't upload a new file.
                candidate.setProfileImage(updatedCandidate.getProfileImage());
            }

            Candidate saved = candidateRepository.save(candidate);
            activityLogService.log("UPDATE_CANDIDATE",
                    "Candidate updated. id=" + saved.getId() +
                            ", name=" + safe(saved.getName()) +
                            ", electionId=" + (saved.getElection() == null ? null : saved.getElection().getId()));
            return saved;
        }).orElseThrow(() -> new RuntimeException("Candidate not found with id: " + id));
    }

    public void setCandidateActive(Long id, boolean active) {
        Candidate candidate = candidateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Candidate not found with id: " + id));
        candidate.setActive(active);
        Candidate saved = candidateRepository.save(candidate);
        activityLogService.log(active ? "ACTIVATE_CANDIDATE" : "DEACTIVATE_CANDIDATE",
                "Candidate " + (active ? "activated" : "deactivated") + ". id=" + saved.getId() +
                        ", name=" + safe(saved.getName()) +
                        ", electionId=" + (saved.getElection() == null ? null : saved.getElection().getId()));
    }

    public void deleteCandidate(Long id) {
        Candidate existing = candidateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Candidate not found with id: " + id));

        if (voteRepository.existsByCandidateId(id)) {
            throw new RuntimeException(
                    "This candidate cannot be permanently deleted because votes already exist. " +
                            "You may deactivate the candidate to preserve election records."
            );
        }

        candidateRepository.deleteById(id);
        activityLogService.log("DELETE_CANDIDATE",
                "Candidate deleted. id=" + id +
                        ", name=" + safe(existing.getName()) +
                        ", electionId=" + (existing.getElection() == null ? null : existing.getElection().getId()));
    }

    private String storeImage(MultipartFile file) {
        try {
            Files.createDirectories(imageDir);

            String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
            String extension = "";
            int dot = originalFilename.lastIndexOf('.');
            if (dot >= 0 && dot < originalFilename.length() - 1) {
                extension = originalFilename.substring(dot);
            }

            String filename = UUID.randomUUID() + extension;
            Path destination = imageDir.resolve(filename);

            try (InputStream in = file.getInputStream()) {
                Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
            }

            // Store web path in DB so Thymeleaf can render it directly.
            return "/images/" + filename;
        } catch (IOException ex) {
            throw new RuntimeException("Failed to save image: " + ex.getMessage(), ex);
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s.replace("\n", " ").replace("\r", " ").trim();
    }

    private static void validateCandidate(Candidate candidate) {
        if (candidate == null) {
            throw new RuntimeException("Candidate is required.");
        }
        if (candidate.getName() == null || candidate.getName().trim().isEmpty()) {
            throw new RuntimeException("Candidate name is required.");
        }
        if (candidate.getElection() == null || candidate.getElection().getId() == null) {
            throw new RuntimeException("Election is required for candidate.");
        }
    }
}
