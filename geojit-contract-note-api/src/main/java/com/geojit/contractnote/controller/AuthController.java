package com.geojit.contractnote.controller;

import com.geojit.contractnote.dto.request.LoginRequest;
import com.geojit.contractnote.dto.response.*;
import com.geojit.contractnote.entity.AuditLog;
import com.geojit.contractnote.entity.User;
import com.geojit.contractnote.repository.UserRepository;
import com.geojit.contractnote.service.AuditService;
import com.geojit.contractnote.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication endpoints.
 *
 * JWT tokens are stateless — true server-side invalidation requires a token blacklist
 * (e.g. Redis SET with TTL = remaining token lifetime). Since this deployment uses
 * only PostgreSQL locally, logout is advisory: the client must discard the token.
 * A production upgrade path: store a "jti" (JWT ID) claim and add a DB/Redis deny-list.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService    authService;
    private final AuditService   auditService;
    private final UserRepository userRepository;

    @PostMapping("/login")
    @Operation(summary = "Login with email + password — returns JWT Bearer token")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.ok("Login successful",
                authService.login(request, httpRequest)));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout — instructs client to discard token",
               security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpRequest) {

        if (userDetails != null) {
            userRepository.findByEmail(userDetails.getUsername()).ifPresent(user ->
                    auditService.log(user, AuditLog.AuditAction.LOGOUT, "auth", null, httpRequest));
        }
        // Client must delete the token on its side.
        // Future: add jti to deny-list in Redis with remaining TTL.
        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully", null));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user info",
               security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<ApiResponse<AuthResponse>> me(
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        AuthResponse response = AuthResponse.builder()
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole().name())
                .userId(user.getUserId().toString())
                .build();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
