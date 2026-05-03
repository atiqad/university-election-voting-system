package com.voting.votingsystem.controller;

import com.voting.votingsystem.entity.Candidate;
import com.voting.votingsystem.entity.Election;
import com.voting.votingsystem.service.CandidateService;
import com.voting.votingsystem.service.ElectionService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/candidates")
public class CandidateController {

    private final CandidateService candidateService;
    private final ElectionService electionService;

    public CandidateController(CandidateService candidateService, ElectionService electionService) {
        this.candidateService = candidateService;
        this.electionService = electionService;
    }

    // ===== Admin Thymeleaf Pages =====

    @GetMapping("/new")
    public String newCandidatePage(Model model) {
        model.addAttribute("elections", electionService.getAllElections());
        model.addAttribute("candidate", new Candidate());
        model.addAttribute("mode", "create");
        return "candidate-form";
    }

    @GetMapping("/edit/{id}")
    public String editCandidatePage(@PathVariable Long id, Model model) {
        Candidate candidate = candidateService.getCandidateById(id)
                .orElseThrow(() -> new RuntimeException("Candidate not found with id: " + id));
        model.addAttribute("elections", electionService.getAllElections());
        model.addAttribute("candidate", candidate);
        model.addAttribute("mode", "edit");
        return "candidate-form";
    }

    @PostMapping(value = "/add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String addCandidate(@RequestParam String name,
                               @RequestParam(required = false) String department,
                               @RequestParam(required = false) String programName,
                               @RequestParam(required = false) String contactNumber,
                               @RequestParam(required = false) String email,
                               @RequestParam(required = false) String position,
                               @RequestParam(required = false) String bio,
                               @RequestParam(required = false) String active,
                               @RequestParam Long electionId,
                               @RequestParam(required = false, name = "image") MultipartFile image,
                               RedirectAttributes redirectAttributes) {
        try {
            Election election = electionService.getElectionById(electionId)
                    .orElseThrow(() -> new RuntimeException("Election not found with id: " + electionId));

            Candidate candidate = new Candidate();
            candidate.setName(name);
            candidate.setDepartment(department);
            candidate.setProgramName(programName);
            candidate.setContactNumber(contactNumber);
            candidate.setEmail(email);
            candidate.setPosition(position);
            candidate.setBio(bio);
            candidate.setActive(active != null);
            candidate.setElection(election);

            candidateService.saveCandidateWithImage(candidate, image);
            redirectAttributes.addFlashAttribute("successMessage", "Candidate added successfully.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/candidates/new";
        }

        return "redirect:/candidates-page";
    }

    @PostMapping(value = "/update/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String updateCandidateForm(@PathVariable Long id,
                                      @RequestParam String name,
                                      @RequestParam(required = false) String department,
                                      @RequestParam(required = false) String programName,
                                      @RequestParam(required = false) String contactNumber,
                                      @RequestParam(required = false) String email,
                                      @RequestParam(required = false) String position,
                                      @RequestParam(required = false) String bio,
                                      @RequestParam(required = false) String active,
                                      @RequestParam Long electionId,
                                      @RequestParam(required = false, name = "image") MultipartFile image,
                                      @RequestParam(required = false) String profileImage,
                                      RedirectAttributes redirectAttributes) {
        try {
            Election election = electionService.getElectionById(electionId)
                    .orElseThrow(() -> new RuntimeException("Election not found with id: " + electionId));

            Candidate updated = new Candidate();
            updated.setName(name);
            updated.setDepartment(department);
            updated.setProgramName(programName);
            updated.setContactNumber(contactNumber);
            updated.setEmail(email);
            updated.setPosition(position);
            updated.setBio(bio);
            updated.setElection(election);
            updated.setProfileImage(profileImage);
            updated.setActive(active != null);

            candidateService.updateCandidateWithImage(id, updated, image);
            redirectAttributes.addFlashAttribute("successMessage", "Candidate updated successfully.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/candidates/edit/" + id;
        }

        return "redirect:/candidates-page";
    }

    @PostMapping("/delete/{id}")
    public String deleteCandidateForm(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            candidateService.deleteCandidate(id);
            redirectAttributes.addFlashAttribute("successMessage", "Candidate deleted successfully.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/candidates-page";
    }

    @PostMapping("/deactivate/{id}")
    public String deactivateCandidate(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            candidateService.setCandidateActive(id, false);
            redirectAttributes.addFlashAttribute("successMessage", "Candidate hidden successfully.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/candidates-page";
    }

    @PostMapping("/activate/{id}")
    public String activateCandidate(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            candidateService.setCandidateActive(id, true);
            redirectAttributes.addFlashAttribute("successMessage", "Candidate activated successfully.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/candidates-page";
    }

    // ===== Existing JSON API (kept for compatibility) =====

    @PostMapping(value = "/add", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Candidate addCandidateApi(@RequestBody Candidate candidate) {
        return candidateService.saveCandidate(candidate);
    }

    @GetMapping("/all")
    @ResponseBody
    public List<Candidate> getAllCandidates() {
        return candidateService.getAllCandidates();
    }

    @GetMapping("/{id}")
    @ResponseBody
    public Optional<Candidate> getCandidateById(@PathVariable Long id) {
        return candidateService.getCandidateById(id);
    }

    @GetMapping("/election/{electionId}")
    @ResponseBody
    public List<Candidate> getCandidatesByElectionId(@PathVariable Long electionId) {
        return candidateService.getCandidatesByElectionId(electionId);
    }

    @PutMapping("/update/{id}")
    @ResponseBody
    public Candidate updateCandidate(@PathVariable Long id, @RequestBody Candidate candidate) {
        return candidateService.updateCandidate(id, candidate);
    }

    @DeleteMapping("/delete/{id}")
    @ResponseBody
    public String deleteCandidate(@PathVariable Long id) {
        candidateService.deleteCandidate(id);
        return "Candidate deleted successfully with id: " + id;
    }
}
