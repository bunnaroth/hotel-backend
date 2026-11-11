package com.example.hotel.module.auth.service;

public class InvalidCredentialsException extends RuntimeException {

	public InvalidCredentialsException() {
		super("Invalid login credentials");
	}
}
