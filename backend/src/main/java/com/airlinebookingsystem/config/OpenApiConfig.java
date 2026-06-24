package com.airlinebookingsystem.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 3 configuration for Swagger UI.
 *
 * Swagger UI available at: http://localhost:8080/swagger-ui/index.html
 * OpenAPI JSON spec at: http://localhost:8080/v3/api-docs
 *
 * All protected endpoints show a padlock icon in the UI.
 * Click "Authorise" and enter: Bearer <your-jwt-token>
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local development server")))
                // Registers the JWT Bearer scheme globally
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, jwtSecurityScheme()))
                // Applies Bearer auth requirement to all endpoints by default
                // Individual public endpoints override this via @SecurityRequirements({})
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
    }

    private Info apiInfo() {
        return new Info()
                .title("Airline Booking System API")
                .description("""
                        REST API for the Airline Booking System.

                        ## Authentication
                        1. Register at `POST /api/v1/auth/register`
                        2. Login at `POST /api/v1/auth/login` to receive a JWT token
                        3. Click **Authorize** above and enter: `Bearer <your-token>`
                        4. All protected endpoints will now include your token automatically

                        ## Public Endpoints
                        The following endpoints require no authentication:
                        - `POST /api/v1/auth/register`
                        - `POST /api/v1/auth/login`
                        - `GET /api/v1/flights/**`
                        - `GET /api/v1/airports/**`
                        - `GET /api/v1/airlines/**`
                        """)
                .version("1.0.0")
                .contact(new Contact()
                        .name("Jordan Shodipo")
                        .email("jordan.shodipo11@gmail.com"));
    }

    private SecurityScheme jwtSecurityScheme() {
        return new SecurityScheme()
                .name(SECURITY_SCHEME_NAME)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Enter your JWT token. Obtain it from POST /api/v1/auth/login");
    }
}
