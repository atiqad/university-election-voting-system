package com.voting.votingsystem.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "elections")
public class Election {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private boolean active;

    /**
     * Derived at runtime from (active/enabled flag) + start/end window.
     * Not persisted. Used for displaying the real-time status without relying on stale DB state.
     */
    @Transient
    private Boolean currentlyOpen;

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isCurrentlyOpen() {
        // If not populated by the service/controller, fall back to "active" (enabled) so old code still works.
        return currentlyOpen != null ? currentlyOpen : active;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setCurrentlyOpen(Boolean currentlyOpen) {
        this.currentlyOpen = currentlyOpen;
    }
}
