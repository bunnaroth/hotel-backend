package com.example.hotel.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class AdminSecurityConfiguration {

	private static final Logger LOGGER = LoggerFactory.getLogger(AdminSecurityConfiguration.class);
	private static final int MINIMUM_ADMIN_PASSWORD_LENGTH = 12;

	@Bean
	SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
		return http
				.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers(
								"/swagger-ui.html",
								"/swagger-ui/**",
								"/v3/api-docs/**",
								"/actuator/health",
								"/actuator/info",
								"/error")
						.permitAll()
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/availability").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/reservations").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/reservations/*/payments/checkout").permitAll()
						.requestMatchers("/api/public/reservation-lookups/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/payments/stripe/checkout-sessions/**").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/payments/stripe/webhook").permitAll()
						.requestMatchers("/api/**").hasRole("ADMIN")
						.anyRequest().permitAll())
				.httpBasic(Customizer.withDefaults())
				.build();
	}

	@Bean
	PasswordEncoder adminPasswordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	UserDetailsService adminUserDetailsService(
			@Value("${app.security.admin.username:}") String configuredUsername,
			@Value("${app.security.admin.password:}") String configuredPassword,
			PasswordEncoder passwordEncoder) {
		String username = configuredUsername.trim();
		if (username.isEmpty() && configuredPassword.isBlank()) {
			LOGGER.warn("Admin credentials are not configured; protected API endpoints will reject all users");
			return new InMemoryUserDetailsManager();
		}
		if (username.isEmpty() || configuredPassword.isBlank()) {
			throw new IllegalStateException("Both HOTEL_ADMIN_USERNAME and HOTEL_ADMIN_PASSWORD must be configured");
		}
		if (configuredPassword.length() < MINIMUM_ADMIN_PASSWORD_LENGTH) {
			throw new IllegalStateException("HOTEL_ADMIN_PASSWORD must be at least 12 characters long");
		}

		return new InMemoryUserDetailsManager(User.withUsername(username)
				.password(passwordEncoder.encode(configuredPassword))
				.roles("ADMIN")
				.build());
	}
}
