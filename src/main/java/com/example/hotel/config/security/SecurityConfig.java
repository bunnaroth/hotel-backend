package com.example.hotel.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	private final JwtAuthenticationConverter jwtAuthenticationConverter;

	public SecurityConfig(JwtAuthenticationConverter jwtAuthenticationConverter) {
		this.jwtAuthenticationConverter = jwtAuthenticationConverter;
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http
				.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/availability").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/reservations").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/reservations/*/payments/checkout").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/payments/stripe/webhook").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/payments/stripe/checkout-sessions/**").permitAll()
						.requestMatchers("/api/reservations/**")
						.hasAnyRole("OWNER", "GENERAL_MANAGER", "ADMINISTRATOR", "RECEPTIONIST")
						.requestMatchers("/api/guests/**")
						.hasAnyRole("OWNER", "GENERAL_MANAGER", "ADMINISTRATOR", "RECEPTIONIST")
						.requestMatchers("/api/hotel/**", "/api/room-types/**", "/api/rooms/**")
						.hasAnyRole("OWNER", "GENERAL_MANAGER", "ADMINISTRATOR")
						.anyRequest().denyAll())
				.httpBasic(basic -> basic.disable())
				.formLogin(form -> form.disable())
				.oauth2ResourceServer(resourceServer -> resourceServer.jwt(jwt -> jwt
						.jwtAuthenticationConverter(jwtAuthenticationConverter)))
				.build();
	}
}
