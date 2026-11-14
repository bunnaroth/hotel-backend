package com.example.hotel.module.reservation.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.example.hotel.module.reservation.enums.ReservationPaymentStatus;

public record ReservationPaymentResponse(
		UUID paymentId,
		UUID reservationId,
		ReservationPaymentStatus status,
		BigDecimal amount,
		String currency,
		String provider,
		String providerReference,
		Instant createdAt) {
}
