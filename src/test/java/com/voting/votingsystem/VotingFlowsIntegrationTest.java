package com.voting.votingsystem;

import com.voting.votingsystem.dto.ResultResponse;
import com.voting.votingsystem.entity.Candidate;
import com.voting.votingsystem.entity.Election;
import com.voting.votingsystem.entity.User;
import com.voting.votingsystem.entity.Vote;
import com.voting.votingsystem.repository.CandidateRepository;
import com.voting.votingsystem.repository.ElectionRepository;
import com.voting.votingsystem.repository.UserRepository;
import com.voting.votingsystem.service.VoteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class VotingFlowsIntegrationTest {

    @Autowired
    VoteService voteService;

    @Autowired
    ElectionRepository electionRepository;

    @Autowired
    CandidateRepository candidateRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    MockMvc mockMvc;

    @Test
    void duplicateVoteRestriction_studentCanVoteOnlyOncePerElection() {
        Election election = new Election();
        election.setTitle("Test Election");
        election.setStartDate(LocalDateTime.now().minusHours(1));
        election.setEndDate(LocalDateTime.now().plusHours(1));
        election.setActive(true);
        election = electionRepository.save(election);

        Candidate candidate = new Candidate();
        candidate.setName("Alice");
        candidate.setElection(election);
        candidate = candidateRepository.save(candidate);

        User student = new User();
        student.setFullName("Student One");
        student.setRegNo("REG-001");
        student.setEmail("student1@example.com");
        student.setPassword("x");
        student.setRole("STUDENT");
        student = userRepository.save(student);

        Vote v1 = new Vote();
        v1.setUser(student);
        v1.setElection(election);
        v1.setCandidate(candidate);
        voteService.castVote(v1);

        Vote v2 = new Vote();
        v2.setUser(student);
        v2.setElection(election);
        v2.setCandidate(candidate);

        assertThatThrownBy(() -> voteService.castVote(v2))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already voted");
    }

    @Test
    void resultCounting_winnerIsCandidateWithMostVotes() {
        Election election = new Election();
        election.setTitle("Counting Election");
        election.setStartDate(LocalDateTime.now().minusHours(1));
        election.setEndDate(LocalDateTime.now().plusHours(1));
        election.setActive(true);
        election = electionRepository.save(election);

        Candidate a = new Candidate();
        a.setName("Candidate A");
        a.setElection(election);
        a = candidateRepository.save(a);

        Candidate b = new Candidate();
        b.setName("Candidate B");
        b.setElection(election);
        b = candidateRepository.save(b);

        User s1 = new User();
        s1.setFullName("S1");
        s1.setRegNo("REG-101");
        s1.setEmail("s1@example.com");
        s1.setPassword("x");
        s1.setRole("STUDENT");
        s1 = userRepository.save(s1);

        User s2 = new User();
        s2.setFullName("S2");
        s2.setRegNo("REG-102");
        s2.setEmail("s2@example.com");
        s2.setPassword("x");
        s2.setRole("STUDENT");
        s2 = userRepository.save(s2);

        Vote v1 = new Vote();
        v1.setUser(s1);
        v1.setElection(election);
        v1.setCandidate(a);
        voteService.castVote(v1);

        Vote v2 = new Vote();
        v2.setUser(s2);
        v2.setElection(election);
        v2.setCandidate(a);
        voteService.castVote(v2);

        ResultResponse winner = voteService.getWinnerFromStats(election.getId());
        assertThat(winner).isNotNull();
        assertThat(winner.getCandidateName()).contains("Candidate A");
        assertThat(winner.getTotalVotes()).isEqualTo(2L);

        assertThat(voteService.getElectionResults(election.getId()))
                .anyMatch(r -> r.getCandidateName().equals("Candidate A") && r.getTotalVotes() == 2L);
    }

    @Test
    void electionRule_inactiveElectionBlocksVoting() {
        Election election = new Election();
        election.setTitle("Inactive Election");
        election.setStartDate(LocalDateTime.now().minusHours(1));
        election.setEndDate(LocalDateTime.now().plusHours(1));
        election.setActive(false);
        election = electionRepository.save(election);

        Candidate candidate = new Candidate();
        candidate.setName("Alice");
        candidate.setElection(election);
        candidate = candidateRepository.save(candidate);

        User student = new User();
        student.setFullName("Student Two");
        student.setRegNo("REG-002");
        student.setEmail("student2@example.com");
        student.setPassword("x");
        student.setRole("STUDENT");
        student = userRepository.save(student);

        Vote v = new Vote();
        v.setUser(student);
        v.setElection(election);
        v.setCandidate(candidate);

        assertThatThrownBy(() -> voteService.castVote(v))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not active");
    }

    @Test
    void electionRule_notStartedYetBlocksVoting() {
        Election election = new Election();
        election.setTitle("Future Election");
        election.setStartDate(LocalDateTime.now().plusHours(2));
        election.setEndDate(LocalDateTime.now().plusHours(3));
        election.setActive(true);
        election = electionRepository.save(election);

        Candidate candidate = new Candidate();
        candidate.setName("Bob");
        candidate.setElection(election);
        candidate = candidateRepository.save(candidate);

        User student = new User();
        student.setFullName("Student Three");
        student.setRegNo("REG-003");
        student.setEmail("student3@example.com");
        student.setPassword("x");
        student.setRole("STUDENT");
        student = userRepository.save(student);

        Vote v = new Vote();
        v.setUser(student);
        v.setElection(election);
        v.setCandidate(candidate);

        assertThatThrownBy(() -> voteService.castVote(v))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not started");
    }

    @Test
    void electionRule_endedElectionBlocksVoting() {
        Election election = new Election();
        election.setTitle("Ended Election");
        election.setStartDate(LocalDateTime.now().minusHours(3));
        election.setEndDate(LocalDateTime.now().minusHours(1));
        election.setActive(true);
        election = electionRepository.save(election);

        Candidate candidate = new Candidate();
        candidate.setName("Carol");
        candidate.setElection(election);
        candidate = candidateRepository.save(candidate);

        User student = new User();
        student.setFullName("Student Four");
        student.setRegNo("REG-004");
        student.setEmail("student4@example.com");
        student.setPassword("x");
        student.setRole("STUDENT");
        student = userRepository.save(student);

        Vote v = new Vote();
        v.setUser(student);
        v.setElection(election);
        v.setCandidate(candidate);

        assertThatThrownBy(() -> voteService.castVote(v))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("ended");
    }

    @Test
    void securityRule_adminRoleCannotVoteEvenIfTheyTry() {
        Election election = new Election();
        election.setTitle("Admin Vote Block");
        election.setStartDate(LocalDateTime.now().minusHours(1));
        election.setEndDate(LocalDateTime.now().plusHours(1));
        election.setActive(true);
        election = electionRepository.save(election);

        Candidate candidate = new Candidate();
        candidate.setName("Dave");
        candidate.setElection(election);
        candidate = candidateRepository.save(candidate);

        User admin = new User();
        admin.setFullName("Admin User");
        admin.setRegNo("ADMIN-TEST-1");
        admin.setEmail("admin-test@example.com");
        admin.setPassword("x");
        admin.setRole("ADMIN");
        admin = userRepository.save(admin);

        Vote v = new Vote();
        v.setUser(admin);
        v.setElection(election);
        v.setCandidate(candidate);

        assertThatThrownBy(() -> voteService.castVote(v))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Only STUDENT");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void roleBasedAccess_adminCanOpenAdminDashboard() throws Exception {
        mockMvc.perform(get("/admin-dashboard"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void roleBasedAccess_studentCannotOpenAdminDashboard() throws Exception {
        mockMvc.perform(get("/admin-dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanOpenCreateElectionForm() throws Exception {
        mockMvc.perform(get("/admin/elections/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("election-form"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("action=\"/admin/elections\"")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanCreateElection_withDateTimes() throws Exception {
        mockMvc.perform(post("/admin/elections")
                        .with(csrf())
                        .param("title", "UI Test Election")
                        .param("startDate", "2026-04-30T10:00:00")
                        .param("endDate", "2026-04-30T12:00:30")
                        .param("active", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/elections"));
    }
}
