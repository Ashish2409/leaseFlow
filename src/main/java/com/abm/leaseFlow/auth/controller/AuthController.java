package com.abm.leaseFlow.auth.controller;

import com.abm.leaseFlow.auth.dto.request.*;
import com.abm.leaseFlow.auth.dto.response.*;
import com.abm.leaseFlow.auth.service.AuthService;
import com.abm.leaseFlow.common.dto.ApiResponse;
import com.abm.leaseFlow.common.security.TenantContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Tenant registration, user management, and token operations")
public class AuthController {

    private final AuthService authService;

    // ── POST /api/v1/auth/register/tenant ─────────────────────────────────

    @PostMapping("/register/tenant")
    @Operation(summary = "Register a new tenant (property management company) with an admin user")
    public ResponseEntity<ApiResponse<TenantRegistrationResponse>> registerTenant(
            @Valid @RequestBody RegisterTenantRequest request) {

        TenantRegistrationResponse response = authService.registerTenant(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response));
    }

    // ── POST /api/v1/auth/register/user ───────────────────────────────────

    @PostMapping("/register/user")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','PROPERTY_MANAGER')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Register a new user within the current tenant (requires PLATFORM_ADMIN or PROPERTY_MANAGER)")
    public ResponseEntity<ApiResponse<UserResponse>> registerUser(
            @Valid @RequestBody RegisterUserRequest request,
            @RequestAttribute(name = "tenantId", required = false) UUID tenantId,
            @RequestAttribute(name = "userId",   required = false) UUID userId,
            org.springframework.security.core.Authentication authentication) {

        UUID currentTenantId = extractTenantId(authentication);
        UUID currentUserId   = extractUserId(authentication);

        UserResponse response = authService.registerUser(request, currentTenantId, currentUserId);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response));
    }

    // ── POST /api/v1/auth/login ───────────────────────────────────────────

    @PostMapping("/login")
    @Operation(summary = "Authenticate and receive JWT access + refresh tokens")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        String ip = resolveClientIp(httpRequest);
        AuthResponse response = authService.login(request, ip);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // ── POST /api/v1/auth/refresh ─────────────────────────────────────────

    @PostMapping("/refresh")
    @Operation(summary = "Exchange a valid refresh token for a new access + refresh token pair")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {

        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // ── POST /api/v1/auth/logout ──────────────────────────────────────────

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Revoke the current refresh token (idempotent)")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody RefreshTokenRequest request,
            org.springframework.security.core.Authentication authentication) {

        UUID userId = extractUserId(authentication);
        authService.logout(request, userId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private UUID extractUserId(org.springframework.security.core.Authentication auth) {
        return (UUID) auth.getPrincipal();
    }

    private UUID extractTenantId(org.springframework.security.core.Authentication auth) {
        // TenantContextHolder is populated by JwtAuthenticationFilter for the current thread
        return TenantContextHolder.get();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
