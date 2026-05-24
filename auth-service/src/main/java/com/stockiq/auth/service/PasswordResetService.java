package com.stockiq.auth.service;

import com.stockiq.auth.entity.PasswordResetToken;
import com.stockiq.auth.exception.ResourceNotFoundException;
import com.stockiq.auth.repository.PasswordResetTokenRepository;
import com.stockiq.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public String createResetToken(String email) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("No account found with email: " + email));

        String token = UUID.randomUUID().toString();
        var resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .build();

        tokenRepository.save(resetToken);
        log.info("Password reset token created for user: {}", email);
        // In production: send email via notification-service
        return token;
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        var resetToken = tokenRepository.findByTokenAndUsedFalse(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired reset token"));

        if (resetToken.isExpired()) {
            throw new IllegalArgumentException("Reset token has expired");
        }

        var user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);
        log.info("Password reset successful for user: {}", user.getEmail());
    }
}
