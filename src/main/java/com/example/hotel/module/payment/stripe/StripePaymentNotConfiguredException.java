package com.example.hotel.module.payment.stripe;

public class StripePaymentNotConfiguredException extends RuntimeException {

	public StripePaymentNotConfiguredException(String message) {
		super(message);
	}
}
