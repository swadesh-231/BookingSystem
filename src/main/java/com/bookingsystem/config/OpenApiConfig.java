package com.bookingsystem.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        SecurityScheme bearerScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Enter your JWT access token");

        return new OpenAPI()
                .info(new Info()
                        .title("StayEase - Hotel Booking API")
                        .version("1.0.0")
                        .description("""
                                Backend reservation engine for hotel booking with real-time inventory management, \
                                dynamic pricing, Stripe payments, and JWT authentication.

                                **Authentication:** Most endpoints require a JWT access token. \
                                Use `/auth/login` to obtain one, then click "Authorize" and enter: `<your_access_token>`

                                **Roles:**
                                - `GUEST` — Book hotels, manage bookings, update profile
                                - `HOTEL_MANAGER` — Manage hotels, rooms, inventory, view reports
                                """)
                        .contact(new Contact()
                                .name("StayEase API Support")))
                .servers(List.of(
                        new Server().url("/api/v1").description("API v1")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", bearerScheme))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
