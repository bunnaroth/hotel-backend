package com.example.hotel.module.reservation.service;

public class ReservationLookupRateLimitException extends RuntimeException {

	public ReservationLookupRateLimitException() {
		super("Too many lookup requests. Please try again later.");
	}
}
