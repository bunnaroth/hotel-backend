package com.example.hotel.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@SecurityScheme(
		name = "adminBasicAuth",
		type = SecuritySchemeType.HTTP,
		scheme = "basic",
		description = "HTTP Basic credentials for the single ADMIN account. Configure HOTEL_ADMIN_USERNAME and HOTEL_ADMIN_PASSWORD; the password must be at least 12 characters.")
@OpenAPIDefinition(
		info = @Info(
				title = "Hotel Management Backend API",
				version = "1.0",
				description = """
						Hotel booking API for public availability, reservation creation and lookup, Stripe Checkout, \
						and admin room inventory management.
						""",
				contact = @Contact(name = "Hotel Management Backend"),
				license = @License(name = "Private")
		),
		servers = {
				@Server(url = "http://localhost:8080", description = "Local development"),
				@Server(url = "/", description = "Current deployment")
		}
)
public class OpenApiConfig {
}
