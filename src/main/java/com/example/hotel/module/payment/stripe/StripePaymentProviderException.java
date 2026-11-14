package com.example.hotel.module.payment.stripe;

public class StripePaymentProviderException extends RuntimeException {

	public StripePaymentProviderException(String message, Throwable cause) {
		super(message, cause);
	}
}
