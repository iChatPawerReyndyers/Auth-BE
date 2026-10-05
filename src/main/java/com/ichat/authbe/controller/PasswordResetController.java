package com.ichat.authbe.controller;

import com.ichat.authbe.dto.AuthResponse;
import com.ichat.authbe.dto.ForgotPasswordRequest;
import com.ichat.authbe.dto.ResetPasswordRequest;
import com.ichat.authbe.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Password Reset", description = "Forgot/reset password via a 6-digit SMS code")
@RestController
@RequestMapping("/api/auth/password")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    @Operation(summary = "Request a password reset code",
            description = "Always returns a generic success message, whether or not the username exists or has a phone "
                    + "number on file, so this can't be used to enumerate accounts. No real SMS provider is configured "
                    + "by default — the code is logged to the backend console instead.")
    @PostMapping("/forgot")
    public ResponseEntity<AuthResponse> forgot(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(passwordResetService.requestReset(request.getUsername()));
    }

    @Operation(summary = "Complete a password reset",
            description = "Verifies the 6-digit code (correct, unused, not expired) and sets the new password.")
    @PostMapping("/reset")
    public ResponseEntity<AuthResponse> reset(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(passwordResetService.resetPassword(
                request.getUsername(),
                request.getOtp(),
                request.getNewPassword(),
                request.getConfirmNewPassword()
        ));
    }
}
