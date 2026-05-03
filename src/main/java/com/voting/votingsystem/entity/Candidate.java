package com.voting.votingsystem.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "candidates")
public class Candidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String department;

    private String programName;

    private String contactNumber;

    private String email;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String bio;

    /**
     * Image path stored in DB (example: "/images/abc123.png").
     * The actual file is stored under src/main/resources/static/images/ in development.
     */
    private String profileImage;

    private String position;

    /**
     * Visibility flag for student pages/voting.
     * Nullable for backward compatibility when the DB column is first added; NULL is treated as active.
     */
    private Boolean active = true;

    @ManyToOne
    @JoinColumn(name = "election_id", nullable = false)
    private Election election;

    // Getters

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDepartment() {
        return department;
    }

    public String getProgramName() {
        return programName;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public String getEmail() {
        return email;
    }

    public String getBio() {
        return bio;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public String getPosition() {
        return position;
    }

    public boolean isActive() {
        return active == null || active;
    }

    public Election getElection() {
        return election;
    }

    // Setters

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public void setProgramName(String programName) {
        this.programName = programName;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setElection(Election election) {
        this.election = election;
    }
}
