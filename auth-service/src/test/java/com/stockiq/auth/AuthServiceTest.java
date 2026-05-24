package com.stockiq.auth;

import com.stockiq.auth.dto.AuthDtos.*;
import com.stockiq.auth.entity.User;
import com.stockiq.auth.repository.UserRepository;
import com.stockiq.auth.security.JwtService;
import com.stockiq.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @InjectMocks AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@stockiq.com")
                .username("testuser")
                .passwordHash("$2a$12$hashedpassword")
                .role(User.Role.ANALYST)
                .enabled(true)
                .build();
    }

    @Test
    @DisplayName("register — success creates user and returns tokens")
    void register_success() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$hashed");
        when(userRepository.save(any())).thenReturn(testUser);
        when(jwtService.generateAccessToken(any())).thenReturn("access.token.here");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh.token.here");
        when(jwtService.getAccessTokenExpiry()).thenReturn(900000L);

        AuthResponse response = authService.register(
                new RegisterRequest("test@stockiq.com", "testuser", "Password1!"));

        assertThat(response.accessToken()).isEqualTo("access.token.here");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("register — duplicate email throws IllegalArgumentException")
    void register_duplicateEmail_throws() {
        when(userRepository.existsByEmail("test@stockiq.com")).thenReturn(true);
        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("test@stockiq.com", "testuser", "Password1!")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already registered");
    }

    @Test
    @DisplayName("register — duplicate username throws IllegalArgumentException")
    void register_duplicateUsername_throws() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername("testuser")).thenReturn(true);
        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("new@stockiq.com", "testuser", "Password1!")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username already taken");
    }

    @Test
    @DisplayName("login — wrong password throws BadCredentialsException")
    void login_wrongPassword_throws() {
        when(userRepository.findByIdentifier(anyString()))
                .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(
                new LoginRequest("testuser", "wrongpassword")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("login — unknown user throws BadCredentialsException")
    void login_unknownUser_throws() {
        when(userRepository.findByIdentifier(anyString()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(
                new LoginRequest("nobody", "Password1!")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("login — disabled account throws BadCredentialsException")
    void login_disabledAccount_throws() {
        testUser.setEnabled(false);
        when(userRepository.findByIdentifier(anyString()))
                .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        assertThatThrownBy(() -> authService.login(
                new LoginRequest("testuser", "Password1!")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("disabled");
    }

    @Test
    @DisplayName("login — success returns auth response with user info")
    void login_success() {
        when(userRepository.findByIdentifier(anyString()))
                .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtService.generateAccessToken(any())).thenReturn("access.token");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh.token");
        when(jwtService.getAccessTokenExpiry()).thenReturn(900000L);

        AuthResponse response = authService.login(
                new LoginRequest("testuser", "Password1!"));

        assertThat(response).isNotNull();
        assertThat(response.user().username()).isEqualTo("testuser");
        assertThat(response.user().role()).isEqualTo("ANALYST");
    }
}
