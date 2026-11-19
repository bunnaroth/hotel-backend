package com.example.hotel.module.reservation.dto;

import java.time.LocalDate;
import java.util.List;

import com.example.hotel.module.reservation.enums.ReservationStatus;

public record ReservationLookupResponse(
		String bookingReference,
		LocalDate checkInDate,
		LocalDate checkOutDate,
		ReservationStatus status,
		List<String> roomTypes) {
}
