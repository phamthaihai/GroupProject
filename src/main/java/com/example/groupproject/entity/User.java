package com.example.groupproject.entity;

import jakarta.persistence.*;
import java.time.ZonedDateTime;

@Entity
@Table(name = "users")
public class User {
    public enum UserRole {
        ADMIN,
        HR_MANAGER,
        INTERVIEWER,
        CANDIDATE
    }

    public enum UserStatus {
        ACTIVE,
        LOCKED,
        INACTIVE
    }
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "failed_login_count", nullable = false)
    private Short failedLoginCount = 0;

    @Column(name = "locked_at")
    private ZonedDateTime lockedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;

    public User() {
    }

    public User(Integer id, String fullName, String username, String email, String passwordHash,
                UserRole role, UserStatus status, Short failedLoginCount, ZonedDateTime lockedAt) {
        this.id = id;
        this.fullName = fullName;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.status = status != null ? status : UserStatus.ACTIVE;
        this.failedLoginCount = failedLoginCount != null ? failedLoginCount : 0;
        this.lockedAt = lockedAt;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = ZonedDateTime.now();
        this.updatedAt = ZonedDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = ZonedDateTime.now();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public Short getFailedLoginCount() {
        return failedLoginCount;
    }

    public void setFailedLoginCount(Short failedLoginCount) {
        this.failedLoginCount = failedLoginCount;
    }

    public ZonedDateTime getLockedAt() {
        return lockedAt;
    }

    public void setLockedAt(ZonedDateTime lockedAt) {
        this.lockedAt = lockedAt;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public ZonedDateTime getUpdatedAt() {
        return updatedAt;
    }
}