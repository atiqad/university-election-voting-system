package com.voting.votingsystem.controller;

import com.voting.votingsystem.entity.Election;
import com.voting.votingsystem.service.ElectionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/elections")
public class ElectionController {

    private final ElectionService electionService;

    public ElectionController(ElectionService electionService) {
        this.electionService = electionService;
    }

    @PostMapping("/add")
    public Election addElection(@RequestBody Election election) {
        return electionService.saveElection(election);
    }

    @GetMapping("/all")
    public List<Election> getAllElections() {
        return electionService.getAllElections();
    }

    @GetMapping("/{id}")
    public Optional<Election> getElectionById(@PathVariable Long id) {
        return electionService.getElectionById(id);
    }

    @GetMapping("/active")
    public Optional<Election> getActiveElection() {
        return electionService.getActiveElection();
    }

    @GetMapping("/status/{active}")
    public List<Election> getElectionsByStatus(@PathVariable boolean active) {
        return electionService.getElectionsByStatus(active);
    }

    @PutMapping("/update/{id}")
    public Election updateElection(@PathVariable Long id, @RequestBody Election election) {
        return electionService.updateElection(id, election);
    }

    @DeleteMapping("/delete/{id}")
    public String deleteElection(@PathVariable Long id) {
        electionService.deleteElection(id);
        return "Election deleted successfully with id: " + id;
    }
}