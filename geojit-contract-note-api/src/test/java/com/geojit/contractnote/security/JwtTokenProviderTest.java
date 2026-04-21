package com.geojit.contractnote.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for JWT token generation and validation.
 *
 * Generates a fresh RSA key pair in-memory for each test run so no file I/O needed.
 * Uses ReflectionTestUtils to inject the keys without Spring context overhead.
 */
class JwtTokenProviderTest {

    private JwtTokenProvider provider;

    private static final String TEST_EMAIL = "test@geojit.com";
    private static final String TEST_ROLE  = "ADMIN";
    private static final String TEST_UID   = "00000000-0000-0000-0000-000000000001";
    private static final long   EXPIRY_MS  = 3_600_000L; // 1 hour

    @BeforeEach
    void setUp() throws Exception {
        provider = new JwtTokenProvider();

        // Generate in-memory RSA key pair (avoids classpath dependency)
        java.security.KeyPairGenerator kpg = java.security.KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        java.security.KeyPair kp = kpg.generateKeyPair();

        // Inject via reflection
        ReflectionTestUtils.setField(provider, "privateKey",   kp.getPrivate());
        ReflectionTestUtils.setField(provider, "publicKey",    kp.getPublic());
        ReflectionTestUtils.setField(provider, "expirationMs", EXPIRY_MS);
        ReflectionTestUtils.setField(provider, "issuer",       "geojit-test");
    }

    @Test
    @DisplayName("generateToken returns a non-blank JWT string")
    void generateToken_returnsNonBlankJwt() {
        String token = provider.generateToken(TEST_EMAIL, TEST_ROLE, TEST_UID);
        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3); // header.payload.signature
    }

    @Test
    @DisplayName("validateToken returns true for a fresh token")
    void validateToken_freshToken_returnsTrue() {
        String token = provider.generateToken(TEST_EMAIL, TEST_ROLE, TEST_UID);
        assertThat(provider.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("validateToken returns false for a tampered token")
    void validateToken_tamperedToken_returnsFalse() {
        String token = provider.generateToken(TEST_EMAIL, TEST_ROLE, TEST_UID);
        String tampered = token.substring(0, token.length() - 4) + "XXXX";
        assertThat(provider.validateToken(tampered)).isFalse();
    }

    @Test
    @DisplayName("getEmailFromToken extracts correct email")
    void getEmailFromToken_returnsCorrectEmail() {
        String token = provider.generateToken(TEST_EMAIL, TEST_ROLE, TEST_UID);
        assertThat(provider.getEmailFromToken(token)).isEqualTo(TEST_EMAIL);
    }

    @Test
    @DisplayName("getRoleFromToken extracts correct role")
    void getRoleFromToken_returnsCorrectRole() {
        String token = provider.generateToken(TEST_EMAIL, TEST_ROLE, TEST_UID);
        assertThat(provider.getRoleFromToken(token)).isEqualTo(TEST_ROLE);
    }

    @Test
    @DisplayName("Expired token fails validation")
    void validateToken_expiredToken_returnsFalse() throws Exception {
        // Set expiry to -1 ms (already expired)
        ReflectionTestUtils.setField(provider, "expirationMs", -1L);
        String token = provider.generateToken(TEST_EMAIL, TEST_ROLE, TEST_UID);
        assertThat(provider.validateToken(token)).isFalse();
    }
}
