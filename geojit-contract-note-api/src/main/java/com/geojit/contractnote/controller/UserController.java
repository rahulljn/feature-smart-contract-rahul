package com.geojit.contractnote.controller;

import com.geojit.contractnote.dto.request.UserRequest;
import com.geojit.contractnote.dto.response.*;
import com.geojit.contractnote.entity.*;
import com.geojit.contractnote.repository.UserRepository;
import com.geojit.contractnote.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class UserController {

    private final UserService     userService;
    private final UserRepository  userRepository;

    @GetMapping
    @Operation(summary = "List all users")
    public ResponseEntity<ApiResponse<PageResponse<User>>> getAll(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        User caller = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return ResponseEntity.ok(ApiResponse.ok(
                PageResponse.from(userService.getAll(caller, PageRequest.of(page, size)))));
    }

    @PostMapping
    @Operation(summary = "Create a new user")
    public ResponseEntity<ApiResponse<User>> create(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UserRequest req) {
        User caller = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("User created", userService.create(req, caller)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user")
    public ResponseEntity<ApiResponse<User>> update(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id,
            @Valid @RequestBody UserRequest req) {
        User caller = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return ResponseEntity.ok(ApiResponse.ok("User updated", userService.update(id, req, caller)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate user")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable UUID id) {
        userService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.ok("User deactivated", null));
    }
}
