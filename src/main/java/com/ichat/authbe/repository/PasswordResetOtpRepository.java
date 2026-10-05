package com.ichat.authbe.repository;

import com.ichat.authbe.model.PasswordResetOtp;
import com.ichat.authbe.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PasswordResetOtpRepository extends JpaRepository<PasswordResetOtp, Long> {
    // Most recent first, so the service can check the latest unused/unexpired code.
    List<PasswordResetOtp> findByUserAndUsedFalseOrderByCreatedAtDesc(User user);
}
