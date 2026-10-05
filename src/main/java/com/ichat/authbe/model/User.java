package com.ichat.authbe.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "app_user", uniqueConstraints = @UniqueConstraint(columnNames = "username"))
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    // Nullable: a user who signed up purely via Google/Facebook has no
    // local password until/unless they set one.
    @Column
    private String passwordHash;

    // Nullable: required at password registration (enforced by RegisterRequest
    // validation), but not collected for Google/Facebook sign-ups.
    @Column
    private Integer birthYear;

    @Column
    private String phoneNumber;

    // Used to auto-link a Google/Facebook sign-in to an existing account.
    // Nullable because the original registration form doesn't require it;
    // only accounts that supplied it (or signed up via a social provider)
    // can be auto-linked by email.
    @Column(unique = true)
    private String email;

    // Subscription is active while this date is today or in the future.
    // Null means the user has never had a subscription.
    @Column(name = "subscription_expires_at")
    private LocalDate subscriptionExpiresAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<LinkedIdentity> linkedIdentities = new ArrayList<>();

    public User() {}

    public User(String username, String firstName, String lastName, String passwordHash,
                Integer birthYear, String phoneNumber, String email, LocalDate subscriptionExpiresAt) {
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.passwordHash = passwordHash;
        this.birthYear = birthYear;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.subscriptionExpiresAt = subscriptionExpiresAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public Integer getBirthYear() { return birthYear; }
    public void setBirthYear(Integer birthYear) { this.birthYear = birthYear; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public LocalDate getSubscriptionExpiresAt() { return subscriptionExpiresAt; }
    public void setSubscriptionExpiresAt(LocalDate subscriptionExpiresAt) { this.subscriptionExpiresAt = subscriptionExpiresAt; }

    public List<LinkedIdentity> getLinkedIdentities() { return linkedIdentities; }
    public void setLinkedIdentities(List<LinkedIdentity> linkedIdentities) { this.linkedIdentities = linkedIdentities; }
}
