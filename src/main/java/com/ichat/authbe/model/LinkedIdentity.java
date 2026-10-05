package com.ichat.authbe.model;

import jakarta.persistence.*;

@Entity
@Table(
    name = "linked_identity",
    uniqueConstraints = @UniqueConstraint(columnNames = {"provider", "provider_id"})
)
public class LinkedIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider provider;

    // The stable subject/user id the provider issues (Google "sub", Facebook "id") —
    // NOT the email, since emails can change on the provider's side.
    @Column(name = "provider_id", nullable = false)
    private String providerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public LinkedIdentity() {}

    public LinkedIdentity(AuthProvider provider, String providerId, User user) {
        this.provider = provider;
        this.providerId = providerId;
        this.user = user;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public AuthProvider getProvider() { return provider; }
    public void setProvider(AuthProvider provider) { this.provider = provider; }

    public String getProviderId() { return providerId; }
    public void setProviderId(String providerId) { this.providerId = providerId; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}
