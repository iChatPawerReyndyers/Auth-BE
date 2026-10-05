package com.ichat.authbe.service;

import com.ichat.authbe.dto.AuthResponse;
import com.ichat.authbe.exception.AuthException;
import com.ichat.authbe.model.AuthProvider;
import com.ichat.authbe.model.LinkedIdentity;
import com.ichat.authbe.model.User;
import com.ichat.authbe.repository.LinkedIdentityRepository;
import com.ichat.authbe.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class OAuthService {

    private final UserRepository userRepository;
    private final LinkedIdentityRepository linkedIdentityRepository;
    private final AuthService authService;
    private final RestClient restClient = RestClient.create();

    // Accepts a Google ID token minted for ANY of these client IDs (web /
    // iOS / Android each get their own in Google Cloud Console). Comma-
    // separated. Leave empty/unset during setup — tokens just won't verify
    // until at least one is configured.
    @Value("${app.oauth.google.client-ids:}")
    private String googleClientIdsRaw;

    @Value("${app.oauth.facebook.app-id:}")
    private String facebookAppId;

    @Value("${app.oauth.facebook.app-secret:}")
    private String facebookAppSecret;

    public OAuthService(UserRepository userRepository,
                         LinkedIdentityRepository linkedIdentityRepository,
                         AuthService authService) {
        this.userRepository = userRepository;
        this.linkedIdentityRepository = linkedIdentityRepository;
        this.authService = authService;
    }

    public AuthResponse loginWithGoogle(String idToken, String appId) {
        Map<String, Object> claims;
        try {
            claims = restClient.get()
                    .uri("https://oauth2.googleapis.com/tokeninfo?id_token={token}", idToken)
                    .retrieve()
                    .body(Map.class);
        } catch (RestClientException e) {
            throw new AuthException("Google sign-in failed: invalid or expired token");
        }
        if (claims == null) {
            throw new AuthException("Google sign-in failed: invalid or expired token");
        }

        String aud = String.valueOf(claims.get("aud"));
        List<String> allowedClientIds = Arrays.stream(googleClientIdsRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        if (allowedClientIds.isEmpty()) {
            throw new AuthException(
                    "Google sign-in is not configured yet (app.oauth.google.client-ids is empty)");
        }
        if (!allowedClientIds.contains(aud)) {
            throw new AuthException("Google sign-in failed: token was not issued for this app");
        }

        String providerId = String.valueOf(claims.get("sub"));
        String email = (String) claims.get("email");
        String givenName = (String) claims.getOrDefault("given_name", "");
        String familyName = (String) claims.getOrDefault("family_name", "");

        User user = resolveOrCreateUser(AuthProvider.GOOGLE, providerId, email, givenName, familyName);
        return authService.issueLoginResponse(user, appId);
    }

    public AuthResponse loginWithFacebook(String accessToken, String appId) {
        if (facebookAppId.isBlank() || facebookAppSecret.isBlank()) {
            throw new AuthException(
                    "Facebook sign-in is not configured yet (app.oauth.facebook.app-id / app-secret are empty)");
        }

        Map<String, Object> debugResponse;
        try {
            debugResponse = restClient.get()
                    .uri("https://graph.facebook.com/debug_token?input_token={token}&access_token={appToken}",
                            accessToken, facebookAppId + "|" + facebookAppSecret)
                    .retrieve()
                    .body(Map.class);
        } catch (RestClientException e) {
            throw new AuthException("Facebook sign-in failed: invalid or expired token");
        }

        Map<String, Object> data = debugResponse == null ? null : (Map<String, Object>) debugResponse.get("data");
        boolean valid = data != null && Boolean.TRUE.equals(data.get("is_valid"));
        boolean appMatches = data != null && facebookAppId.equals(String.valueOf(data.get("app_id")));
        if (!valid || !appMatches) {
            throw new AuthException("Facebook sign-in failed: token is invalid or was issued for a different app");
        }

        Map<String, Object> profile;
        try {
            profile = restClient.get()
                    .uri("https://graph.facebook.com/me?fields=id,email,first_name,last_name&access_token={token}",
                            accessToken)
                    .retrieve()
                    .body(Map.class);
        } catch (RestClientException e) {
            throw new AuthException("Facebook sign-in failed: could not fetch profile");
        }
        if (profile == null) {
            throw new AuthException("Facebook sign-in failed: could not fetch profile");
        }

        String providerId = String.valueOf(profile.get("id"));
        String email = (String) profile.get("email");
        String firstName = (String) profile.getOrDefault("first_name", "");
        String lastName = (String) profile.getOrDefault("last_name", "");

        User user = resolveOrCreateUser(AuthProvider.FACEBOOK, providerId, email, firstName, lastName);
        return authService.issueLoginResponse(user, appId);
    }

    /**
     * Core auto-link rule: if this provider+providerId was seen before, use
     * that account. Otherwise, if the provider gave us an email that matches
     * an existing account, link this identity to it. Otherwise, create a
     * brand-new account (same default trial subscription as normal
     * registration) and link this identity to it.
     */
    private User resolveOrCreateUser(AuthProvider provider, String providerId, String email,
                                      String firstName, String lastName) {
        var existingLink = linkedIdentityRepository.findByProviderAndProviderId(provider, providerId);
        if (existingLink.isPresent()) {
            return existingLink.get().getUser();
        }

        User user;
        if (email != null && !email.isBlank() && userRepository.findByEmail(email).isPresent()) {
            user = userRepository.findByEmail(email).get();
        } else {
            user = createUserForSocialSignUp(email, firstName, lastName);
        }

        LinkedIdentity link = new LinkedIdentity(provider, providerId, user);
        linkedIdentityRepository.save(link);

        return user;
    }

    private User createUserForSocialSignUp(String email, String firstName, String lastName) {
        String baseUsername = (email != null && email.contains("@"))
                ? email.substring(0, email.indexOf('@'))
                : ((firstName + lastName).isBlank() ? "user" : firstName + lastName);
        baseUsername = baseUsername.toLowerCase().replaceAll("[^a-z0-9_]", "");
        if (baseUsername.length() < 3) baseUsername = (baseUsername + "user").substring(0, 3);

        String username = baseUsername;
        int suffix = 1;
        while (userRepository.existsByUsername(username)) {
            username = baseUsername + suffix;
            suffix++;
        }

        User user = new User(
                username,
                firstName == null || firstName.isBlank() ? "Unknown" : firstName,
                lastName == null || lastName.isBlank() ? "Unknown" : lastName,
                null, // no password — social-only account until/unless they set one
                null, // birth year not provided by social providers
                "",   // phone number not provided by social providers
                email,
                LocalDate.now().plusDays(authService.getDefaultTrialDays())
        );
        userRepository.save(user);
        return user;
    }
}
