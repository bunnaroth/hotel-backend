package com.example.hotel.module.reservation.dto;

import java.util.UUID;

import com.example.hotel.module.reservation.enums.ReservationPaymentStatus;
import com.example.hotel.module.reservation.enums.ReservationStatus;

public record StripeCheckoutStatusResponse(
		UUID reservationId,
		String bookingReference,
		ReservationStatus reservationStatus,
		ReservationPaymentStatus paymentStatus) {
}
