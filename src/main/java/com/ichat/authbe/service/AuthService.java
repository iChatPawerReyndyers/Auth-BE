package com.ichat.authbe.service;

import com.ichat.authbe.dto.AuthResponse;
import com.ichat.authbe.dto.LoginRequest;
import com.ichat.authbe.dto.RegisterRequest;
import com.ichat.authbe.exception.AuthException;
import com.ichat.authbe.model.User;
import com.ichat.authbe.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Dev/testing only — see application-dev.properties. Defaults to false.
    @Value("${app.security.skip-password-check:false}")
    private boolean skipPasswordCheck;

    // Days of free access granted when a registration/social sign-up doesn't
    // specify subscriptionExpiresAt itself. Configurable via application.properties.
    @Value("${app.subscription.default-trial-days:30}")
    private int defaultTrialDays;

    // Comma-separated app IDs exempt from the subscription gate (e.g.
    // "stickies,galleries,calculator"). An app ID NOT in this list — including
    // an unrecognized/misspelled one — defaults to REQUIRING an active
    // subscription. This fails closed on purpose: a typo in a free app's ID
    // wrongly blocks it (easy to notice and fix in config), rather than a
    // typo in a paid app's ID accidentally letting it through for free.
    @Value("${app.clients.free-ids:}")
    private String freeClientIdsRaw;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public int getDefaultTrialDays() {
        return defaultTrialDays;
    }

    /** True if this app ID is NOT on the free list — i.e. login for it requires an active subscription. */
    public boolean appRequiresSubscription(String appId) {
        if (appId == null || appId.isBlank()) return true;
        Set<String> freeIds = Arrays.stream(freeClientIdsRaw.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
        return !freeIds.contains(appId.trim().toLowerCase());
    }

    public AuthResponse register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new AuthException("Password and confirm password do not match");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AuthException("Username is already taken");
        }
        if (request.getEmail() != null && userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new AuthException("An account with this email already exists");
        }

        User user = new User(
                request.getUsername(),
                request.getFirstName(),
                request.getLastName(),
                passwordEncoder.encode(request.getPassword()),
                request.getBirthYear(),
                request.getPhoneNumber(),
                request.getEmail(),
                request.getSubscriptionExpiresAt() != null
                        ? request.getSubscriptionExpiresAt()
                        : LocalDate.now().plusDays(defaultTrialDays)
        );
        userRepository.save(user);

        return new AuthResponse(true, "Registration successful", user.getUsername(), isProfileComplete(user));
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new AuthException("Invalid username or password"));

        if (!skipPasswordCheck) {
            if (user.getPasswordHash() == null
                    || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
                throw new AuthException("Invalid username or password");
            }
        }

        return issueLoginResponse(user, appRequiresSubscription(request.getAppId()));
    }

    /**
     * Shared by password login and OAuth login: conditionally enforces the
     * subscription gate (per requiresSubscription, decided by the calling
     * app's ID) and builds the response. OAuthService calls this once it has
     * resolved (or created/linked) the User.
     */
    public AuthResponse issueLoginResponse(User user, boolean requiresSubscription) {
        if (requiresSubscription) {
            boolean hasActiveSubscription = user.getSubscriptionExpiresAt() != null
                    && !user.getSubscriptionExpiresAt().isBefore(LocalDate.now());

            if (!hasActiveSubscription) {
                throw new AuthException(
                        "Unable to log in: your subscription is not active. Please renew your subscription to continue."
                );
            }
        }

        return new AuthResponse(true, "Login successful", user.getUsername(), isProfileComplete(user));
    }

    /**
     * Fills in birthYear/phoneNumber on an existing account — used by the
     * blocking "complete your profile" modal after a Google/Facebook
     * sign-up that didn't provide them. Doesn't touch the subscription
     * check; the account was already past that to get here.
     */
    public AuthResponse completeProfile(String username, Integer birthYear, String phoneNumber) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AuthException("Account not found"));

        user.setBirthYear(birthYear);
        user.setPhoneNumber(phoneNumber);
        userRepository.save(user);

        return new AuthResponse(true, "Profile completed", user.getUsername(), true);
    }

    /**
     * True unless birthYear/phoneNumber are still missing — the case for a
     * Google/Facebook sign-up that was auto-created without them. Checked
     * live (not a stored flag) so it's always accurate, including for
     * accounts that were incomplete before this check existed.
     */
    public static boolean isProfileComplete(User user) {
        return user.getBirthYear() != null
                && user.getPhoneNumber() != null
                && !user.getPhoneNumber().isBlank();
    }
}
