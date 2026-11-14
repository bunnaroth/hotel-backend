package com.example.hotel.module.guest.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.example.hotel.module.reservation.enums.ReservationStatus;

public record GuestReservationHistoryResponse(
		UUID reservationId,
		String bookingReference,
		LocalDate checkInDate,
		LocalDate checkOutDate,
		ReservationStatus status,
		List<GuestResponse> guests) {
}
