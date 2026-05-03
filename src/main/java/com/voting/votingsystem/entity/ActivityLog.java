package com.voting.votingsystem.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "activity_logs",
        indexes = {
                @Index(name = "ix_activity_logs_created_at", columnList = "created_at"),
                @Index(name = "ix_activity_logs_action", columnList = "action"),
                @Index(name = "ix_activity_logs_user", columnList = "user_id")
        }
)
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String action;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String details;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "performed_by", nullable = false, length = 255)
    private String performedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public Long getId() {
        return id;
    }

    public String getAction() {
        return action;
    }

    public String getDetails() {
        return details;
    }

    public User getUser() {
        return user;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setPerformedBy(String performedBy) {
        this.performedBy = performedBy;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
