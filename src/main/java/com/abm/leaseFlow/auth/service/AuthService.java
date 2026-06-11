package com.abm.leaseFlow.auth.service;

import com.abm.leaseFlow.auth.dto.request.*;
import com.abm.leaseFlow.auth.dto.response.*;
import com.abm.leaseFlow.auth.entity.RefreshToken;
import com.abm.leaseFlow.auth.repository.RefreshTokenRepository;
import com.abm.leaseFlow.common.audit.AuditAction;
import com.abm.leaseFlow.common.audit.AuditEvent;
import com.abm.leaseFlow.common.exception.DuplicateResourceException;
import com.abm.leaseFlow.common.exception.InvalidTokenException;
import com.abm.leaseFlow.common.exception.ResourceNotFoundException;
import com.abm.leaseFlow.common.security.JwtProperties;
import com.abm.leaseFlow.common.security.JwtTokenProvider;
import com.abm.leaseFlow.tenant.entity.Tenant;
import com.abm.leaseFlow.tenant.repository.TenantRepository;
import com.abm.leaseFlow.user.entity.Role;
import com.abm.leaseFlow.user.entity.RoleName;
import com.abm.leaseFlow.user.entity.User;
import com.abm.leaseFlow.user.repository.RoleRepository;
import com.abm.leaseFlow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final TenantRepository      tenantRepository;
    private final UserRepository        userRepository;
    private final RoleRepository        roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder       passwordEncoder;
    private final JwtTokenProvider      jwtTokenProvider;
    private final JwtProperties         jwtProperties;
    private final ApplicationEventPublisher eventPublisher;

    // ── Tenant Registration ───────────────────────────────────────────────

    @Transactional
    public TenantRegistrationResponse registerTenant(RegisterTenantRequest request) {
        if (tenantRepository.existsByNameIgnoreCase(request.getCompanyName())) {
            throw new DuplicateResourceException(
                "A company with name '" + request.getCompanyName() + "' already exists");
        }

        // Create tenant
        Tenant tenant = Tenant.builder()
                .name(request.getCompanyName())
                .subscriptionPlan(Tenant.SubscriptionPlan.STARTER)
                .status(Tenant.TenantStatus.ACTIVE)
                .build();
        tenant = tenantRepository.save(tenant);

        // Create admin user
        Role adminRole = requireRole(RoleName.ROLE_PLATFORM_ADMIN);
        User admin = User.builder()
                .tenantId(tenant.getId())
                .email(request.getAdminEmail().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.getAdminPassword()))
                .enabled(true)
                .roles(Set.of(adminRole))
                .build();
        admin = userRepository.save(admin);

        log.info("Tenant registered: tenantId={}, adminUserId={}", tenant.getId(), admin.getId());

        // Publish audit event (fires after commit)
        eventPublisher.publishEvent(AuditEvent.builder()
                .tenantId(tenant.getId())
                .entityType("TENANT")
                .entityId(tenant.getId())
                .action(AuditAction.TENANT_REGISTERED)
                .performedBy(admin.getId())
                .details(Map.of(
                    "companyName", tenant.getName(),
                    "adminEmail",  admin.getEmail()
                ))
                .build());

        return TenantRegistrationResponse.builder()
                .tenantId(tenant.getId())
                .companyName(tenant.getName())
                .adminUserId(admin.getId())
                .adminEmail(admin.getEmail())
                .build();
    }

    // ── User Registration ─────────────────────────────────────────────────

    @Transactional
    public UserResponse registerUser(RegisterUserRequest request, UUID tenantId, UUID registeredBy) {
        if (userRepository.existsByEmailAndTenantId(request.getEmail().toLowerCase(), tenantId)) {
            throw new DuplicateResourceException(
                "A user with email '" + request.getEmail() + "' already exists in this organisation");
        }

        // PLATFORM_ADMIN can only be created during tenant registration
        if (request.getRole() == RoleName.ROLE_PLATFORM_ADMIN) {
            throw new DuplicateResourceException("PLATFORM_ADMIN cannot be created via this endpoint");
        }

        Role role = requireRole(request.getRole());
        User user = User.builder()
                .tenantId(tenantId)
                .email(request.getEmail().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .enabled(true)
                .roles(Set.of(role))
                .build();
        user = userRepository.save(user);

        log.info("User registered: userId={}, role={}, tenantId={}", user.getId(), request.getRole(), tenantId);

        eventPublisher.publishEvent(AuditEvent.builder()
                .tenantId(tenantId)
                .entityType("USER")
                .entityId(user.getId())
                .action(AuditAction.USER_CREATED)
                .performedBy(registeredBy)
                .details(Map.of(
                    "email", user.getEmail(),
                    "role",  request.getRole().name()
                ))
                .build());

        return toUserResponse(user);
    }

    // ── Login ─────────────────────────────────────────────────────────────

    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress) {
        // Resolve tenantId: explicit or by email lookup
        UUID tenantId = resolveTenantId(request);

        User user = userRepository
                .findByEmailAndTenantIdAndDeletedAtIsNull(request.getEmail().toLowerCase(), tenantId)
                .orElseThrow(() -> {
                    publishLoginFailed(request.getEmail(), tenantId, ipAddress, "User not found");
                    return new BadCredentialsException("Invalid email or password");
                });

        if (!user.isActive()) {
            publishLoginFailed(request.getEmail(), tenantId, ipAddress, "Account disabled");
            throw new BadCredentialsException("Account is disabled");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            publishLoginFailed(request.getEmail(), tenantId, ipAddress, "Wrong password");
            throw new BadCredentialsException("Invalid email or password");
        }

        AuthResponse response = issueTokenPair(user);

        log.info("Login success: userId={}, tenantId={}", user.getId(), tenantId);
        eventPublisher.publishEvent(AuditEvent.builder()
                .tenantId(tenantId)
                .entityType("USER")
                .entityId(user.getId())
                .action(AuditAction.LOGIN_SUCCESS)
                .performedBy(user.getId())
                .ipAddress(ipAddress)
                .details(Map.of("email", user.getEmail()))
                .build());

        return response;
    }

    // ── Refresh Token ─────────────────────────────────────────────────────

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String tokenHash = sha256(request.getRefreshToken());

        RefreshToken stored = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidTokenException("Refresh token not found"));

        if (!stored.isValid()) {
            // If revoked — possible token theft. Revoke all tokens for this user.
            if (stored.isRevoked()) {
                refreshTokenRepository.revokeAllByUserId(stored.getUserId());
                log.warn("Revoked refresh token reuse detected for userId={}", stored.getUserId());
            }
            throw new InvalidTokenException("Refresh token is no longer valid");
        }

        User user = userRepository.findByIdAndDeletedAtIsNull(stored.getUserId())
                .orElseThrow(() -> new InvalidTokenException("User not found"));

        // Rotate: revoke old, issue new pair
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        AuthResponse response = issueTokenPair(user);

        eventPublisher.publishEvent(AuditEvent.builder()
                .tenantId(user.getTenantId())
                .entityType("USER")
                .entityId(user.getId())
                .action(AuditAction.TOKEN_REFRESHED)
                .performedBy(user.getId())
                .build());

        return response;
    }

    // ── Logout ────────────────────────────────────────────────────────────

    @Transactional
    public void logout(RefreshTokenRequest request, UUID userId) {
        String tokenHash = sha256(request.getRefreshToken());

        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });

        // Get tenantId from user for audit
        userRepository.findByIdAndDeletedAtIsNull(userId).ifPresent(user ->
            eventPublisher.publishEvent(AuditEvent.builder()
                    .tenantId(user.getTenantId())
                    .entityType("USER")
                    .entityId(userId)
                    .action(AuditAction.LOGOUT)
                    .performedBy(userId)
                    .build())
        );

        log.info("Logout: userId={}", userId);
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private AuthResponse issueTokenPair(User user) {
        List<String> roles = user.getRoles().stream()
                .map(r -> r.getName().name())
                .collect(Collectors.toList());

        String accessToken  = jwtTokenProvider.generateAccessToken(user.getId(), user.getTenantId(), roles);
        String rawRefresh   = jwtTokenProvider.generateRefreshToken();
        String refreshHash  = sha256(rawRefresh);

        RefreshToken token = RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(refreshHash)
                .expiresAt(Instant.now().plusMillis(jwtProperties.getRefreshTokenExpiryMs()))
                .revoked(false)
                .build();
        refreshTokenRepository.save(token);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefresh)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getAccessTokenExpiryMs() / 1000)
                .userId(user.getId())
                .tenantId(user.getTenantId())
                .email(user.getEmail())
                .build();
    }

    private UUID resolveTenantId(LoginRequest request) {
        if (request.getTenantId() != null && !request.getTenantId().isBlank()) {
            return UUID.fromString(request.getTenantId());
        }
        // Without an explicit tenantId, look up by email using the indexed query.
        // Residents and multi-tenant shared emails should always pass tenantId explicitly.
        return userRepository
                .findFirstByEmailAndDeletedAtIsNull(request.getEmail().toLowerCase())
                .map(User::getTenantId)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
    }

    private Role requireRole(RoleName roleName) {
        return roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role", roleName));
    }

    private void publishLoginFailed(String email, UUID tenantId, String ip, String reason) {
        eventPublisher.publishEvent(AuditEvent.builder()
                .tenantId(tenantId)
                .entityType("USER")
                .action(AuditAction.LOGIN_FAILED)
                .ipAddress(ip)
                .details(Map.of("email", email, "reason", reason))
                .build());
    }

    private UserResponse toUserResponse(User user) {
        Set<RoleName> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
        return UserResponse.builder()
                .userId(user.getId())
                .tenantId(user.getTenantId())
                .email(user.getEmail())
                .roles(roleNames)
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .build();
    }

    /** SHA-256 of a raw token for safe DB storage. */
    private String sha256(String raw) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
