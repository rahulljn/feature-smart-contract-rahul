package com.geojit.contractnote.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configures Swagger UI with:
 *  - JWT Bearer token auth (RS256)
 *  - Server environment labels
 *  - Project metadata
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "BearerAuth";

    @Value("${spring.profiles.active:local}")
    private String activeProfile;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(servers())
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, bearerScheme()));
    }

    private Info apiInfo() {
        return new Info()
                .title("Geojit Smart Contract Note — Back-Office API")
                .version("1.0.0")
                .description("""
                        REST API for the Geojit Smart Contract Note back-office platform.
                        Manages job ingestion, pipeline status tracking, client 360, \
                        suppression, audit, and email operations.

                        **Authentication**: Use `POST /api/v1/auth/login` to obtain a JWT.
                        Click *Authorize* and paste the token (without the `Bearer ` prefix).
                        """)
                .contact(new Contact()
                        .name("ACC Dev")
                        .email("suraj@acc.ltd"))
                .license(new License()
                        .name("Proprietary")
                        .url("https://geojit.com"));
    }

    private List<Server> servers() {
        Server local = new Server()
                .url("http://localhost:8080")
                .description("Local development");
        Server prod = new Server()
                .url("https://api.contractnote.geojit.com")
                .description("Production");
        return List.of(local, prod);
    }

    private SecurityScheme bearerScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Provide the JWT token obtained from POST /api/v1/auth/login");
    }
}
