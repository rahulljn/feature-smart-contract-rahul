package com.geojit.contractnote.controller;

import com.geojit.contractnote.dto.request.SuppressionRequest;
import com.geojit.contractnote.dto.response.ApiResponse;
import com.geojit.contractnote.dto.response.PageResponse;
import com.geojit.contractnote.entity.*;
import com.geojit.contractnote.repository.UserRepository;
import com.geojit.contractnote.service.SuppressionService;
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

@RestController
@RequestMapping("/api/v1/suppression")
@Tag(name = "Suppression List")
@RequiredArgsConstructor
public class SuppressionController {

    private final SuppressionService suppressionService;
    private final UserRepository     userRepository;

    @GetMapping
    @Operation(summary = "List all suppressed emails")
    public ResponseEntity<ApiResponse<PageResponse<SuppressionList>>> getAll(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(suppressionService.getAll(pageable))));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPS_MANAGER')")
    @Operation(summary = "Add email to suppression list")
    public ResponseEntity<ApiResponse<SuppressionList>> add(
            @Valid @RequestBody SuppressionRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        SuppressionList entry = suppressionService.add(req, user.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Email suppressed", entry));
    }

    @DeleteMapping("/{email}")
    @PreAuthorize("hasAnyRole('ADMIN','OPS_MANAGER')")
    @Operation(summary = "Remove email from suppression list")
    public ResponseEntity<ApiResponse<Void>> remove(@PathVariable String email) {
        suppressionService.remove(email);
        return ResponseEntity.ok(ApiResponse.ok("Email removed from suppression", null));
    }

    @PostMapping("/push-aws")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Push all local suppression entries to AWS SES account suppression list")
    public ResponseEntity<ApiResponse<String>> pushToAws() {
        int count = suppressionService.pushToAws();
        return ResponseEntity.ok(ApiResponse.ok(
                count + " local entries pushed to AWS SES account suppression list",
                String.valueOf(count)));
    }

    @PostMapping("/sync-aws")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Sync suppression list from AWS SES account suppression list")
    public ResponseEntity<ApiResponse<String>> syncFromAws() {
        int count = suppressionService.syncFromAws();
        return ResponseEntity.ok(ApiResponse.ok(
                count > 0 ? count + " new entries synced from AWS SES" : "Already in sync — no new entries found",
                String.valueOf(count)));
    }
}
