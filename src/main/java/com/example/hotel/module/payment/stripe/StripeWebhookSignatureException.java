package com.example.hotel.module.payment.stripe;

public class StripeWebhookSignatureException extends RuntimeException {

	public StripeWebhookSignatureException(String message, Throwable cause) {
		super(message, cause);
	}
}
