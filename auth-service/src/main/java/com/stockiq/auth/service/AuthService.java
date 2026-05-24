package com.stockiq.auth.service;

import com.stockiq.auth.dto.AuthDtos.*;
import com.stockiq.auth.entity.User;
import com.stockiq.auth.repository.UserRepository;
import com.stockiq.auth.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new IllegalArgumentException("Email already registered");
        }
        if (userRepository.existsByUsername(req.username())) {
            throw new IllegalArgumentException("Username already taken");
        }

        User user = User.builder()
                .email(req.email())
                .username(req.username())
                .passwordHash(passwordEncoder.encode(req.password()))
                .role(User.Role.ANALYST)
                .enabled(true)
                .build();

        user = userRepository.save(user);
        log.info("Registered new user: {}", user.getEmail());
        return buildAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        User user = userRepository
                .findByEmailOrUsername(req.usernameOrEmail(), req.usernameOrEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }
        if (!user.isEnabled()) {
            throw new BadCredentialsException("Account disabled");
        }

        log.info("User logged in: {}", user.getEmail());
        return buildAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse refresh(RefreshRequest req) {
        String userId = jwtService.extractSubject(req.refreshToken());
        User user = userRepository.findById(java.util.UUID.fromString(userId))
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        return new AuthResponse(
                jwtService.generateAccessToken(user),
                jwtService.generateRefreshToken(user),
                "Bearer",
                jwtService.getAccessTokenExpiry() / 1000,
                new UserInfo(user.getId().toString(), user.getEmail(), user.getUsername(), user.getRole().name())
        );
    }
}
