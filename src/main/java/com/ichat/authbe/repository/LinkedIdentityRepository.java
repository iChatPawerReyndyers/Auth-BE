package com.ichat.authbe.repository;

import com.ichat.authbe.model.AuthProvider;
import com.ichat.authbe.model.LinkedIdentity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LinkedIdentityRepository extends JpaRepository<LinkedIdentity, Long> {
    Optional<LinkedIdentity> findByProviderAndProviderId(AuthProvider provider, String providerId);
}
