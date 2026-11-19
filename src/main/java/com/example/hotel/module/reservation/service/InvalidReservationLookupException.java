package com.example.hotel.module.reservation.service;

public class InvalidReservationLookupException extends RuntimeException {

	public InvalidReservationLookupException() {
		super("The code is invalid or expired. Request a new code to try again.");
	}
}
