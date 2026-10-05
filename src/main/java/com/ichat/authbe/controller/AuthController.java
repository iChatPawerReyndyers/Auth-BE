package com.ichat.authbe.controller;

import com.ichat.authbe.dto.AuthResponse;
import com.ichat.authbe.dto.CompleteProfileRequest;
import com.ichat.authbe.dto.FacebookLoginRequest;
import com.ichat.authbe.dto.GoogleLoginRequest;
import com.ichat.authbe.dto.LoginRequest;
import com.ichat.authbe.dto.RegisterRequest;
import com.ichat.authbe.service.AuthService;
import com.ichat.authbe.service.OAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "Registration, password login, and Google/Facebook login")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final OAuthService oAuthService;

    public AuthController(AuthService authService, OAuthService oAuthService) {
        this.authService = authService;
        this.oAuthService = oAuthService;
    }

    @Operation(summary = "Register a new account",
            description = "Creates a user with a password. Optional email enables later auto-linking to a Google/Facebook sign-in. "
                    + "A 30-day trial subscription is granted automatically unless subscriptionExpiresAt is provided.")
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @Operation(summary = "Log in with username + password",
            description = "appId identifies the calling app (e.g. \"stickies\"). Apps on app.clients.free-ids skip the "
                    + "subscription check entirely; every other app ID requires subscriptionExpiresAt to be today or later.")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(summary = "Log in or sign up with Google",
            description = "Accepts a Google ID token obtained client-side. Verifies it server-side, then logs into a "
                    + "previously-linked account, auto-links by matching email, or creates a new account. appId decides "
                    + "whether the subscription gate applies, same as /login.")
    @PostMapping("/oauth/google")
    public ResponseEntity<AuthResponse> loginWithGoogle(@Valid @RequestBody GoogleLoginRequest request) {
        return ResponseEntity.ok(oAuthService.loginWithGoogle(request.getIdToken(), request.getAppId()));
    }

    @Operation(summary = "Log in or sign up with Facebook",
            description = "Accepts a Facebook access token obtained client-side. Verifies it server-side, then logs into a "
                    + "previously-linked account, auto-links by matching email, or creates a new account. appId decides "
                    + "whether the subscription gate applies, same as /login.")
    @PostMapping("/oauth/facebook")
    public ResponseEntity<AuthResponse> loginWithFacebook(@Valid @RequestBody FacebookLoginRequest request) {
        return ResponseEntity.ok(oAuthService.loginWithFacebook(request.getAccessToken(), request.getAppId()));
    }

    @Operation(summary = "Complete a profile missing birthYear/phoneNumber",
            description = "Used after a Google/Facebook sign-up, which doesn't provide these required fields. "
                    + "The frontend shows a blocking modal for this whenever a login response has profileComplete: false.")
    @PostMapping("/profile/complete")
    public ResponseEntity<AuthResponse> completeProfile(@Valid @RequestBody CompleteProfileRequest request) {
        return ResponseEntity.ok(authService.completeProfile(
            request.getUsername(), request.getBirthYear(), request.getPhoneNumber(), request.getAppId()
        ));
    }
}
