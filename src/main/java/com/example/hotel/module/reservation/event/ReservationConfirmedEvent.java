package com.example.hotel.module.reservation.event;

public record ReservationConfirmedEvent(
		String bookingReference,
		String guestName,
		String guestEmail) {
}
