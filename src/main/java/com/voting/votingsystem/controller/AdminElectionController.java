package com.voting.votingsystem.controller;

import com.voting.votingsystem.entity.Election;
import com.voting.votingsystem.service.ElectionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
import java.util.Locale;

@Controller
@RequestMapping("/admin/elections")
public class AdminElectionController {

    private final ElectionService electionService;
    private final Clock clock;
    // Accepts browser datetime-local values with or without seconds / fractional seconds.
    // Examples:
    // - 2026-04-30T10:00
    // - 2026-04-30T10:00:30
    // - 2026-04-30T10:00:30.123
    private static final DateTimeFormatter DATETIME_LOCAL = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd'T'HH:mm")
            .optionalStart()
            .appendPattern(":ss")
            .optionalStart()
            .appendFraction(ChronoField.NANO_OF_SECOND, 1, 9, true)
            .optionalEnd()
            .optionalEnd()
            .toFormatter();

    private static final DateTimeFormatter DISPLAY_DT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a", Locale.ENGLISH);

    public AdminElectionController(ElectionService electionService, Clock clock) {
        this.electionService = electionService;
        this.clock = clock;
    }

    @GetMapping
    public String electionsList(Model model) {
        model.addAttribute("elections", electionService.getAllElections());
        return "admin-elections";
    }

    @GetMapping("/new")
    public String newElectionForm(Model model) {
        Election election = new Election();
        model.addAttribute("election", election);
        model.addAttribute("mode", "create");
        model.addAttribute("formAction", "/admin/elections");
        model.addAttribute("startDateValue", "");
        model.addAttribute("endDateValue", "");
        addTimeMeta(model);
        return "election-form";
    }

    @PostMapping
    public String createElection(@RequestParam String title,
                                 @RequestParam(required = false) String startDate,
                                 @RequestParam(required = false) String endDate,
                                 @RequestParam(defaultValue = "false") boolean active,
                                 RedirectAttributes redirectAttributes) {
        try {
            Election election = new Election();
            election.setTitle(title);
            election.setStartDate(parseDateTimeOrNull(startDate));
            election.setEndDate(parseDateTimeOrNull(endDate));
            election.setActive(active);
            electionService.saveElection(election);
            redirectAttributes.addFlashAttribute("successMessage", "Election created successfully.");
            return "redirect:/admin/elections";
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/elections/new";
        }
    }

    @GetMapping("/{id}/edit")
    public String editElectionForm(@PathVariable Long id, Model model) {
        Election election = electionService.getElectionById(id)
                .orElseThrow(() -> new RuntimeException("Election not found with id: " + id));
        model.addAttribute("election", election);
        model.addAttribute("mode", "edit");
        model.addAttribute("formAction", "/admin/elections/" + election.getId());
        model.addAttribute("startDateValue", formatDateTimeOrEmpty(election.getStartDate()));
        model.addAttribute("endDateValue", formatDateTimeOrEmpty(election.getEndDate()));
        addTimeMeta(model);
        return "election-form";
    }

    private void addTimeMeta(Model model) {
        LocalDateTime now = LocalDateTime.now(clock);
        model.addAttribute("serverNow", now.format(DISPLAY_DT));
        model.addAttribute("serverZone", clock.getZone().toString());
    }

    @PostMapping("/{id}")
    public String updateElection(@PathVariable Long id,
                                 @RequestParam String title,
                                 @RequestParam(required = false) String startDate,
                                 @RequestParam(required = false) String endDate,
                                 @RequestParam(defaultValue = "false") boolean active,
                                 RedirectAttributes redirectAttributes) {
        try {
            Election updated = new Election();
            updated.setTitle(title);
            updated.setStartDate(parseDateTimeOrNull(startDate));
            updated.setEndDate(parseDateTimeOrNull(endDate));
            updated.setActive(active);
            electionService.updateElection(id, updated);
            redirectAttributes.addFlashAttribute("successMessage", "Election updated successfully.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/elections";
    }

    @PostMapping("/toggle/{id}")
    public String toggleElection(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Election saved = electionService.toggleElectionStatus(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Election " + (saved.isActive() ? "activated" : "deactivated") + " successfully.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/elections";
    }

    private static String formatDateTimeOrEmpty(LocalDateTime dt) {
        return dt == null ? "" : dt.format(DATETIME_LOCAL);
    }

    private static LocalDateTime parseDateTimeOrNull(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(trimmed, DATETIME_LOCAL);
    }

    @PostMapping("/{id}/delete")
    public String deleteElection(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            electionService.deleteElection(id);
            redirectAttributes.addFlashAttribute("successMessage", "Election deleted successfully.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/elections";
    }
}
