package com.example.hotel.config;

import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReservationLookupSigningConfigTest {

	private final ReservationLookupSigningConfig config = new ReservationLookupSigningConfig();

	@Test
	void requiresConfiguredSecret() {
		assertThrows(IllegalStateException.class, () -> config.reservationLookupSigningKey(""));
	}

	@Test
	void rejectsSecretShorterThan32Utf8Bytes() {
		assertThrows(IllegalStateException.class, () -> config.reservationLookupSigningKey("short-secret"));
	}

	@Test
	void buildsHmacSha256KeyFromConfiguredSecret() {
		String secret = "0123456789abcdef0123456789abcdef";

		SecretKey key = config.reservationLookupSigningKey(secret);

		assertEquals("HmacSHA256", key.getAlgorithm());
		assertArrayEquals(secret.getBytes(StandardCharsets.UTF_8), key.getEncoded());
	}
}
