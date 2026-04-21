package com.geojit.contractnote.service;

import com.geojit.contractnote.dto.request.LoginRequest;
import com.geojit.contractnote.dto.response.AuthResponse;
import com.geojit.contractnote.entity.User;
import com.geojit.contractnote.exception.ResourceNotFoundException;
import com.geojit.contractnote.repository.UserRepository;
import com.geojit.contractnote.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock AuthenticationManager authenticationManager;
    @Mock JwtTokenProvider      jwtTokenProvider;
    @Mock UserRepository        userRepository;
    @Mock AuditService          auditService;
    @Mock HttpServletRequest    httpRequest;

    @InjectMocks AuthService authService;

    private User testUser;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "expirationMs", 3_600_000L);

        testUser = User.builder()
                .userId(UUID.randomUUID())
                .email("admin@geojit.com")
                .name("Admin User")
                .role(User.Role.ADMIN)
                .password("$2a$12$hashedpassword")
                .isActive(true)
                .build();

        loginRequest = new LoginRequest();
        loginRequest.setEmail("admin@geojit.com");
        loginRequest.setPassword("Admin@123");
    }

    @Test
    @DisplayName("login() returns AuthResponse with token on valid credentials")
    void login_validCredentials_returnsAuthResponse() {
        given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .willReturn(mock(org.springframework.security.core.Authentication.class));
        given(userRepository.findByEmail("admin@geojit.com"))
                .willReturn(Optional.of(testUser));
        given(jwtTokenProvider.generateToken(
                testUser.getEmail(), testUser.getRole().name(), testUser.getUserId().toString()))
                .willReturn("mocked.jwt.token");

        AuthResponse response = authService.login(loginRequest, httpRequest);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("mocked.jwt.token");
        assertThat(response.getEmail()).isEqualTo("admin@geojit.com");
        assertThat(response.getRole()).isEqualTo("ADMIN");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        then(userRepository).should().save(testUser); // lastLogin updated
    }

    @Test
    @DisplayName("login() throws ResourceNotFoundException when user not in DB despite auth success")
    void login_userNotFound_throwsResourceNotFoundException() {
        given(authenticationManager.authenticate(any()))
                .willReturn(mock(org.springframework.security.core.Authentication.class));
        given(userRepository.findByEmail("admin@geojit.com"))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(loginRequest, httpRequest))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("login() propagates BadCredentialsException from AuthenticationManager")
    void login_badCredentials_propagatesBadCredentials() {
        given(authenticationManager.authenticate(any()))
                .willThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(loginRequest, httpRequest))
                .isInstanceOf(BadCredentialsException.class);
    }
}
