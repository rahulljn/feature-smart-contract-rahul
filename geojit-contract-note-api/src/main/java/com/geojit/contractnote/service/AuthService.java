package com.geojit.contractnote.service;

import com.geojit.contractnote.dto.request.LoginRequest;
import com.geojit.contractnote.dto.response.AuthResponse;
import com.geojit.contractnote.entity.AuditLog;
import com.geojit.contractnote.entity.User;
import com.geojit.contractnote.exception.ResourceNotFoundException;
import com.geojit.contractnote.repository.UserRepository;
import com.geojit.contractnote.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider      jwtTokenProvider;
    private final UserRepository        userRepository;
    private final AuditService          auditService;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        String token = jwtTokenProvider.generateToken(
                user.getEmail(),
                user.getRole().name(),
                user.getUserId().toString()
        );

        auditService.log(user, AuditLog.AuditAction.LOGIN, "auth", null, httpRequest);
        log.info("User logged in | email={} | role={}", user.getEmail(), user.getRole());

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(expirationMs / 1000)
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole().name())
                .userId(user.getUserId().toString())
                .build();
    }
}
