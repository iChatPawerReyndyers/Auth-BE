package com.ichat.authbe.service;

import com.ichat.authbe.dto.AuthResponse;
import com.ichat.authbe.exception.AuthException;
import com.ichat.authbe.model.PasswordResetOtp;
import com.ichat.authbe.model.User;
import com.ichat.authbe.repository.PasswordResetOtpRepository;
import com.ichat.authbe.repository.UserRepository;
import com.ichat.authbe.service.sms.SmsSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetOtpRepository otpRepository;
    private final PasswordEncoder passwordEncoder;
    private final SmsSender smsSender;
    private final SecureRandom random = new SecureRandom();

    @Value("${app.otp.expiry-minutes:5}")
    private int otpExpiryMinutes;

    public PasswordResetService(UserRepository userRepository,
                                 PasswordResetOtpRepository otpRepository,
                                 PasswordEncoder passwordEncoder,
                                 SmsSender smsSender) {
        this.userRepository = userRepository;
        this.otpRepository = otpRepository;
        this.passwordEncoder = passwordEncoder;
        this.smsSender = smsSender;
    }

    /**
     * Always returns a generic success message, even for an unknown
     * username — so this endpoint can't be used to enumerate which
     * usernames exist.
     */
    public AuthResponse requestReset(String username) {
        String genericMessage = "If an account with that username exists, a verification code has been sent to its registered mobile number.";

        userRepository.findByUsername(username).ifPresent(user -> {
            if (user.getPhoneNumber() == null || user.getPhoneNumber().isBlank()) {
                // No phone on file (e.g. a social-only account) — nothing to send to.
                // Stay silent here too, for the same enumeration-safety reason.
                return;
            }

            String otp = generateOtp();
            PasswordResetOtp record = new PasswordResetOtp(
                    user,
                    passwordEncoder.encode(otp),
                    LocalDateTime.now().plusMinutes(otpExpiryMinutes)
            );
            otpRepository.save(record);

            smsSender.send(
                    user.getPhoneNumber(),
                    "Your verification code is " + otp + ". It expires in " + otpExpiryMinutes + " minutes."
            );
        });

        return new AuthResponse(true, genericMessage, null);
    }

    public AuthResponse resetPassword(String username, String otp, String newPassword, String confirmNewPassword) {
        if (!newPassword.equals(confirmNewPassword)) {
            throw new AuthException("New password and confirm password do not match");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AuthException("Invalid or expired code"));

        List<PasswordResetOtp> candidates = otpRepository.findByUserAndUsedFalseOrderByCreatedAtDesc(user);

        PasswordResetOtp match = candidates.stream()
                .filter(record -> record.getExpiresAt().isAfter(LocalDateTime.now()))
                .filter(record -> passwordEncoder.matches(otp, record.getOtpHash()))
                .findFirst()
                .orElseThrow(() -> new AuthException("Invalid or expired code"));

        match.setUsed(true);
        otpRepository.save(match);

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        return new AuthResponse(true, "Password reset successful. You can now log in with your new password.", user.getUsername());
    }

    private String generateOtp() {
        int code = 100000 + random.nextInt(900000); // always 6 digits
        return String.valueOf(code);
    }
}
