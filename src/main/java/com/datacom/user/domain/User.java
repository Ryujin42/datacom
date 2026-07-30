package com.datacom.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "users")
public class User {

    public static final int MAX_FAILED_ATTEMPTS = 5;
    public static final long FAILURE_WINDOW_MINUTES = 15;
    public static final long LOCK_DURATION_MINUTES = 15;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String login;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false, length = 100)
    private String firstname;

    @Column(nullable = false, length = 100)
    private String lastname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "failed_attempts", nullable = false)
    private short failedAttempts;

    @Column(name = "last_failed_attempt_at")
    private Instant lastFailedAttemptAt;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected User() {
        // JPA
    }

    public User(String login, String passwordHash, String firstname, String lastname, Role role) {
        this.login = login;
        this.passwordHash = passwordHash;
        this.firstname = firstname;
        this.lastname = lastname;
        this.role = role;
        this.enabled = true;
    }

    public void registerFailedAttempt(Instant now) {
        boolean withinWindow =
                lastFailedAttemptAt != null
                        && lastFailedAttemptAt
                                .plus(FAILURE_WINDOW_MINUTES, ChronoUnit.MINUTES)
                                .isAfter(now);
        failedAttempts = (short) (withinWindow ? failedAttempts + 1 : 1);
        lastFailedAttemptAt = now;
        if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
            lockedUntil = now.plus(LOCK_DURATION_MINUTES, ChronoUnit.MINUTES);
        }
    }

    public void registerSuccessfulLogin() {
        failedAttempts = 0;
        lastFailedAttemptAt = null;
        lockedUntil = null;
    }

    public boolean isLocked(Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void changePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
    }

    public Long getId() {
        return id;
    }

    public String getLogin() {
        return login;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getFirstname() {
        return firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public Role getRole() {
        return role;
    }

    public short getFailedAttempts() {
        return failedAttempts;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }
}
