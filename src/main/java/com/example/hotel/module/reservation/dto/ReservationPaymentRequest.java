package com.example.hotel.module.reservation.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReservationPaymentRequest(
		@NotNull @DecimalMin(value = "0.01", inclusive = true) BigDecimal amount,
		@NotBlank String currency,
		@NotBlank String provider,
		String providerReference) {

	public ReservationPaymentRequest {
		if (amount == null || amount.signum() <= 0) {
			throw new IllegalArgumentException("Payment amount must be greater than zero");
		}
		currency = currency == null || currency.isBlank() ? "USD" : currency.trim().toUpperCase();
		provider = provider == null || provider.isBlank() ? "MANUAL" : provider.trim().toUpperCase();
	}
}
