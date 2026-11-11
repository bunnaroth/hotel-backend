package com.example.hotel.module.guest.dto;

import java.util.UUID;

public record GuestResponse(
		UUID id,
		String firstName,
		String lastName,
		String email,
		String phoneNumber) {
}
