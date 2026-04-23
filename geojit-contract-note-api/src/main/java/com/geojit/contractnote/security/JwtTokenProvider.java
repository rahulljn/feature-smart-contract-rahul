package com.geojit.contractnote.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.Scanner;

@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${app.jwt.private-key-path}")
    private Resource privateKeyResource;

    @Value("${app.jwt.public-key-path}")
    private Resource publicKeyResource;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    @Value("${app.jwt.issuer}")
    private String issuer;

    private PrivateKey privateKey;
    private PublicKey publicKey;

    @PostConstruct
    public void init() {
        try {
            this.privateKey = loadPrivateKey(privateKeyResource);
            this.publicKey  = loadPublicKey(publicKeyResource);
            log.info("JWT RSA keys loaded successfully");
        } catch (Exception e) {
            log.warn("JWT key files not found — generating in-memory RSA keypair for local dev. Tokens will reset on restart.");
            try {
                KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
                kpg.initialize(2048);
                KeyPair kp = kpg.generateKeyPair();
                this.privateKey = kp.getPrivate();
                this.publicKey  = kp.getPublic();
            } catch (Exception ex) {
                throw new RuntimeException("JWT key initialization failed", ex);
            }
        }
    }

    public String generateToken(String email, String role, String userId) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(email)
                .issuer(issuer)
                .issuedAt(now)
                .expiration(expiry)
                .claim("role", role)
                .claim("userId", userId)
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(publicKey).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    public String getEmailFromToken(String token) {
        return getClaims(token).getSubject();
    }

    public String getRoleFromToken(String token) {
        return (String) getClaims(token).get("role");
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // ─── Key loaders ─────────────────────────────────────────────────
    private PrivateKey loadPrivateKey(Resource resource) throws Exception {
        String pem = readPem(resource)
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(pem);
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decoded));
    }

    private PublicKey loadPublicKey(Resource resource) throws Exception {
        String pem = readPem(resource)
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(pem);
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decoded));
    }

    private String readPem(Resource resource) throws Exception {
        try (Scanner scanner = new Scanner(resource.getInputStream(), StandardCharsets.UTF_8)) {
            return scanner.useDelimiter("\\A").next();
        }
    }
}
