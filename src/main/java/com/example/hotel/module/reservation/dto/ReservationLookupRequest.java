package com.example.hotel.module.reservation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReservationLookupRequest(
		@NotBlank @Size(max = 24) String bookingReference,
		@NotBlank @Email @Size(max = 254) String email) {
}
