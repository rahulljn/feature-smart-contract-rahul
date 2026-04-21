package com.geojit.contractnote.config;

import com.geojit.contractnote.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:3001}")
    private String allowedOriginsRaw;

    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/v1/auth/login",      // explicit — only login is public (not all /auth/**)
            "/api/v1/auth/logout",     // logout may be called without valid token to be safe
            "/actuator/health",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/api-docs/**",
            "/api/v1/webhooks/**",     // SNS webhooks — verified by AWS message type header
            "/api/v1/pipeline/status-event"  // StatusConsumer Lambda — verified by X-Internal-Key header
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // ─── Disable sessions (stateless JWT) ────────────────────────
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // ─── CORS ─────────────────────────────────────────────────────
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // ─── HTTP Security Headers ────────────────────────────────────
            .headers(headers -> {
                // Prevent clickjacking
                headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::deny);
                // Force HTTPS in prod (ignored in local HTTP)
                headers.httpStrictTransportSecurity(hsts -> hsts
                        .maxAgeInSeconds(31536000)
                        .includeSubDomains(true));
                // Referrer policy
                headers.referrerPolicy(rp -> rp.policy(
                        ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN));
                // Permissions policy
                headers.permissionsPolicy(pp -> pp.policy(
                        "camera=(), microphone=(), geolocation=(), payment=()"));
                // Content Security Policy
                headers.contentSecurityPolicy(csp -> csp.policyDirectives(
                        "default-src 'self'; " +
                        "script-src 'self'; " +
                        "style-src 'self' 'unsafe-inline'; " +
                        "img-src 'self' data:; " +
                        "frame-ancestors 'none';"));
            })

            // ─── Authorization rules ──────────────────────────────────────
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                .requestMatchers("/api/v1/users/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/audit/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )

            // ─── JWT filter ───────────────────────────────────────────────
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        List<String> origins = Arrays.asList(allowedOriginsRaw.split(","));

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of(
                "Authorization", "Content-Type", "Accept",
                "X-Requested-With", "X-Amz-Sns-Message-Type", "X-Internal-Key"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
