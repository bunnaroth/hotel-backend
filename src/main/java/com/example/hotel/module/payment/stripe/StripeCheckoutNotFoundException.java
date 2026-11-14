package com.example.hotel.module.payment.stripe;

public class StripeCheckoutNotFoundException extends RuntimeException {

	public StripeCheckoutNotFoundException(String sessionId) {
		super("Checkout session not found: " + sessionId);
	}
}
