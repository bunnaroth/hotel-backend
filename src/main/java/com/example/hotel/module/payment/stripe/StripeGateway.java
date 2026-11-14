package com.example.hotel.module.payment.stripe;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public interface StripeGateway {

	CheckoutSession createCheckoutSession(CheckoutRequest request, String secretKey, String idempotencyKey);

	WebhookEvent verifyWebhook(String payload, String signature, String webhookSecret);

	void refundPaymentIntent(String paymentIntentId, long amountMinor, String secretKey, String idempotencyKey);

	record CheckoutRequest(
			UUID reservationId,
			String bookingReference,
			LocalDate checkInDate,
			LocalDate checkOutDate,
			String guestEmail,
			BigDecimal amount,
			long amountMinor,
			String currency,
			Instant expiresAt,
			String successUrl,
			String cancelUrl) {
	}

	record CheckoutSession(String id, String url, Instant expiresAt) {
	}

	record WebhookEvent(String id, String type, String sessionId, String paymentStatus, String paymentIntentId) {
	}
}
