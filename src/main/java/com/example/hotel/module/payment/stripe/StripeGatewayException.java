package com.example.hotel.module.payment.stripe;

public class StripeGatewayException extends RuntimeException {

	public StripeGatewayException(String message, Throwable cause) {
		super(message, cause);
	}

	public StripeGatewayException(String message) {
		super(message);
	}
}
