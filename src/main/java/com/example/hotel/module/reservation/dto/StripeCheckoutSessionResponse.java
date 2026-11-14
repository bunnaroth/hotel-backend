package com.example.hotel.module.reservation.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record StripeCheckoutSessionResponse(
		UUID reservationId,
		String bookingReference,
		String sessionId,
		String checkoutUrl,
		BigDecimal amount,
		String currency,
		Instant expiresAt) {
}
