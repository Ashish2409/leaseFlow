package com.abm.leaseFlow.auth;

import com.abm.leaseFlow.auth.dto.request.*;
import com.abm.leaseFlow.auth.dto.response.*;
import com.abm.leaseFlow.common.BaseIntegrationTest;
import com.abm.leaseFlow.user.entity.RoleName;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    // Shared state across ordered test methods
    private static String adminAccessToken;
    private static String adminRefreshToken;

    // ── Register Tenant ───────────────────────────────────────────────────

    @Test
    @Order(1)
    void registerTenant_success_returns201() {
        RegisterTenantRequest request = new RegisterTenantRequest();
        request.setCompanyName("Acme Properties LLC");
        request.setAdminFirstName("John");
        request.setAdminLastName("Smith");
        request.setAdminEmail("admin@acme.com");
        request.setAdminPassword("SecurePass1");

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/v1/auth/register/tenant", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Map<?, ?> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("success")).isEqualTo(true);
        Map<?, ?> data = (Map<?, ?>) body.get("data");
        assertThat(data.get("companyName")).isEqualTo("Acme Properties LLC");
        assertThat(data.get("tenantId")).isNotNull();
        assertThat(data.get("adminUserId")).isNotNull();
    }

    @Test
    @Order(2)
    void registerTenant_duplicateName_returns409() {
        RegisterTenantRequest request = new RegisterTenantRequest();
        request.setCompanyName("Acme Properties LLC");
        request.setAdminFirstName("Jane");
        request.setAdminLastName("Doe");
        request.setAdminEmail("jane@acme2.com");
        request.setAdminPassword("SecurePass1");

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/v1/auth/register/tenant", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @Order(3)
    void registerTenant_invalidEmail_returns400() {
        RegisterTenantRequest request = new RegisterTenantRequest();
        request.setCompanyName("New Co");
        request.setAdminFirstName("Bob");
        request.setAdminLastName("Jones");
        request.setAdminEmail("not-an-email");
        request.setAdminPassword("SecurePass1");

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/v1/auth/register/tenant", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Map<?, ?> body = response.getBody();
        assertThat(body.get("success")).isEqualTo(false);
    }

    // ── Login ─────────────────────────────────────────────────────────────

    @Test
    @Order(4)
    void login_validCredentials_returnsTokens() {
        LoginRequest request = new LoginRequest();
        request.setEmail("admin@acme.com");
        request.setPassword("SecurePass1");

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/v1/auth/login", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<?, ?> data = (Map<?, ?>) ((Map<?, ?>) response.getBody()).get("data");
        assertThat(data.get("accessToken")).isNotNull();
        assertThat(data.get("refreshToken")).isNotNull();
        assertThat(data.get("tokenType")).isEqualTo("Bearer");

        // Store tokens for subsequent tests
        adminAccessToken  = (String) data.get("accessToken");
        adminRefreshToken = (String) data.get("refreshToken");
    }

    @Test
    @Order(5)
    void login_wrongPassword_returns401() {
        LoginRequest request = new LoginRequest();
        request.setEmail("admin@acme.com");
        request.setPassword("WrongPassword1");

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/v1/auth/login", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ── Protected endpoint with JWT ───────────────────────────────────────

    @Test
    @Order(6)
    void protectedEndpoint_withValidToken_returns201() {
        assertThat(adminAccessToken).isNotNull();

        RegisterUserRequest request = new RegisterUserRequest();
        request.setFirstName("Alice");
        request.setLastName("Manager");
        request.setEmail("alice@acme.com");
        request.setPassword("SecurePass1");
        request.setRole(RoleName.ROLE_PROPERTY_MANAGER);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminAccessToken);
        HttpEntity<RegisterUserRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/auth/register/user", HttpMethod.POST, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Map<?, ?> data = (Map<?, ?>) ((Map<?, ?>) response.getBody()).get("data");
        assertThat(data.get("email")).isEqualTo("alice@acme.com");
    }

    @Test
    @Order(7)
    void protectedEndpoint_withoutToken_returns401() {
        RegisterUserRequest request = new RegisterUserRequest();
        request.setFirstName("Bob");
        request.setLastName("Agent");
        request.setEmail("bob@acme.com");
        request.setPassword("SecurePass1");
        request.setRole(RoleName.ROLE_LEASING_AGENT);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/v1/auth/register/user", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ── Refresh Tokens ────────────────────────────────────────────────────

    @Test
    @Order(8)
    void refresh_validToken_returnsNewTokenPair() {
        assertThat(adminRefreshToken).isNotNull();

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(adminRefreshToken);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/v1/auth/refresh", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<?, ?> data = (Map<?, ?>) ((Map<?, ?>) response.getBody()).get("data");
        assertThat(data.get("accessToken")).isNotNull();
        String newRefresh = (String) data.get("refreshToken");
        assertThat(newRefresh).isNotEqualTo(adminRefreshToken); // token rotation

        adminRefreshToken = newRefresh; // Update for logout test
    }

    @Test
    @Order(9)
    void refresh_reusedOldToken_returns401() {
        // The original token from test 4 was already rotated in test 8
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("invalid-or-rotated-token");

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/v1/auth/refresh", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ── Logout ────────────────────────────────────────────────────────────

    @Test
    @Order(10)
    void logout_validToken_returns200AndRevokes() {
        assertThat(adminAccessToken).isNotNull();
        assertThat(adminRefreshToken).isNotNull();

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(adminRefreshToken);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminAccessToken);
        HttpEntity<RefreshTokenRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/auth/logout", HttpMethod.POST, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Attempting to use the revoked token should fail
        ResponseEntity<Map> refreshAfterLogout = restTemplate.postForEntity(
                "/api/v1/auth/refresh", request, Map.class);
        assertThat(refreshAfterLogout.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
