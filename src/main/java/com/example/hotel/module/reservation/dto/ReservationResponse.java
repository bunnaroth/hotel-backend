package com.example.hotel.module.reservation.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.example.hotel.module.reservation.enums.ReservationStatus;

public record ReservationResponse(
		UUID id,
		String bookingReference,
		LocalDate checkInDate,
		LocalDate checkOutDate,
		ReservationStatus status,
		Instant expiresAt,
		List<GuestSummary> guests,
		List<Long> roomIds) {

	public record GuestSummary(String fullName, boolean primary) {
	}
}
