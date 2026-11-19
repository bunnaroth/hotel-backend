package com.example.hotel.config;

import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ReservationLookupSigningConfig {

	@Bean
	SecretKey reservationLookupSigningKey(@Value("${app.reservation-lookup.hmac-key}") String secret) {
		byte[] key = secret.getBytes(StandardCharsets.UTF_8);
		if (key.length < 32) {
			throw new IllegalArgumentException("Reservation lookup signing key must contain at least 32 UTF-8 bytes");
		}
		return new SecretKeySpec(key, "HmacSHA256");
	}
}
